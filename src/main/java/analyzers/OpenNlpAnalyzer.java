package analyzers;

import analyzers.filters.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.*;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.util.AttributeFactory;
import utils.StopWords;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analyzer that use OpenNLP for tokenization and part-of-the-speech tagging
 */
public class OpenNlpAnalyzer extends Analyzer {
    private final FilterStrategy filterStrategy;
    final Set<String> stopTypes = Stream.of(
            //https://dpdearing.com/posts/2011/12/opennlp-part-of-speech-pos-tags-penn-english-treebank/
            ".", ",", ":", "\"", "(", ")", "<", ">", "``", "''", "-LRB-", "-RRB-", "-RSB-", "-RSB-", "-LCB-", "-RCB-",
            "IN", //Preposition or subordinating conjunction
            "CC", //Coordinating conjunction
            "PDT", //Predeterminer
            "POS", //Possessive ending
            "PRP", //Personal pronoun
            "PRP$", //Possessive pronoun
            "RB", "RBR", "RBS", //Adverb
            "SYM", //Symbol
            "RP", //Particle
            "TO",
            "UH", //Interjection
            "WDT", //Whdeterminer
            "WP", //Whpronoun
            "WP$", //Possessive whpronoun
            "WRB", //Whadverb
            "CD", //Cardinal number
            "date", "time"
    ).collect(Collectors.toCollection(HashSet::new));
    private final CharArraySet stopWords; //for caching purpose

    /**
     * Create a new OpenNlpAnalyzer
     * @param filterStrategy how to filter the output tokens
     */
    public OpenNlpAnalyzer(FilterStrategy filterStrategy) {
        this.filterStrategy = filterStrategy;
        stopWords = CharArraySet.unmodifiableSet(StopWords.loadStopWords("99webtools.txt"));
    }

    /**
     * Create a new OpenNlpAnalyzer with the default {@link FilterStrategy} NONE
     */
    public OpenNlpAnalyzer() {
        this(FilterStrategy.NONE);
    }

    /**
     * Create a new tokenStream according with the {@link FilterStrategy} provided
     * @param fieldName name of the field to search
     * @return stream of tokens
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final var loader = new ClasspathResourceLoader(ClassLoader.getSystemClassLoader());
        final Tokenizer tokenizer = createTokenizer(loader);
        TokenStream stream;

            stream = createNLPPOSFilter(tokenizer, loader);

            stream = new RemoveTypesFilter(stream, stopTypes);
            stream = new BreakHyphensFilter(stream);
            stream = new LowerCaseFilter(stream);
            stream = new StringReplaceFilter(stream, "'s", "is");
            stream = new StringReplaceFilter(stream, "'m", "am");
            stream = new StringReplaceFilter(stream, "'re", "are");
            stream = new StopFilter(stream, stopWords);
            stream = new PorterStemFilter(stream);
            stream = new TypeConcatenateSynonymFilter(stream);

        stream = filterStrategy.filterStream(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }

    /**
     * Create a new NLP-NER Tagger Filter
     * It's not actually used in the final version of the analyzer
     * @param stream input {@link TokenStream}
     * @param loader The {@link ClasspathResourceLoader} to use
     * @param name The name of the .bin ner-file to load
     * @return a new TokenStream with the type attribute update according
     */
    private TokenStream createNLPNERFilter(TokenStream stream, ClasspathResourceLoader loader, String name) {
        try {
            final var nerMd = OpenNLPOpsFactory.getNERTaggerModel("opennlp/" + name, loader);
            final var tag = new NLPNERTaggerOp(nerMd);
            return new OpenNLPNERFilter(stream, tag);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create a new NLP-POS Tagger Filter
     * @param tokenizer {@link Tokenizer} to use
     * @param loader {@link ClasspathResourceLoader} to use
     * @return a new TokenStream with the type attribute update according
     */
    private TokenStream createNLPPOSFilter(Tokenizer tokenizer, ClasspathResourceLoader loader) {
        try {
            final var posMd = OpenNLPOpsFactory.getPOSTaggerModel("opennlp/en-pos-maxent.bin", loader);
//            final var posMd = OpenNLPOpsFactory.getPOSTaggerModel("opennlp/en-pos-perceptron.bin", loader);
            final var tagOp = new NLPPOSTaggerOp(posMd);
            return new OpenNLPPOSFilter(tokenizer, tagOp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create a new NLP Tokenizer
     * @param loader the {@link ClasspathResourceLoader} to use for loading the nlp file
     * @return a new {@link OpenNLPTokenizer}
     */
    private Tokenizer createTokenizer(ClasspathResourceLoader loader) {
        try {
            final var tokOpModel = OpenNLPOpsFactory
                    .getTokenizerModel("opennlp/en-token.bin", loader);
            final var nlpTk = new NLPTokenizerOp(tokOpModel);
            final var sentMd = OpenNLPOpsFactory.getSentenceModel("opennlp/en-sent.bin", loader);
            final var sentDetOP = new NLPSentenceDetectorOp(sentMd);
            return new OpenNLPTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY, sentDetOP, nlpTk);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Enum that define the filter strategy to apply in the {@link OpenNlpAnalyzer} token stream
     */
    public enum FilterStrategy {
        /**
         * Do not apply any filtering
         * This is the one that should be used for the indexing
         * Every token will have a synonym {@literal <}type{@literal >}token
         */
        NONE {
            @Override
            protected TokenStream filterStream(TokenStream stream) {
                return stream;
            }
        },
        /**
         * Return only the original tokens without the type synonyms
         */
        ORIGINAL_ONLY {
            @Override
            protected TokenStream filterStream(TokenStream stream) {
                return new SeparateTokenTypesFilter(stream, SeparateTokenTypesFilter.Keep.ORIGINAL);
            }
        },
        /**
         * Return only the typed tokens {@literal <}type{@literal >}token
         */
        TYPED_ONLY {
            @Override
            protected TokenStream filterStream(TokenStream stream) {
                return new SeparateTokenTypesFilter(stream, SeparateTokenTypesFilter.Keep.TYPE_C_TOKEN);
            }
        };

        protected abstract TokenStream filterStream(TokenStream stream);
    }

    /**
     * Test method for {@link OpenNlpAnalyzer}
     * @param args command line args (ignored)
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final var analyzer = new OpenNlpAnalyzer();
        final var testText = "Should felons who have completed their sentence be allowed to vote?";
        final var stream = analyzer.tokenStream("body", testText);
        CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posAtt = stream.getAttribute(PositionIncrementAttribute.class);
        TypeAttribute typeAttribute = stream.addAttribute(TypeAttribute.class);
        KeywordAttribute keywordAttribute = stream.addAttribute(KeywordAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            final var token = att.toString();
            final var type = typeAttribute.type();
            final var pos = (posAtt!=null)?posAtt.getPositionIncrement():1;
            final var isKeyword = keywordAttribute.isKeyword();
            if (pos > 0)
                if (type != null)
                    System.out.printf("Token: %s -> %s%s%n", token, type, isKeyword?" [k]":"");
                else
                    System.out.printf("Token: %s%s%n", token, isKeyword?" [k]":"");
            else
                if (type != null)
                    System.out.printf("       %s -> %s%s%n", token, type, isKeyword?" [k]":"");
                else
                    System.out.printf("       %s%s%n", token, isKeyword?" [k]":"");
        }
    }

}
