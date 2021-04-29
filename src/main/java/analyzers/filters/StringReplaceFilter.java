package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

/**
 * Filter that replace a given string inside the stream
 */
public class StringReplaceFilter extends TokenFilter {
    /**
     * String pattern to replace
     */
    final String pattern;

    /**
     * Replacement String
     */
    final String replacement;

    private final CharTermAttribute charTermAttribute;

    /**
     * Create a new {@link String}
     * @param input input strea,
     * @param pattern pattern to replace
     * @param replacement replacement String
     */
    public StringReplaceFilter(TokenStream input, String pattern, String replacement) {
        super(input);
        this.pattern = pattern;
        this.replacement = replacement;
        charTermAttribute = addAttribute(CharTermAttribute.class);
        if (input.hasAttribute(PositionIncrementAttribute.class)) addAttribute(PositionIncrementAttribute.class);
        if (input.hasAttribute(TypeAttribute.class)) addAttribute(TypeAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) return false;
        final var token = charTermAttribute.toString();
        if (token.equals(pattern)) charTermAttribute.setEmpty().append(replacement);
        return true;
    }
}
