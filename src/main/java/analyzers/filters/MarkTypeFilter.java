package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public class MarkTypeFilter extends TokenFilter {
    private final TypeAttribute typeAttribute;

    public MarkTypeFilter(TokenStream input) {
        super(input);
        typeAttribute = addAttribute(TypeAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            final var type = typeAttribute.type();
            if (type != null) {
                typeAttribute.setType('<' + type + '>');
            }
            return true;
        }
        return false;
    }
}
