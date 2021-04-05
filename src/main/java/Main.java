import index.DirectoryIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import parse.Task1Parser;
import search.Task1Searcher;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Main {
    private static Properties loadProps() throws IOException {
        final var in = Main.class.getResourceAsStream("data.properties");
        if (in == null) {
            throw new IllegalArgumentException("Copy example.properties as data.properties and edit it");
        }
        final var prop = new Properties();
        prop.load(in);
        return prop;
    }

    public static void main(String[] args) throws Exception {
        final var props = loadProps();

        final int ramBuffer = 256;

        new File(props.getProperty("work_folder")).mkdir();
        final String indexPath = "%s/%s".formatted(props.getProperty("work_folder"), props.getProperty("index_folder"));
        final var docsPath = props.getProperty("docs_path");

        final String extension = props.getProperty("extension");
        final int expectedDocs = Integer.parseInt(props.getProperty("expectedDocs"));
        final String charsetName = props.getProperty("charsetName");

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).build();

        final Similarity similarity = new BM25Similarity();

        final String runPath = props.getProperty("work_folder");

        final String runID = props.getProperty("runID");

        final int maxDocsRetrieved = Integer.parseInt(props.getProperty("maxDocsRetrieved"));

        final int expectedTopics = Integer.parseInt(props.getProperty("expectedTopics"));


        // indexing
        final DirectoryIndexer i = new DirectoryIndexer(a, similarity, ramBuffer, indexPath, docsPath, extension, charsetName,
                expectedDocs, Task1Parser.class);
        i.index();

        final var topics = props.getProperty("topics_path");

        // searching
        final var s = new Task1Searcher(a, similarity, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);
        s.search();

    }
}
