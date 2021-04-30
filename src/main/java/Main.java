import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import utils.Props;

import java.io.File;
import java.util.Objects;
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
        parseOptionAndEditProps(args, props);

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

        switch(props.getProperty("RunToExecute")) {
            case "OpenNlpRun" -> PreparedRuns.OPEN_NLP.execute(data);
            case "TaskBodyRun" -> PreparedRuns.TASK_BODY_SEARCHER.execute(data);
            case "KRun" -> PreparedRuns.K_RUN.execute(data);
            case "SimpleRun" -> PreparedRuns.SIMPLE_RUN.execute(data);
            default -> {
                throw new IllegalArgumentException("Command line parsing messed up");
            }
        }

        final long endTime = System.currentTimeMillis();

        System.out.printf("Total execution time %f seconds%n", (endTime - startTime) / 1000.0);
    }

    private static void errorAndPrintHelp(final String error) {
        System.err.println(error);
        System.err.println("""
                Usage: this_program [-i input folder] [-o output dir] run_name
                -i     Replace input directory (for TIRA)
                -o     Replace output directory (for TIRA)
                Possible run names:
                               SimpleRun
                               KRun
                               TaskBodyRun
                               OpenNlpRun""");
    }

    private static void parseOptionAndEditProps(final String[] args, final Properties props) {
        final var cmdParser = new DefaultParser();
        final var options = new Options();

        options.addOption("i", true, "Input dir for both dataset and topics");
        options.addOption("o", true, "Output dir");

        CommandLine cmd = null;
        try {
            cmd = cmdParser.parse(options, args, true);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        cmd = Objects.requireNonNull(cmd);

        if (cmd.getArgs().length != 1) {
            errorAndPrintHelp("Unrecognised run");
            System.exit(1);
        }
        props.setProperty("RunToExecute", cmd.getArgs()[0]);

        final var i = cmd.getOptionValue("i");
        final var o = cmd.getOptionValue("o");
        if (i != null) {
            System.out.println("Replacing input props value with command line options...");
            props.setProperty("docs_path", i);
            props.setProperty("topics_path", i + "/" + "topics.xml");
        }
        if (o != null) {
            System.out.println("Replacing output props with command line option");
            props.setProperty("work_folder", o);
        }
    }
}
