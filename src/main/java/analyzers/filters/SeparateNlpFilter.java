package analyzers.filters;

import analyzers.OpenNlpAnalyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public class SeparateNlpFilter extends TokenFilter {
    private final Strategy strategy;

    private final CharTermAttribute charTermAttribute;
    private final PositionIncrementAttribute positionIncrementAttribute;
    private final TypeAttribute typeAttribute;

    public SeparateNlpFilter(TokenStream input, Strategy strategy) {
        super(input);
        this.strategy = strategy;
        charTermAttribute = addAttribute(CharTermAttribute.class);
        positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
        typeAttribute = addAttribute(TypeAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        return switch (strategy) {
            case ORIGINAL_ONLY -> originalOnlyIT();
            case TYPES_ONLY -> typesOnlyIT();
            case RESTORE_TYPES -> restoreTypesIT();
        };
    }

    private boolean originalOnlyIT() throws IOException {
        int pos;
        String token;
        do {
            if (!input.incrementToken()) return false;
            pos = positionIncrementAttribute.getPositionIncrement();
            token = charTermAttribute.toString();
        } while (pos == 0 && isNlpSynType(token));
        return true;
    }

    private boolean typesOnlyIT() throws IOException {
        int pos;
        String token;
        do {
            if (!input.incrementToken()) return false;
            pos = positionIncrementAttribute.getPositionIncrement();
            token = charTermAttribute.toString();
        } while (pos > 0 && !isNlpSynType(token));
        charTermAttribute.setEmpty().append(token.substring(1, token.length()-1));
        positionIncrementAttribute.setPositionIncrement(1);
        return true;
    }

    private boolean restoreTypesIT() throws IOException {
        if (!input.incrementToken()) return false;
        if (positionIncrementAttribute.getPositionIncrement() == 0)
            throw new IllegalStateException("The token shouldn't be a synonym");
        final var token = charTermAttribute.toString();
        int pos;
        String type = null;
        while (true){
            if (!input.incrementToken()) break;
            pos = positionIncrementAttribute.getPositionIncrement();
            if (pos > 0) throw new IllegalStateException("Type synonym absent");
            type = charTermAttribute.toString();
            if (isNlpSynType(type)) break;
        }
        if (type == null) throw new IllegalStateException("Type synonym not found, stream ended");
        type = type.substring(1, type.length()-1);
        charTermAttribute.setEmpty().append(token);
        positionIncrementAttribute.setPositionIncrement(1);
        typeAttribute.setType(type);
        return true;
    }

    private boolean isNlpSynType(final String token) {
        return (token.charAt(0) == '<') && (token.charAt(token.length()-1) == '>');
    }

    public enum Strategy {
        ORIGINAL_ONLY, //remove types synonyms
        TYPES_ONLY, //keep only types synonyms
        RESTORE_TYPES //remove type synonyms and set them as type attribute
    }

    public static void main(String[] args) throws IOException {
        final var testText = "I now live in Rome where I met my wife Alice back in 2010 during a beautiful afternoon. ";
        final var analyzer = new OpenNlpAnalyzer();
        var stream = analyzer.tokenStream("body", testText);
//        final var strategy = Strategy.ORIGINAL_ONLY;
//        final var strategy = Strategy.TYPES_ONLY;
        final var strategy = Strategy.RESTORE_TYPES;
        stream = new SeparateNlpFilter(stream, strategy);
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
