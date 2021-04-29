package analyzers;

import analyzers.filters.AddCategoryFilter;
import analyzers.filters.CustomSynonymsFilter;
import analyzers.filters.LovinsStemFilter;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import utils.StopWords;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Analyzer fro Task1
 * It uses the Lovin Stemmer and the 99webtools stop-list
 */
public class TaskAnalyzer extends Analyzer {
    /**
     * The expansion strategy to use
     */
    private final ExpansionStrategy expansionStrategy;

    /**
     * stop-word list for caching reason
     */
    private final CharArraySet stopWords;

    /**
     * Map of word -> synonyms (for caching reason)
     */
    private static final Map<String, Set<String>> synonymsMap = new HashMap<>();

    /**
     * Map of word -> category (for caching reason)
     */
    private static final Map<String, String> categoryMap = new HashMap<>(); //caching reason

    /**
     * Create a new {@link TaskAnalyzer} with default strategy (no expansion)
     */
    public TaskAnalyzer() {
        this(ExpansionStrategy.NONE);
    }

    /**
     * Create a new {@link TaskAnalyzer} with the specified {@link ExpansionStrategy}
     * @param expansionStrategy the expansion strategy to use
     */
    public TaskAnalyzer(final ExpansionStrategy expansionStrategy) {
        this.expansionStrategy = expansionStrategy;
        stopWords = CharArraySet.unmodifiableSet(StopWords.loadStopWords("99webtools.txt"));
    }

    /**
     * Create a new token stream according with the provided {@link ExpansionStrategy}
     * @param fieldName name of the field to search
     * @return streams of token
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer tokenizer = new StandardTokenizer();

        TokenStream stream = normalize(fieldName, tokenizer);
        stream = new EnglishPossessiveFilter(stream);
        stream = new EnglishMinimalStemFilter(stream);
        stream = new StopFilter(stream, stopWords);
        stream = expansionStrategy.expand(stream);
        stream = new LovinsStemFilter(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }

    /**
     * Normalize the stream reducing it to lower-case
     * @param fieldName name of the field to search
     * @param in input {@link TokenStream}
     * @return normalized stream
     */
    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }

    /**
     * Enum that define the query expansion strategy for {@link TaskAnalyzer}
     */
    public enum ExpansionStrategy {
        /**
         * Do nothing
         */
        NONE {
            @Override
            protected TokenStream expand(TokenStream stream) {
                return stream;
            }
        },
        /**
         * Append word-net synonyms
         */
        SYNONYMS {
            @Override
            protected TokenStream expand(TokenStream stream) {
                return new CustomSynonymsFilter(stream, synonymsMap);
            }
        },
        /**
         * Append categories (extracted from word-net files) as synonyms
         */
        CATEGORIES {
            @Override
            protected TokenStream expand(TokenStream stream) {
                return new AddCategoryFilter(stream, categoryMap);
            }
        };

        protected abstract TokenStream expand(final TokenStream stream);
    }

    /**
     * Main method for testing purpose
     * @param args command line arguments (ignored)
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final var analyzer = new TaskAnalyzer(ExpansionStrategy.SYNONYMS);
        final var testText = "The cat! It's on the table! cat-table";
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
