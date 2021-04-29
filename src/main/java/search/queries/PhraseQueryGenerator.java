package search.queries;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for generating phrase queries of consecutive tokens
 */
public class PhraseQueryGenerator {
    /**
     * Create a {@link Query} that search for phrases of @size dimension (considering also synonyms)
     * @param analyzer the analyzer to use for tokenization
     * @param fieldName name of the field to search
     * @param queryText the text of the query
     * @param size the dimension of the phrases
     * @return a new {@link Query} that fuse all the generated phrase-queries
     * @throws IOException
     */
    public static Query create(final Analyzer analyzer, final String fieldName, final String queryText, final int size) throws IOException {
        final var queryBuilder = new BooleanQuery.Builder();
        final List<Term[][]> phraseList = new ArrayList<>();
        final Queue<Term[]> tmpQueue = new LinkedList<>();

        queryToTerms(analyzer, fieldName, queryText).forEach(word -> {
            if (tmpQueue.size() == size) {
                phraseList.add(tmpQueue.toArray(new Term[0][0]));
                tmpQueue.remove();
            }
            tmpQueue.add(word);
        });
        if (tmpQueue.size() == size) phraseList.add(tmpQueue.toArray(new Term[0][0]));
        phraseList.forEach(phrase -> {
            final var phraseQueryBuilder = new MultiPhraseQuery.Builder();
            for (final var terms: phrase) {
                phraseQueryBuilder.add(terms);
            }
            queryBuilder.add(phraseQueryBuilder.build(), BooleanClause.Occur.SHOULD);
        });
        return queryBuilder.build();
    }

    private static List<Term[]> queryToTerms(final Analyzer analyzer, final String fieldName, final String queryText) throws IOException {
        final TokenStream stream = analyzer.tokenStream(fieldName, queryText);
        final CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);
        final PositionIncrementAttribute positionIncrementAttribute = stream.addAttribute(PositionIncrementAttribute.class);
        final List<List<Term>> list = new ArrayList<>();
        stream.reset();
        while (stream.incrementToken()) {
            final var word = charTermAttribute.toString();
            final var isSynonym = positionIncrementAttribute.getPositionIncrement() == 0;
            if (isSynonym) {
                final var last = list.get(list.size() - 1);
                last.add(new Term(fieldName, word));
            } else {
                final var nl = new LinkedList<Term>();
                nl.add(new Term(fieldName, word));
                list.add(nl);
            }
        }
        stream.close();
        return list.stream().map(l -> l.toArray(new Term[0])).collect(Collectors.toList());
    }
}
