/*
 *  Copyright 2017-2021 University of Padua, Italy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import parse.DocumentParser;
import parse.ParsedDocument;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Indexes documents processing a whole directory tree.
 * This variant execute the indexing with multiple thread
 *
 * @author Nicola Ferro
 * @author elrond-group
 * @version 2.00
 * @since 1.00
 */
public class DirectoryIndexerMT implements Indexer {

    /**
     * One megabyte
     */
    private static final int MBYTE = 1024 * 1024;

    /**
     * The index writer.
     */
    private final IndexWriter writer;

    /**
     * The class of the {@code DocumentParser} to be used.
     */
    private final Class<? extends DocumentParser> dpCls;

    /**
     * The directory (and sub-directories) where documents are stored.
     */
    private final Path docsDir;

    /**
     * The extension of the files to be indexed.
     */
    private final String extension;

    /**
     * The charset used for encoding documents.
     */
    private final Charset cs;

    /**
     * The total number of documents expected to be indexed.
     */
    private final long expectedDocs;

    /**
     * The start instant of the indexing.
     */
    private final long start;

    /**
     * The total number of indexed files.
     */
    private long filesCount;

    /**
     * The total number of indexed documents.
     */
    private AtomicLong docsCount;

    /**
     * The total number of indexed bytes
     */
    private long bytesCount;

    /**
     * Set for removing duplicate documents
     */
    private Set<String> idSet = new HashSet<>();

    /**
     * Number ot thread to use
     */
    private final int numThreads;

    /**
     * Dimension of the thread-task queue as a factor of @numThreads
     */
    private final double threadsQueueFactor;

