package parse;

import java.io.*;
import java.util.Scanner;

public class QrelsParser {
    Reader in;
    Scanner sc;
    String[] line;
    public QrelsParser(String qrelsPath) throws FileNotFoundException {
        File qrels = new File(qrelsPath);
        sc = new Scanner(qrels);
    }

    public boolean hasNext(){
        return sc.hasNextLine();
    }

    public void next(){
        line = sc.nextLine().split(" ");
    }

    public int currTopic(){
        return Integer.parseInt(line[0]);
    }

    public String currDoc(){
        return line[2];
    }

    public Double currScore(){
        return Double.parseDouble(line[3]);
    }

    public static void main(String[] args) throws FileNotFoundException {
        String qrelsPath =
                "/home/enrico/se-workspace/data/touche/2020-qrels-topics/touche2020-task1-relevance-args-me-corpus-version-2020-04-01.qrels";
        var qrelsp = new QrelsParser(qrelsPath);
        qrelsp.next();
        System.out.printf("Topic: %d%nDocument: %s%nScore: %f%n",
                qrelsp.currTopic(),
                qrelsp.currDoc(),
                qrelsp.currScore());
    }
}
