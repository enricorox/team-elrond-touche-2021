package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

public class BreakHyphensFilter extends TokenFilter {
    private final Pattern minus = Pattern.compile("-");
    private final Queue<String[]> queue = new LinkedList<>();
    private final CharTermAttribute charTermAttribute;
    private final TypeAttribute typeAttribute;

    public BreakHyphensFilter(TokenStream input) {
        super(input);
        charTermAttribute = addAttribute(CharTermAttribute.class);
        typeAttribute = addAttribute(TypeAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!queue.isEmpty()) {
            final var token = queue.remove();
            charTermAttribute.setEmpty().append(token[0]);
            typeAttribute.setType(token[1]);
            return true;
        }
        if (input.incrementToken()) {
            final var token = charTermAttribute.toString();
            if (token.contains("-")) {
                final var splits = minus.split(token);
                final var type = typeAttribute.type();
                for (final var s: splits) {
                    queue.add(new String[]{s, type});
                }
                if (!queue.isEmpty()) {
                    final var ret = queue.remove();
                    charTermAttribute.setEmpty().append(ret[0]);
                }
            }
            return true;
        }
        return false;
    }
}
