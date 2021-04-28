/*
 *  Copyright 2021 University of Padua, Italy
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

package search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import parse.ParsedDocument;
import search.queries.PhraseQueryGenerator;
import topics.Topics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Searches a document collection.
 *
 * @author Nicola Ferro (ferro@dei.unipd.it)
 * @version 1.00
 * @since 1.00
 */
public class TaskSearcher3g implements BasicSearcher {

    /**
     * The fields of the typical TREC topics.
     *
     * @author Nicola Ferro
     * @version 1.00
     * @since 1.00
     */
    private static final class TOPIC_FIELDS {

        /**
         * The title of a topic.
         */
        public static final String TITLE = "title";
    }


    /**
     * The identifier of the run
     */
    private final String runID;

    /**
     * The run to be written
     */
    private final PrintWriter run;

    /**
     * The index reader
     */
    private final IndexReader reader;

    /**
     * The index searcher.
     */
    private final IndexSearcher searcher;

    /**
     * The topics to be searched
     */
    private final QualityQuery[] topics;

    /**
     * The query parser
     */
    private final QueryParser bodyQueryParser;

    private final QueryParser titleQueryParser;

    private final QueryParser typedBodyQueryParser;

    private final QueryParser typedTitleQueryParser;

    /**
     * The maximum number of documents to retrieve
     */
    private final int maxDocsRetrieved;

    /**
     * The total elapsed time.
     */
    private long elapsedTime = Long.MIN_VALUE;

    private final Analyzer analyzer;
    private final Analyzer analyzer2;

    private final int numThreads;
    private final double threadsQueueFactor;


    /**
     * Creates a new searcher.
     *
     * @param analyzer         the {@code Analyzer} to be used.
     * @param similarity       the {@code Similarity} to be used.
     * @param indexPath        the directory where containing the index to be searched.
     * @param topicsFile       the file containing the topics to search for.
     * @param expectedTopics   the total number of topics expected to be searched.
     * @param runID            the identifier of the run to be created.
     * @param runPath          the path where to store the run.
     * @param maxDocsRetrieved the maximum number of documents to be retrieved.
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public TaskSearcher3g(final Analyzer analyzer, final Analyzer analyzer2, final Similarity similarity, final String indexPath,
                          final String topicsFile, final int expectedTopics, final String runID, final String runPath,
                          final int maxDocsRetrieved, int numThreads, double threadsQueueFactor) {

        if (analyzer == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }
        this.analyzer = analyzer;

        if (analyzer2 == null) {
            throw new NullPointerException("Analyzer (2) cannot be null.");
        }
        this.analyzer2 = analyzer2;

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be read.", indexDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to search the index.",
                    indexDir.toAbsolutePath().toString()));
        }

        try {
            reader = DirectoryReader.open(FSDirectory.open(indexDir));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index reader for directory %s: %s.",
                    indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);

        if (topicsFile == null) {
            throw new NullPointerException("Topics file cannot be null.");
        }

        if (topicsFile.isEmpty()) {
            throw new IllegalArgumentException("Topics file cannot be empty.");
        }

        try {
            //###########################################################
            //load topics
            final var list = new ArrayList<QualityQuery>();
            Topics.loadTopics(topicsFile).topics.forEach(topic -> {
                final var m = Collections.singletonMap("title", topic.title);
                final var q = new QualityQuery(Integer.toString(topic.number), m);
                list.add(q);
            });
            topics = list.toArray(QualityQuery[]::new);
            //############################################################
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to process topic file %s: %s.", topicsFile, e.getMessage()), e);
        }

        if (expectedTopics <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of topics to be searched cannot be less than or equal to zero.");
        }

        if (topics.length != expectedTopics) {
            System.out.printf("Expected to search for %s topics; %s topics found instead.", expectedTopics,
                    topics.length);
        }

        bodyQueryParser = new QueryParser(ParsedDocument.FIELDS.BODY, analyzer);
        titleQueryParser = new QueryParser(ParsedDocument.FIELDS.TITLE, analyzer);
        typedBodyQueryParser = new QueryParser(ParsedDocument.FIELDS.BODY, analyzer2);
        typedTitleQueryParser = new QueryParser(ParsedDocument.FIELDS.TITLE, analyzer2);

        if (runID == null) {
            throw new NullPointerException("Run identifier cannot be null.");
        }

        if (runID.isEmpty()) {
            throw new IllegalArgumentException("Run identifier cannot be empty.");
        }

        this.runID = runID;


        if (runPath == null) {
            throw new NullPointerException("Run path cannot be null.");
        }

        if (runPath.isEmpty()) {
            throw new IllegalArgumentException("Run path cannot be empty.");
        }

        final Path runDir = Paths.get(runPath);
        if (!Files.isWritable(runDir)) {
            throw new IllegalArgumentException(
                    String.format("Run directory %s cannot be written.", runDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(runDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the run.",
                    runDir.toAbsolutePath().toString()));
        }

        Path runFile = runDir.resolve(runID + ".txt");
        try {
            run = new PrintWriter(Files.newBufferedWriter(runFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to open run file %s: %s.", runFile.toAbsolutePath(), e.getMessage()), e);
        }

        if (maxDocsRetrieved <= 0) {
            throw new IllegalArgumentException(
                    "The maximum number of documents to be retrieved cannot be less than or equal to zero.");
        }

        this.numThreads = numThreads;
        this.threadsQueueFactor = threadsQueueFactor;

        this.maxDocsRetrieved = maxDocsRetrieved;
    }

    /**
     * Returns the total elapsed time.
     *
     * @return the total elapsed time.
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * /** Searches for the specified topics.
     *
     * @throws IOException    if something goes wrong while searching.
     * @throws ParseException if something goes wrong while parsing topics.
     */
    public void search() throws IOException, ParseException {

        System.out.printf("%n#### Start searching ####%n");

        // the start time of the searching
        final long start = System.currentTimeMillis();

        final Set<String> idField = new HashSet<>();
        idField.add(ParsedDocument.FIELDS.ID);

//        BooleanQuery.Builder booleanQueryBuilder = null;
//        Query query = null;
//        TopDocs docs = null;
//        ScoreDoc[] scoreDocs = null;
//        String docID = null;
        Queue<Future<FutureSearchResult>> futures = new LinkedList<>();
        Queue<String[]> results = new LinkedList<>();
        final var threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);

