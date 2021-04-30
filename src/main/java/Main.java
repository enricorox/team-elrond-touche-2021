import utils.Props;

import java.io.File;
import java.util.Properties;

public class Main {
    private static Properties loadProps() {
        try {
            return Props.loadProps(Main.class, "data.properties");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Copy example.properties as data.properties and edit it");
        }
    }

    public static void main(String[] args) throws Exception {
        final var props = loadProps();

        final int ramBuffer = 256;

        new File(props.getProperty("work_folder")).mkdir();
        final var docsPath = props.getProperty("docs_path");

        final String extension = props.getProperty("extension");
        final int expectedDocs = Integer.parseInt(props.getProperty("expectedDocs"));
        final String charsetName = props.getProperty("charsetName");

        final int numThreads = Integer.parseInt(props.getProperty("numThreads"));
        final double threadQueueFactor = Double.parseDouble(props.getProperty("threadQueueFactor"));

        final String runPath = props.getProperty("work_folder");
        final int maxDocsRetrieved = Integer.parseInt(props.getProperty("maxDocsRetrieved"));
        final int expectedTopics = Integer.parseInt(props.getProperty("expectedTopics"));
        final var topics = props.getProperty("topics_path");

        final String indexPath = "%s/index-task1parser".formatted(props.getProperty("work_folder"));

        final var data = new PreparedRuns.Data(
                ramBuffer,
                extension,
                expectedDocs,
                charsetName,
                numThreads,
                threadQueueFactor,
                runPath,
                maxDocsRetrieved,
                expectedTopics,
                topics,
                indexPath,
                docsPath
        );

        final long startTime = System.currentTimeMillis();

        if (args.length >= 1) {
            switch(args[0]) {
                case "OpenNlpRun" -> PreparedRuns.OPEN_NLP.execute(data);
                case "TaskBodyRun" -> PreparedRuns.TASK_BODY_SEARCHER.execute(data);
                case "KRun" -> PreparedRuns.K_RUN.execute(data);
                case "SimpleRun" -> PreparedRuns.SIMPLE_RUN.execute(data);
                default -> {
                    errorAndPrintHelp("Unknown run name");
                }
            };
        } else errorAndPrintHelp("No run specified");

        final long endTime = System.currentTimeMillis();

        System.out.printf("Total execution time %f seconds%n", (endTime - startTime) / 1000.0);
    }

    private static void errorAndPrintHelp(final String error) {
        System.err.println(error);
        System.err.println("""
                HELP: execute this program passing a single parameter on the command line with the name of the run.
                Possible runs:
                               SimpleRun
                               KRun
                               TaskBodyRun
                               OpenNlpRun""");
    }
}
