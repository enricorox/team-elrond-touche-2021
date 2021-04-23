package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public class TypeConcatenateSynonymFilter extends TokenFilter {
    private final TypeAttribute typeAttribute;
    private final CharTermAttribute charTermAttribute;
    private final PositionIncrementAttribute positionIncrementAttribute;

    private String lastSynonym = null;

    public TypeConcatenateSynonymFilter(TokenStream input) {
        super(input);
        typeAttribute = addAttribute(TypeAttribute.class);
        charTermAttribute = addAttribute(CharTermAttribute.class);
        positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (lastSynonym != null) {
            charTermAttribute.setEmpty().append(lastSynonym);
            positionIncrementAttribute.setPositionIncrement(0);
            lastSynonym = null;
            return true;
        }
        if (input.incrementToken()) {
            lastSynonym = '<' + typeAttribute.type() + '>' + charTermAttribute.toString();
            return true;
        }
        return false;
    }
}
