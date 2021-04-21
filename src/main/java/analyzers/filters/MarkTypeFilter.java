package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public class MarkTypeFilter extends TokenFilter {
    private final char start;
    private final char end;
    private final TypeAttribute typeAttribute;

    public MarkTypeFilter(TokenStream input, char start, char end) {
        super(input);
        this.start = start;
        this.end = end;
        typeAttribute = addAttribute(TypeAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            final var type = typeAttribute.type();
            if (type != null) {
                typeAttribute.setType(start + type + end);
            }
            return true;
        }
        return false;
    }
}
