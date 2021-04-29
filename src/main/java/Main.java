import index.DirectoryIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.*;
import parse.DocumentParser;
import parse.Task1Parser;
import search.BasicSearcher;
import search.TaskSearcher1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
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
        final var docsPath = props.getProperty("docs_path");

        final String extension = props.getProperty("extension");
        final int expectedDocs = Integer.parseInt(props.getProperty("expectedDocs"));
        final String charsetName = props.getProperty("charsetName");

    /*    final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).build();
*/
       final Analyzer a = CustomAnalyzer.builder(Paths.get(props.getProperty("stop_list")))
                .withTokenizer(StandardTokenizerFactory.class)
               .addTokenFilter(LowerCaseFilterFactory.class)
               .addTokenFilter(StopFilterFactory.class,
                                "ignoreCase", "false", "words", "99webtools.txt", "format", "wordset")
               .addTokenFilter(PorterStemFilterFactory.class)
               .build();
        //final Similarity similarity = new BM25Similarity();
        //final Similarity similarity=new ClassicSimilarity();
        final Similarity similarity=new LMDirichletSimilarity(1000);
       // final Similarity similarity=new DFRSimilarity(new BasicModelIn(), new AfterEffectL(), new NormalizationH1() );
        //final Similarity similarity=new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2() );

        final String runPath = props.getProperty("work_folder");

        final int maxDocsRetrieved = Integer.parseInt(props.getProperty("maxDocsRetrieved"));

        final int expectedTopics = Integer.parseInt(props.getProperty("expectedTopics"));

        final var topics = props.getProperty("topics_path");

        Arrays.stream(props.getProperty("parseList").split(" ")).forEach(parserName -> {
            final String indexPath = "%s/index-%s".formatted(props.getProperty("work_folder"), parserName);
            final Class<? extends DocumentParser> parser = switch (parserName) {
                case "task1parser" -> Task1Parser.class;
                default -> throw new IllegalArgumentException("Unknown parser %s".formatted(parserName));
            };
            System.out.println("\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.printf("Start indexing with '%s'...\n", parserName);
            final DirectoryIndexer i = new DirectoryIndexer(a, similarity, ramBuffer, indexPath, docsPath, extension, charsetName,
                    expectedDocs, parser);
            /*try {
                i.index();
                System.out.println("Indexing succeeded");
            } catch (IOException e) {
                System.out.println("Indexing failed");
                e.printStackTrace();
                return;
            }*/

            Arrays.stream(props.getProperty("methodsList").split(" ")).forEach(method -> {
                final var runID = "%s-%s".formatted(parserName, method);
                final BasicSearcher searcher = switch (method) {
                    case "taskSearcher1" -> new TaskSearcher1(a, similarity, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);
                    default -> throw new IllegalArgumentException("Unknown method %s".formatted(method));
                };
                System.out.println("\n############################################");
                System.out.printf("Searching with '%s'...\n", method);
                try {
                    searcher.search();
                    System.out.println("  Search succeeded");
                } catch (Exception e) {
                    System.out.println("  Search failed");
                    e.printStackTrace();
                }
                System.out.println("############################################");
            });

            System.out.println("\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        });
    }
}
