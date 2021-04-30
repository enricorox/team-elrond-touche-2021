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

        final int numThreads = 12;
        final double threadQueueFactor = 3;

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

        if (args.length >= 1) {
            switch(args[0]) {
                case "OpenNlpRun" -> PreparedRuns.OPEN_NLP.execute(data);
                case "TaskBodyRun" -> PreparedRuns.TASK_BODY_SEARCHER.execute(data);
                case "KRun" -> PreparedRuns.K_RUN.execute(data);
                default -> {
                    System.err.println("Unknown run name");
                }
            };
        } else System.err.println("No run specified");
    }
}
