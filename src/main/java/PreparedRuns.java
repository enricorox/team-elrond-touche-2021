import analyzers.KAnalyzer;
import analyzers.OpenNlpAnalyzer;
import analyzers.TaskAnalyzer;
import index.DirectoryIndexerMT;
import index.Indexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.DFISimilarity;
import org.apache.lucene.search.similarities.IndependenceStandardized;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import parse.Task1Parser;
import search.BasicSearcher;
import search.OpenNlpTaskSearcher;
import search.TaskBodySearcher;
import search.TaskSearcher1;

import java.io.IOException;

/**
 * Enum for executing differents runs
 */
public enum PreparedRuns {
    K_RUN("KAnalyzer", "TaskSearcher1") {
        @Override
        public void execute(Data data) {
            final Analyzer analyzer = new KAnalyzer();
            final Similarity similarity = new LMDirichletSimilarity();
            indexer = new DirectoryIndexerMT(
                    analyzer,
                    similarity,
                    data.ramBuffer,
                    data.indexPath,
                    data.docsPath,
                    data.extension,
                    data.charsetName,
                    data.expectedDocs,
                    Task1Parser.class,
                    data.numThreads,
                    data.threadQueueFactor);
            index();
            searcher = new TaskSearcher1(analyzer,
                    similarity,
                    data.indexPath,
                    data.topics,
                    data.expectedTopics,
                    "ElrondKRun",
                    data.runPath,
                    data.maxDocsRetrieved);

            search();
        }
    },
    OPEN_NLP("OpenNlpAnalyzer", "OpennlpSearcher") {
        @Override
        public void execute(Data data) {
            final Analyzer indexAnalyzer = new OpenNlpAnalyzer();
            final Analyzer queryAnalyzer = new OpenNlpAnalyzer(OpenNlpAnalyzer.FilterStrategy.ORIGINAL_ONLY);
            final Analyzer typedQueryAnalyzer = new OpenNlpAnalyzer(OpenNlpAnalyzer.FilterStrategy.TYPED_ONLY);
            final Similarity similarity = new LMDirichletSimilarity();
            indexer = new DirectoryIndexerMT(
                    indexAnalyzer,
                    similarity,
                    data.ramBuffer,
                    data.indexPath,
                    data.docsPath,
                    data.extension,
                    data.charsetName,
                    data.expectedDocs,
                    Task1Parser.class,
                    data.numThreads,
                    data.threadQueueFactor);
            index();
            searcher = new OpenNlpTaskSearcher(
                    queryAnalyzer,
                    typedQueryAnalyzer,
                    similarity,
                    data.indexPath,
                    data.topics,
                    data.expectedTopics,
                    "ElrondOpenNlpRun",
                    data.runPath,
                    data.maxDocsRetrieved,
                    data.numThreads,
                    data.threadQueueFactor
            );
            search();
        }
    },
    TASK_BODY_SEARCHER("TaskAnalyzer", "TaskBodySearcher") {
        @Override
        public void execute(Data data) {
            final Analyzer analyzer = new TaskAnalyzer();
            final Similarity similarity = new DFISimilarity(new IndependenceStandardized());
            indexer = new DirectoryIndexerMT(
                    analyzer,
                    similarity,
                    data.ramBuffer,
                    data.indexPath,
                    data.docsPath,
                    data.extension,
                    data.charsetName,
                    data.expectedDocs,
                    Task1Parser.class,
                    data.numThreads,
                    data.threadQueueFactor);
            index();
            searcher = new TaskBodySearcher(
                    analyzer,
                    similarity,
                    data.indexPath,
                    data.topics,
                    data.expectedTopics,
                    "ElrondTaskBodyRun",
                    data.runPath,
                    data.maxDocsRetrieved
            );
            search();
        }
    };

    private final String analyzerName;
    private final String searcherName;

    protected Indexer indexer;
    protected BasicSearcher searcher;

    PreparedRuns(String analyzerName, String searcherName) {
        this.analyzerName = analyzerName;
        this.searcherName = searcherName;
    }

    public abstract void execute(Data data);
    protected void index() {
        System.out.printf("Started indexing with '%s'...%n", analyzerName);
        try {
            indexer.index();
            System.out.println("Indexing succeeded");
        } catch (IOException e) {
            System.out.println("Indexing failed");
            e.printStackTrace();
            System.exit(1);
        }
    }
    protected void search() {
        System.out.printf("Started searching with '%s'...%n", searcherName);
        try {
            searcher.search();
            System.out.println("  Search succeeded");
        } catch (Exception e) {
            System.out.println("  Search failed");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Various data for executing the run
     */
    public static class Data {
        public final int ramBuffer;
        public final String extension;
        public final int expectedDocs;
        public final String charsetName;
        public final int numThreads;
        public final double threadQueueFactor;
        public final String runPath;
        public final int maxDocsRetrieved;
        public final int expectedTopics;
        public final String topics;
        public final String indexPath;
        public final String docsPath;

        public Data(int ramBuffer, String extension, int expectedDocs, String charsetName, int numThreads,
                    double threadQueueFactor, String runPath, int maxDocsRetrieved, int expectedTopics,
                    String topics, String indexPath, String docsPath) {
            this.ramBuffer = ramBuffer;
            this.extension = extension;
            this.expectedDocs = expectedDocs;
            this.charsetName = charsetName;
            this.numThreads = numThreads;
            this.threadQueueFactor = threadQueueFactor;
            this.runPath = runPath;
            this.maxDocsRetrieved = maxDocsRetrieved;
            this.expectedTopics = expectedTopics;
            this.topics = topics;
            this.indexPath = indexPath;
            this.docsPath = docsPath;
        }
    }
}