    /**
     * Creates a new indexer.
     *
     * @param analyzer           the {@code Analyzer} to be used.
     * @param similarity         the {@code Similarity} to be used.
     * @param ramBufferSizeMB    the size in megabytes of the RAM buffer for indexing documents.
     * @param indexPath          the directory where to store the index.
     * @param docsPath           the directory from which documents have to be read.
     * @param extension          the extension of the files to be indexed.
     * @param charsetName        the name of the charset used for encoding documents.
     * @param expectedDocs       the total number of documents expected to be indexed
     * @param dpCls              the class of the {@code DocumentParser} to be used.
     * @param numThreads         number of threads to use
     * @param threadsQueueFactor max dimension of threads-task queue as a factor of @numThreads (suggested 2 or 3
     *                           but might be lower if saving ram is important)
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public DirectoryIndexerMT(final Analyzer analyzer, final Similarity similarity, final int ramBufferSizeMB,
                              final String indexPath, final String docsPath, final String extension,
                              final String charsetName, final long expectedDocs,
                              final Class<? extends DocumentParser> dpCls, int numThreads, double threadsQueueFactor) {

        if (dpCls == null) {
            throw new NullPointerException("Document parser class cannot be null.");
        }

        this.dpCls = dpCls;

        if (analyzer == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (ramBufferSizeMB <= 0) {
            throw new IllegalArgumentException("RAM buffer size cannot be less than or equal to zero.");
        }

        final IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setSimilarity(similarity);
        iwc.setRAMBufferSizeMB(ramBufferSizeMB);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setCommitOnClose(true);

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);

        // if the directory does not already exist, create it
        if (Files.notExists(indexDir)) {
            try {
                Files.createDirectory(indexDir);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("Unable to create directory %s:.", indexDir.toAbsolutePath().toString(),
                                      e.getMessage()), e);
            }
        }

        if (!Files.isWritable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be written.", indexDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the index.",
                                                             indexDir.toAbsolutePath().toString()));
        }

        if (docsPath == null) {
            throw new NullPointerException("Documents path cannot be null.");
        }

        if (docsPath.isEmpty()) {
            throw new IllegalArgumentException("Documents path cannot be empty.");
        }

        final Path docsDir = Paths.get(docsPath);
        if (!Files.isReadable(docsDir)) {
            throw new IllegalArgumentException(
                    String.format("Documents directory %s cannot be read.", docsDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(docsDir)) {
            throw new IllegalArgumentException(
                    String.format("%s expected to be a directory of documents.", docsDir.toAbsolutePath().toString()));
        }

        this.docsDir = docsDir;

        if (extension == null) {
            throw new NullPointerException("File extension cannot be null.");
        }

        if (extension.isEmpty()) {
            throw new IllegalArgumentException("File extension cannot be empty.");
        }
        this.extension = extension;

        if (charsetName == null) {
            throw new NullPointerException("Charset name cannot be null.");
        }

        if (charsetName.isEmpty()) {
            throw new IllegalArgumentException("Charset name cannot be empty.");
        }

        try {
            cs = Charset.forName(charsetName);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Unable to create the charset %s: %s.", charsetName, e.getMessage()), e);
        }

        if (expectedDocs <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of documents to be indexed cannot be less than or equal to zero.");
        }
        this.expectedDocs = expectedDocs;

        this.docsCount = new AtomicLong(0);

        this.bytesCount = 0;

        this.filesCount = 0;

        try {
            writer = new IndexWriter(FSDirectory.open(indexDir), iwc);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index writer in directory %s: %s.",
                                                             indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        this.numThreads = numThreads;
        this.threadsQueueFactor = threadsQueueFactor;

        this.start = System.currentTimeMillis();

    }

    /**
     * Indexes the documents.
     *
     * @throws IOException if something goes wrong while indexing.
     */
    public void index() throws IOException {

        System.out.printf("%n#### Start indexing ####%n");

        Files.walkFileTree(docsDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(extension)) {
                    final var threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);

                    DocumentParser dp = DocumentParser.create(dpCls, Files.newBufferedReader(file, cs));

                    bytesCount += Files.size(file);

                    filesCount += 1;

                    Queue<Future<?>> futures = new LinkedList<>();

                    for (ParsedDocument pd : dp) {
                        final var f = threadPool.submit(() -> {
                            Document doc = null;

                            if (idSet.contains(pd.getIdentifier())) {
//                            System.err.printf("Skipped duplicate document %s%n", pd.getIdentifier());
                                return;
                            } else idSet.add(pd.getIdentifier());

                            doc = new Document();

                            // add the document identifier
                            doc.add(new StringField(ParsedDocument.FIELDS.ID, pd.getIdentifier(), Field.Store.YES));

                            //add title
                            doc.add(new TitleField(pd.getTitle()));

                            // add the document body
                            doc.add(new BodyField(pd.getBody()));

                            //add domain
                            doc.add(new StringField(ParsedDocument.FIELDS.DOMAIN, pd.getDomain(), Field.Store.YES));

                            try {
                                writer.addDocument(doc);
                            } catch (IOException e) {
                                throw new IllegalArgumentException(e);
                            }

                            docsCount.incrementAndGet();

                            // print progress every 10000 indexed documents
                            if (docsCount.get() % 10000 == 0) {
                                System.out.printf("%d document(s) (%d files, %d Mbytes) indexed in %d seconds.%n",
                                        docsCount.get(), filesCount, bytesCount / MBYTE,
                                        (System.currentTimeMillis() - start) / 1000);
                            }
                        });
                        futures.add(f);
                        //prevent queue to grow too big
                        while (futures.size() > threadsQueueFactor * numThreads) {
                            try {
                                futures.remove().get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    }
                    //wait for completions
                    futures.forEach(f -> {
                        try {
                            f.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    threadPool.shutdown();
                }
                return FileVisitResult.CONTINUE;
            }
        });

        writer.commit();

        writer.close();

        if (docsCount.get() != expectedDocs) {
            System.out.printf("Expected to index %d documents; %d indexed instead.%n", expectedDocs, docsCount.get());
        }

        System.out.printf("%d document(s) (%d files, %d Mbytes) indexed in %d seconds.%n", docsCount.get(), filesCount,
                          bytesCount / MBYTE, (System.currentTimeMillis() - start) / 1000);

        System.out.printf("#### Indexing complete ####%n");
    }
}