        try {
            for (QualityQuery topic_query : topics) {

                System.out.printf("Searching for topic %s.%n", topic_query.getQueryID());

                final var escapedTopic = QueryParserBase.escape(topic_query.getValue(TOPIC_FIELDS.TITLE));

                //create queries

                // NORMAL QUERY
                Query bodyQuery = bodyQueryParser.parse(escapedTopic);
//                bodyQuery = new BoostQuery(bodyQuery, 2f);

                Query titleQuery = titleQueryParser.parse(escapedTopic);
//                titleQuery = new BoostQuery(titleQuery, 1f);

                Query normalQuery = new BooleanQuery.Builder()
                    .add(bodyQuery, BooleanClause.Occur.SHOULD)
                    .add(titleQuery, BooleanClause.Occur.SHOULD)
                    .build();
                normalQuery = new BoostQuery(normalQuery, 1f);
                ////////////////////

                // TYPED QUERY
                Query typedBodyQuery = typedBodyQueryParser.parse(escapedTopic);
//                typedBodyQuery = new BoostQuery(typedBodyQuery, 2f);

                Query typedTitleQuery = typedTitleQueryParser.parse(escapedTopic);
//                typedTitleQuery = new BoostQuery(typedTitleQuery, 1f);

                Query typedQuery = new BooleanQuery.Builder()
                        .add(typedBodyQuery, BooleanClause.Occur.SHOULD)
                        .add(typedTitleQuery, BooleanClause.Occur.SHOULD)
                        .build();
//                typedQuery = new BoostQuery(typedQuery, 1.4f);
                //////////////////////

                // 2-WORDS PHRASE QUERY
                Query phraseQuery = new BooleanQuery.Builder()
                        .add(PhraseQueryGenerator.create2(analyzer, ParsedDocument.FIELDS.BODY, escapedTopic, 2), BooleanClause.Occur.SHOULD)
                        .add(PhraseQueryGenerator.create2(analyzer, ParsedDocument.FIELDS.TITLE, escapedTopic, 2), BooleanClause.Occur.SHOULD)
                        .build();
//                phraseQuery = new BoostQuery(phraseQuery, 2f);
                ////////////////

                //FINAL QUERY
                Query query = new BooleanQuery.Builder()
                        .add(normalQuery, BooleanClause.Occur.SHOULD)
                        .add(typedQuery, BooleanClause.Occur.SHOULD)
                        .add(phraseQuery, BooleanClause.Occur.SHOULD)
                        .build();
                /////////////


                //submit search
                final var f = threadPool.submit(() -> {
                    final var docs = searcher.search(query, maxDocsRetrieved);
                    return new FutureSearchResult(docs, topic_query.getQueryID());
                });
                futures.add(f);

                //prevent queue to grow too big
                while (futures.size() > threadsQueueFactor * numThreads) {
                    try {
                        final var r = futures.remove().get();
                        results.add(r.resultString(reader, idField, runID));
                    } catch (InterruptedException | ExecutionException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            futures.forEach(f -> {
                try {
                    results.add(f.get().resultString(reader, idField, runID));
                } catch (ExecutionException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            });
            results.forEach(res -> {
                Arrays.asList(res).forEach(run::print);
                run.flush();
            });
        } finally {
            run.close();

            reader.close();

            threadPool.shutdown();
        }

        elapsedTime = System.currentTimeMillis() - start;

        System.out.printf("%d topic(s) searched in %d seconds.", topics.length, elapsedTime / 1000);

        System.out.printf("#### Searching complete ####%n");
    }

    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        final String topics = "../../collections/TREC_08_1999_AdHoc/topics.txt";

        final String indexPath = "experiment/index-stop-nostem";

        final String runPath = "experiment";

        final String runID = "seupd2021-helloTipster-stop-nostem";

        final int maxDocsRetrieved = 1000;

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).build();

        Searcher s = new Searcher(a, new BM25Similarity(), indexPath, topics, 50, runID, runPath, maxDocsRetrieved);

        s.search();


    }

    private record FutureSearchResult(TopDocs docs, String queryId) {

        public String[] resultString(final IndexReader reader, final Set<String> idField, final String runID) {
            try {
                final var scoreDocs = docs.scoreDocs;
                final var ret = new ArrayList<String>();

//                final var threshold = 0.5f * scoreDocs[0].score;

                for (int i = 0, n = scoreDocs.length; i < n; i++) {
                    final var score = scoreDocs[i].score;
//                    if (score < threshold) continue;
                    final var docID = reader.document(scoreDocs[i].doc, idField).get(ParsedDocument.FIELDS.ID);

                    final var s = String.format(Locale.ENGLISH, "%s\tQ0\t%s\t%d\t%.6f\t%s%n", queryId, docID, i, score,
                            runID);
                    ret.add(s);
                }
                return ret.toArray(new String[0]);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
