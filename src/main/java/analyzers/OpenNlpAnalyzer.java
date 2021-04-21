package analyzers;

import analyzers.filters.MarkTypeFilter;
import analyzers.filters.RemoveTypesFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.TypeAsSynonymFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.NLPPOSTaggerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPSentenceDetectorOp;
import org.apache.lucene.analysis.opennlp.tools.NLPTokenizerOp;
import org.apache.lucene.analysis.opennlp.tools.OpenNLPOpsFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenNlpAnalyzer extends Analyzer {
    private boolean typesToSynonyms;

    public OpenNlpAnalyzer(boolean typesToSynonyms) {
        this.typesToSynonyms = typesToSynonyms;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final var loader = new ClasspathResourceLoader(ClassLoader.getSystemClassLoader());
        final Tokenizer tokenizer = createTokenizer(loader);
        TokenStream stream;

        stream = createNLPPOSFilter(tokenizer, loader);

        final var typesToRemove = Stream.of(",", ".")
                .collect(Collectors.toCollection(TreeSet::new));
        stream = new RemoveTypesFilter(stream, typesToRemove);
        stream = new LowerCaseFilter(stream);
        if (typesToSynonyms) {
            stream = new MarkTypeFilter(stream);
            stream = new TypeAsSynonymFilter(stream);
        }
        return new TokenStreamComponents(tokenizer, stream);
    }

    //    private TokenStream createNLPNERFilter(TokenStream stream, ClasspathResourceLoader loader, String name) {
//        try {
//            final var nerMd = OpenNLPOpsFactory.getNERTaggerModel("opennlp/" + name, loader);
//            final var tag = new NLPNERTaggerOp(nerMd);
//            return new OpenNLP
//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }
//    }

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

    public static void main(String[] args) throws IOException {
        final var analyzer = new OpenNlpAnalyzer(true);
//        final var testText = "The cat! It's on the table!";
//        final var testText = "cat";
        final var testText = "My city is beautiful, but Rome is probably better! ???";
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
                System.out.printf("Token: %s --> %s%n", token, type);
            else
                System.out.printf("       %s --> %s%n", token, type);
        }
    }

}
