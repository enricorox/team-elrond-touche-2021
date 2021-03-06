package analyzers.filters;

import analyzers.OpenNlpAnalyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

/**
 * Filter that separate original token from type-concatenated tokens
 * e.g. in stream "token1 {@literal <}type1{@literal >}token1 token2 {@literal <}type2{@literal >}token2"
 * originals are token1 token2
 * type-concatenated are {@literal <}type1{@literal >}token1 {@literal <}type2{@literal >}token2
 */
public class SeparateTokenTypesFilter extends TokenFilter {
    /**
     * What to keep
     */
    private final Keep keep;

    private final CharTermAttribute charTermAttribute;
    private final PositionIncrementAttribute positionIncrementAttribute;

    /**
     * Create new {@link SeparateTokenTypesFilter}
     * @param input input stream
     * @param keep what to keep
     */
    public SeparateTokenTypesFilter(TokenStream input, Keep keep) {
        super(input);
        this.keep = keep;
        charTermAttribute = addAttribute(CharTermAttribute.class);
        positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        String token;
        boolean isTypeToken;
        boolean isToKeep;
        do {
            if (!input.incrementToken()) return false;
            token = charTermAttribute.toString();
            isTypeToken = token.charAt(0) == '<';
            isToKeep = keep.isToKeep(isTypeToken);
        } while (!isToKeep);
        if (isTypeToken)
            positionIncrementAttribute.setPositionIncrement(1);
        return true;
    }

    /**
     * Enum that specify what to keep between original tokens and typed-tokens
     */
    public enum Keep {
        ORIGINAL {@Override protected boolean isToKeep(boolean isTypeToken) {return !isTypeToken;}},
        TYPE_C_TOKEN{@Override protected boolean isToKeep(boolean isTypeToken) {return isTypeToken;}};

        protected abstract boolean isToKeep(boolean isTypeToken);
    }

    /**
     * Main method for testing purpose
     * @param args cmd-args (ignored)
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final var analyzer = new OpenNlpAnalyzer();
        final var testText = "Should felons who have completed their sentence be allowed to vote?";
        var stream = analyzer.tokenStream("body", testText);
        stream = new SeparateTokenTypesFilter(stream, Keep.TYPE_C_TOKEN);
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
