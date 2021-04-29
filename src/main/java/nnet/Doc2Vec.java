package nnet;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import parse.ParsedDocument;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.neuroph.nnet.*;

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

    public static void main(String[] args) throws IOException {
        final String indexPath = "experiment/index-task1parser";
        final String fieldName = ParsedDocument.FIELDS.BODY;
        final String d1Analyzed = "cat table";
        Doc2Vec test = new Doc2Vec(indexPath, fieldName);

        System.out.printf("Dictionary size = %d%n", test.dimension);
        System.out.println("Vector representation (nonnull only):");
        double[] vec =  test.toVec(d1Analyzed);
        for(int k = 0; k < vec.length; k++)
            if(vec[k] > 0)
                System.out.printf("vec[%d] = %.2f%n", k, vec[k]);

    }
}
