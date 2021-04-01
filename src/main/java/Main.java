import index.DirectoryIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import parse.Task1Parser;
import topics.Topics;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {

        final int ramBuffer = 256;
        final String docsPath = "/home/gianmarco/Documenti/Projects/RI-data/task1";

        new File("experiment").mkdir();
        final String indexPath = "experiment/index-task1";

        final String extension = "json";
        final int expectedDocs = 387740;
        final String charsetName = "ISO-8859-1";

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).build();

        final Similarity similarity = new BM25Similarity();

//        final String topics = "../collections/TREC_27_2018_Core/topics.txt"; //todo ???

//        final String runPath = "experiment";

//        final String runID = "task1";

//        final int maxDocsRetrieved = 1000;

//        final int expectedTopics = 50;

        // indexing
        final DirectoryIndexer i = new DirectoryIndexer(a, similarity, ramBuffer, indexPath, docsPath, extension, charsetName,
                expectedDocs, Task1Parser.class);
        i.index();

        final var topics = Topics.loadTopics("/home/gianmarco/Documenti/Projects/RI-data/topics-task-1-only-titles.xml");
        System.out.println(topics);

        // searching
//        final Searcher s = new Searcher(a, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);
//        s.search();

    }
}
