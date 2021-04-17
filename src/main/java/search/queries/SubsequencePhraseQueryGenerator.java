package search.queries;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubsequencePhraseQueryGenerator {
    private static final Pattern symbolsPattern = Pattern.compile("[^a-z]");

    public static Query createQuery(String[] terms, int groupLen, String field) {
        if (groupLen > terms.length) throw new IllegalArgumentException("too big groupLen");
        final var booleanQueryBuilder = new BooleanQuery.Builder();
        final var preparedTerms = prepareTerms(terms);
        final List<String[][]> sequences = genSequences(preparedTerms, groupLen);

        for (final String[][] seq: sequences) {
            final var builder = new MultiPhraseQuery.Builder();
            for (int i = 0; i < seq.length; i++) {
                final var tts = new Term[seq[i].length];
                for (int j = 0; j < seq[i].length; j++) {
                    tts[j] = new Term(field, seq[i][j]);
                }
                builder.add(tts, i);
            }
            booleanQueryBuilder.add(builder.build(), BooleanClause.Occur.SHOULD);
        }

        return booleanQueryBuilder.build();
    }

    private static List<String[][]> genSequences(String[][] preparedTerms, int groupLen) {
        final List<String[][]> sequences = new ArrayList<>();
        int start = 0;
        int end = start + groupLen - 1;
        while (end < preparedTerms.length) {
            int len = end - start + 1;
            final var seq = new String[len][];
            System.arraycopy(preparedTerms, start, seq, 0, len);
            sequences.add(seq);

            start++;
            end++;
        }
        return sequences;
    }

    private static String[][] prepareTerms(String[] terms) {
        final String[][] preparedTerms = new String[terms.length][];
        for (int i = 0; i < terms.length; i++) {
            final var term = terms[i];
            Matcher matcher;
            if ((matcher = symbolsPattern.matcher(term)).matches()) {
                final var tt = new String[2];
                tt[0] = term;
                tt[1] = matcher.replaceAll("");
                preparedTerms[i] = tt;
            } else {
                preparedTerms[i] = new String[]{term};
            }
        }
        return preparedTerms;
    }
}
