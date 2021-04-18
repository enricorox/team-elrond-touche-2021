package analyzers;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import utils.StopWords;

import java.io.IOException;
import java.io.InputStreamReader;

public class MyAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer tokenizer = new StandardTokenizer();

        TokenStream tokenStream = normalize(fieldName, tokenizer);
        tokenStream = new EnglishPossessiveFilter(tokenStream);
        tokenStream = new EnglishMinimalStemFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, StopWords.loadStopWords("99webtools.txt"));
        tokenStream = new PorterStemFilter(tokenStream);

        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }

    public static void main(String[] args) throws IOException {
        final var analyzer = new MyAnalyzer();
        final var testText = "The cat! It's on the table!";
        final var stream = analyzer.tokenStream("body", testText);
        CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
        PositionIncrementAttribute incrementAttribute = stream.getAttribute(PositionIncrementAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            final var token = att.toString();
            final var synonym = incrementAttribute.getPositionIncrement() == 0;
            System.out.printf("Token: '%s'%n" +
                    "  synonym: %s%n" +
                    "%n", token, synonym);
        }
    }
}
