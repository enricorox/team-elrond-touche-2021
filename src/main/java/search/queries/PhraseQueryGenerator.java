package search.queries;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;

import java.util.*;

public class PhraseQueryGenerator {
    public static Query create(final String fieldName, final String queryText, final int size) {
        final var queryBuilder = new BooleanQuery.Builder();
        final List<String[]> phraseList = new ArrayList<>();
        final Queue<String> tmpList = new LinkedList<>();
        Arrays.stream(queryText.split(" ")).forEach(word -> {
            if (tmpList.size() == size) {
                phraseList.add(tmpList.toArray(new String[0]));
                tmpList.remove();
            }
            tmpList.add(word);
        });
        if (tmpList.size() == size) phraseList.add(tmpList.toArray(new String[0]));
        phraseList.forEach(phrase -> {
            final var phraseQueryBuilder = new PhraseQuery.Builder();
            for (final var word: phrase) {
                phraseQueryBuilder.add(new Term(fieldName, word));
            }
            queryBuilder.add(phraseQueryBuilder.build(), BooleanClause.Occur.SHOULD);
        });
        return queryBuilder.build();
    }
}
