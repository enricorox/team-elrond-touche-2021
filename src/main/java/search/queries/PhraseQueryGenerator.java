package search.queries;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;

import java.io.IOException;

public class PhraseQueryGenerator {
    public static PhraseQuery create(final String fieldName, final TokenStream stream) throws IOException {
        final CharTermAttribute charTermAttribute = stream.getAttribute(CharTermAttribute.class);
        final var builder = new PhraseQuery.Builder();

        while (stream.incrementToken()) {
            final var token = charTermAttribute.toString();
            final var term = new Term(fieldName, token);
            builder.add(term);
        }

        return builder.build();
    }

    public static PhraseQuery create(final String fieldName, final Analyzer analyzer, final String text) throws IOException {
        return create(fieldName, analyzer.tokenStream(fieldName, text));
    }
}
