package search.queries;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubsequencePhraseQueryGenerator {
    private static final Pattern symbolsPattern = Pattern.compile("[^a-z]");

    private static class MyTerm {
        private final List<String> subTerms = new ArrayList<>();

        public void add(String t) {subTerms.add(t);}
        public String get(int i) {return subTerms.get(i);}
        public int size() {return subTerms.size();}
    }

    private static class MyTermSequence {
        private final List<MyTerm> seq;

        public MyTermSequence() {seq = new ArrayList<>();}
        public MyTermSequence(int initialCapacity) {seq = new ArrayList<>(initialCapacity);}

        public void add(MyTerm t) {seq.add(t);}
        public void addAll(List<MyTerm> ts) {seq.addAll(ts);}
        public MyTerm get(int i) {return seq.get(i);}
        public int size() {return seq.size();}
    }

    public static interface groupLenTooBigCallback {
        //return a new groupLen or a number <= 0 for abort
        int callback(int termNum);
    }

    public static Query createQuery(String query, Analyzer analyzer, int groupLen, String field) throws IOException {
        return createQuery(query, analyzer, groupLen, field, tokenNum -> {
            throw new IllegalArgumentException("groupLen is too big");
        });
    }

    public static Query createQuery(String query, Analyzer analyzer, int groupLen, String field, groupLenTooBigCallback groupLenTooBigCallback) throws IOException {
        final var booleanQueryBuilder = new BooleanQuery.Builder();
        final var preparedTerms = prepareTerms(query, analyzer, field);
        if (groupLen > preparedTerms.size() || groupLen <= 0) {
            groupLen = groupLenTooBigCallback.callback(preparedTerms.size());
            if (groupLen <= 0) return null;
            if (groupLen > preparedTerms.size()) throw new IllegalArgumentException("groupLen is too big");
        }
        final List<MyTermSequence> sequences = genSequences(preparedTerms, groupLen);

        for (final MyTermSequence seq: sequences) {
            final var builder = new MultiPhraseQuery.Builder();
            for (int i = 0; i < seq.size(); i++) {
                final var iTerm = seq.get(i);
                final var tts = new Term[iTerm.size()];
                for (int j = 0; j < iTerm.size(); j++) {
                    tts[j] = new Term(field, iTerm.get(j));
                }
                builder.add(tts, i);
            }
            booleanQueryBuilder.add(builder.build(), BooleanClause.Occur.SHOULD);
        }

        return booleanQueryBuilder.build();
    }

    private static List<MyTermSequence> genSequences(List<MyTerm> preparedTerms, int groupLen) {
        final List<MyTermSequence> sequences = new ArrayList<>();
        int start = 0;
        int end = start + groupLen;
        while (end <= preparedTerms.size()) {
            int len = end - start + 1;
            final var seq = new MyTermSequence(len);

            seq.addAll(preparedTerms.subList(start, end));
            sequences.add(seq);

            start++;
            end++;
        }
        return sequences;
    }

    public static List<MyTerm> prepareTerms(String query, Analyzer analyzer, String field) throws IOException {
        final var tokenStream = analyzer.tokenStream(field, query);
        final var charAttribute = tokenStream.getAttribute(CharTermAttribute.class);
        final var positionAttribute = tokenStream.getAttribute(PositionIncrementAttribute.class);

        final List<MyTerm> terms = new ArrayList<>();

        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            final var term = charAttribute.toString();
            final var increment = positionAttribute.getPositionIncrement();

            if (increment > 0) { //new term
                final var l = new MyTerm();
                l.add(term);
                terms.add(l);
            } else { //synonym or term in the same position
                final var l = terms.get(terms.size() - 1);
                l.add(term);
            }
        }
        tokenStream.end();
        tokenStream.close();

        return terms;
    }
}
