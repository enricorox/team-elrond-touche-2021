package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

public class CustomSynonymsFilter extends TokenFilter {
    private final Map<String, Set<String>> synonymMap = new HashMap<>();
    private final CharTermAttribute charTermAttribute;
    private final PositionIncrementAttribute positionIncrementAttribute;
    private final Queue<String> remainingSynonyms = new ArrayDeque<>();

    private AttributeSource.State state;

    public CustomSynonymsFilter(TokenStream input) {
        super(input);
        this.charTermAttribute = addAttribute(CharTermAttribute.class);
        this.positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
        loadSynonymMap();
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!remainingSynonyms.isEmpty()) {
            final var syn = remainingSynonyms.poll();
            restoreState(state);
            charTermAttribute.setEmpty().append(syn);
            positionIncrementAttribute.setPositionIncrement(0);
            return true;
        }

        if (!input.incrementToken()) return false;

        final var token = charTermAttribute.toString();
        final var synList = synonymMap.get(token);
        if (synList != null && synList.size() > 1) {
            for (final var s: synList) {
                if (s.equals(token)) continue;
                remainingSynonyms.add(s);
            }
        }
//        charTermAttribute.setEmpty().append(token);
        positionIncrementAttribute.setPositionIncrement(1);
        return true;
    }

    @Override
    public void reset() throws IOException {
        input.reset();
    }

    private void loadSynonymMap() {
        try {
            final var res = CustomSynonymsFilter.class.getResourceAsStream("../../wn_s.pl");
            final var sreader = new InputStreamReader(Objects.requireNonNull(res));
            final var reader = new BufferedReader(sreader);
            final Map<Long, List<String>> tempMap = new HashMap<>();
            reader.lines().forEach(line -> {
                final var parts = line.substring(2, line.length() - 2)
                        .split(",");
                final var id = Long.parseLong(parts[0]);
                final var value = parts[2].substring(1, parts[2].length() - 1);
                final var list = tempMap.getOrDefault(id, new ArrayList<>());
                list.add(value);
                tempMap.put(id, list);
            });
            for (final var list: tempMap.values()) {
                for (final var value: list) {
                    final var set = synonymMap.getOrDefault(value, new HashSet<>());
                    set.addAll(list.stream().filter(s -> !s.contains(" ")).toList()); //#####à only one-word synonyms
                    synonymMap.put(value, set);
                }
            }
            reader.close();
            sreader.close();
            res.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
