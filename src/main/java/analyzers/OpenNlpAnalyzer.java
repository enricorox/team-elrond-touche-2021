package analyzers;

import analyzers.filters.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.*;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenNlpAnalyzer extends Analyzer {
    private final FilterStrategy filterStrategy;
    final Set<String> stopTypes = Stream.of(
            //https://dpdearing.com/posts/2011/12/opennlp-part-of-speech-pos-tags-penn-english-treebank/
            ".", ",", ":",
            "IN", //Preposition or subordinating conjunction
            "DT", //Determiner
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
            "WRB" //Whadverb
    ).collect(Collectors.toCollection(TreeSet::new));

    public OpenNlpAnalyzer(FilterStrategy filterStrategy) {
        this.filterStrategy = filterStrategy;
    }

    public OpenNlpAnalyzer() {
        this(FilterStrategy.NONE);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final var loader = new ClasspathResourceLoader(ClassLoader.getSystemClassLoader());
        final Tokenizer tokenizer = createTokenizer(loader);
        TokenStream stream;

        stream = createNLPPOSFilter(tokenizer, loader);
//        stream = createNLPNERFilter(stream, loader, "en-ner-location.bin");
//        stream = createNLPNERFilter(stream, loader, "en-ner-person.bin");
        stream = createNLPNERFilter(stream, loader, "en-ner-organization.bin");
//        stream = createNLPNERFilter(stream, loader, "en-ner-date.bin");
//        stream = createNLPNERFilter(stream, loader, "en-ner-time.bin");

        stream = new RemoveTypesFilter(stream, stopTypes);
        stream = new LowerCaseFilter(stream);
        stream = new TypeConcatenateSynonymFilter(stream);
//        stream = new TypeAsSynonymFilter(stream);

        stream = switch (filterStrategy) {
            case ORIGINAL_ONLY -> new SeparateTokenTypesFilter(stream, SeparateTokenTypesFilter.Keep.ORIGINAL);
            case TYPED_ONLY -> new SeparateTokenTypesFilter(stream, SeparateTokenTypesFilter.Keep.TYPE_C_TOKEN);
            case NONE -> stream;
        };

        stream = new PorterStemFilter(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }

    private TokenStream createNLPNERFilter(TokenStream stream, ClasspathResourceLoader loader, String name) {
        try {
            final var nerMd = OpenNLPOpsFactory.getNERTaggerModel("opennlp/" + name, loader);
            final var tag = new NLPNERTaggerOp(nerMd);
            return new OpenNLPNERFilter(stream, tag);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private TokenStream createNLPPOSFilter(Tokenizer tokenizer, ClasspathResourceLoader loader) {
        try {
            final var posMd = OpenNLPOpsFactory.getPOSTaggerModel("opennlp/en-pos-maxent.bin", loader);
            final var tagOp = new NLPPOSTaggerOp(posMd);
            return new OpenNLPPOSFilter(tokenizer, tagOp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

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
    
    public enum FilterStrategy {
        NONE, ORIGINAL_ONLY, TYPED_ONLY
    }

    public static void main(String[] args) throws IOException {
        final var analyzer = new OpenNlpAnalyzer();
//        final var testText = "The cat! It's on the table!";
//        final var testText = "My city is beautiful, but Rome is probably better! ???";
//        final var testText = "I now live in Rome where I met my wife Alice back in 2010 during a beautiful afternoon. ";
        final var testText = "Should felons who have completed their sentence be allowed to vote?";
        final var stream = analyzer.tokenStream("body", testText);
        CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posAtt = stream.getAttribute(PositionIncrementAttribute.class);
        TypeAttribute typeAttribute = stream.addAttribute(TypeAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            final var token = att.toString();
            final var type = typeAttribute.type();
            final var pos = (posAtt!=null)?posAtt.getPositionIncrement():1;
            if (pos > 0)
                if (type != null)
                    System.out.printf("Token: %s -> %s%n", token, type);
                else
                    System.out.printf("Token: %s%n", token);
            else
                if (type != null)
                    System.out.printf("       %s -> %s%n", token, type);
                else
                    System.out.printf("       %s%n", token);
        }
    }

}
