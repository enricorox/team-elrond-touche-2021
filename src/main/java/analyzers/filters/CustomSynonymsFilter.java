package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * A custom filter that add synonyms from word-net
 */
public class CustomSynonymsFilter extends TokenFilter {
    /**
     * Map of word -> synonyms
     */
    private final Map<String, Set<String>> synonymMap;
    /**
     * Queue of remaining synonyms to inject in stream
     */
    private final Queue<String> remainingSynonyms = new ArrayDeque<>();

    private final CharTermAttribute charTermAttribute;
    private final PositionIncrementAttribute positionIncrementAttribute;

    /**
     * Create a new {@link CustomSynonymsFilter}
     * @param input input stream
     */
    public CustomSynonymsFilter(TokenStream input) {
        super(input);
        this.charTermAttribute = addAttribute(CharTermAttribute.class);
        this.positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
        synonymMap = new HashMap<>();
        loadSynonymMap();
    }

    /**
     * Create a new {@link CustomSynonymsFilter}
     * @param input input stream
     * @param synonymMap synonyms map to reuse, if size is 0 the map is filled
     */
    public CustomSynonymsFilter(TokenStream input, Map<String, Set<String>> synonymMap) {
        super(input);
        this.charTermAttribute = addAttribute(CharTermAttribute.class);
        this.positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
        this.synonymMap = synonymMap;
        if (synonymMap.size() == 0)
            loadSynonymMap();
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!remainingSynonyms.isEmpty()) {
            final var syn = remainingSynonyms.poll();
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
        positionIncrementAttribute.setPositionIncrement(1);
        return true;
    }

    @Override
    public void reset() throws IOException {
        input.reset();
    }

    /**
     * Method for loading synonyms inside {@code synonymMap}
     */
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
