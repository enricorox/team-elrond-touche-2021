package nnet;

import analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import parse.ParsedDocument;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.StringTokenizer;

import topics.Topics;

public class Doc2Vec {
    final LeafReader index;
    final Directory dir;
    final int dimension;
    final String fieldName;
    final HashMap<String, Integer> map;

    public Doc2Vec(String indexPath, String fieldName) throws IOException {
        // Open the directory in Lucene
        dir = FSDirectory.open(Paths.get(indexPath));

        // Open the index - we need to get a LeafReader to be able to directly access terms
        index = DirectoryReader.open(dir).leaves().get(0).reader();

        this.fieldName = fieldName;

        // Get the vocabulary of the index.
        final Terms voc = index.terms(fieldName);

        // throw exception if too long
        this.dimension = Math.toIntExact(voc.size());

        // Create a map word -> int
        map = new HashMap<String, Integer>();

        // Get an iterator over the vector of terms in the vocabulary
        final TermsEnum termsEnum = voc.iterator();
        int k = 0;
        for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next())
            map.put(term.utf8ToString(), k++);
    }

    public void close() throws IOException {
        index.close();
        dir.close();
    }

    public double[] toVec(String body){
        double[] vec = new double[dimension]; // TODO need to reduce dimensionality? CURSE OF DIMENSIONALITY!
        StringTokenizer st = new StringTokenizer(body, " ");
        while(st.hasMoreTokens()){
            int i = map.getOrDefault(st.nextToken(), -1);
            if(i >= 0)
                vec[i]++;
        }
        return vec;
    }

    public double[] toVec(int doc) throws IOException {
        double[] vec = new double[dimension];
        Terms terms = index.getTermVector(doc, fieldName);
        // Get an iterator over the vector of terms
        TermsEnum termsEnum = terms.iterator();

        // Iterate until there are terms
        for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next()) {

            // Get the text string of the term
            String termstr = term.utf8ToString();

            // Get the total frequency of the term
            double freq = termsEnum.totalTermFreq();
            int i = map.getOrDefault(termstr, -1);
            if(i > 0)
                vec[i] = freq;
        }
        return vec;
    }

    static String buildString(final Analyzer a, final String t) throws IOException {
        StringBuilder sb = new StringBuilder();
        // Create a new TokenStream for a dummy field
        final TokenStream stream = a.tokenStream("field", new StringReader(t));
        final CharTermAttribute tokenTerm = stream.addAttribute(CharTermAttribute.class);
        try {
            // Reset the stream before starting
            stream.reset();

            // Print all tokens until the stream is exhausted
            stream.incrementToken();
            sb.append(tokenTerm.toString());
            while (stream.incrementToken()) {
                sb.append(" ").append(tokenTerm.toString());
            }

            // Perform any end-of-stream operations
            stream.end();
        } finally {

            // Close the stream and release all the resources
            stream.close();
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        final String indexPath = "experiment/index-task1parser";
        final String topicsFile = "/home/enrico/se-workspace/data/touche/2020-qrels-topics/topics-task-1.xml";
        final String fieldName = ParsedDocument.FIELDS.BODY;
        var analyzer = new SimpleAnalyzer();
        final String d1 = "The cat is on the table";

        Doc2Vec test = new Doc2Vec(indexPath, fieldName);

        System.out.printf("Dictionary size = %d%n", test.dimension);

        var t = buildString(analyzer, d1);
        System.out.printf("Vector representation (nonnull only) of \"%s\":%n", t);

        double[] vec =  test.toVec(t);
        for(int k = 0; k < vec.length; k++)
            if(vec[k] > 0)
                System.out.printf("vec[%d] = %.2f%n", k, vec[k]);


        var topics = Topics.loadTopics(topicsFile).topics;
        t = buildString(analyzer, topics.get(0).title);
        System.out.printf("Vector representation (nonnull only) of \"%s\":%n", t);

        vec =  test.toVec(buildString(analyzer, d1));
        for(int k = 0; k < vec.length; k++)
            if(vec[k] > 0)
                System.out.printf("vec[%d] = %.2f%n", k, vec[k]);
    }
}
