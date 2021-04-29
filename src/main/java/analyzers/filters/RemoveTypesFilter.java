package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.util.Set;

/**
 * Filter that remove tokens with specified types from the stream
 */
public class RemoveTypesFilter extends TokenFilter {
    private final Set<String> typesToRemove;
    private final TypeAttribute typeAttribute;

    /**
     * Create a new {@link RemoveTypesFilter}
     * @param input input stream
     * @param typesToRemove set of types of the tokens to remove
     */
    public RemoveTypesFilter(TokenStream input, Set<String> typesToRemove) {
        super(input);
        this.typesToRemove = typesToRemove;
        typeAttribute = addAttribute(TypeAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        while (true) {
            if (!input.incrementToken()) return false;
            if (!typesToRemove.contains(typeAttribute.type())) return true;
        }
    }
}
