package analyzers;

import analyzers.filters.AddCategoryFilter;
import analyzers.filters.BreakHyphensFilter;
import analyzers.filters.CustomSynonymsFilter;
import analyzers.filters.LovinsStemFilter;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import utils.StopWords;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class MyAnalyzer extends Analyzer {
    private final ExpansionStrategy expansionStrategy;
    private final CharArraySet stopWords;

    private static final Map<String, Set<String>> synonymsMap = new HashMap<>(); //caching reason
    private static final Map<String, String> categoryMap = new HashMap<>(); //caching reason

    public MyAnalyzer() {
        this(ExpansionStrategy.NONE);
    }
    public MyAnalyzer(final ExpansionStrategy expansionStrategy) {
        this.expansionStrategy = expansionStrategy;
        stopWords = CharArraySet.unmodifiableSet(StopWords.loadStopWords("99webtools.txt"));
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer tokenizer = new StandardTokenizer();

        TokenStream stream = normalize(fieldName, tokenizer);
//        stream = new BreakHyphensFilter(stream);
        stream = new EnglishPossessiveFilter(stream);
        stream = new EnglishMinimalStemFilter(stream);
        stream = new StopFilter(stream, stopWords);
        stream = expansionStrategy.expand(stream);
//        stream = new PorterStemFilter(stream);
        stream = new LovinsStemFilter(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }

    public enum ExpansionStrategy {
        NONE {
            @Override
            protected TokenStream expand(TokenStream stream) {
                return stream;
            }
        },
        SYNONYMS {
            @Override
            protected TokenStream expand(TokenStream stream) {
                return new CustomSynonymsFilter(stream, synonymsMap);
            }
        },
        CATEGORIES {
            @Override
            protected TokenStream expand(TokenStream stream) {
                return new AddCategoryFilter(stream, categoryMap);
            }
        };

        protected abstract TokenStream expand(final TokenStream stream);
    }

    public static void main(String[] args) throws IOException {
        final var analyzer = new MyAnalyzer();
        final var testText = "The cat! It's on the table! cat-table";
//        final var testText = "cat";
        final var stream = analyzer.tokenStream("body", testText);
        CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
        PositionIncrementAttribute incrementAttribute = stream.getAttribute(PositionIncrementAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            final var token = att.toString();
            final var synonym = incrementAttribute.getPositionIncrement() == 0;
            if (synonym) {
                System.out.printf("       %s%n", token);
            } else {
                System.out.printf("Token: %s%n", token);
            }
        }
    }
}
