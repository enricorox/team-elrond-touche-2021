package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.*;
import java.util.*;

/**
 * Filter that add categories (extracted from word-net) as synonyms
 */
public class AddCategoryFilter extends TokenFilter {
    /**
     * Map word -> category
     */
    private final Map<String, String> categoryMap;

    /**
     * Last category to inject inside the stream
     */
    private String lastCategory = null;

    private final CharTermAttribute charTermAttribute;
    private final PositionIncrementAttribute positionIncrementAttribute;

    /**
     * Create a new {@link AddCategoryFilter}
     * @param stream input stream
     */
    public AddCategoryFilter(TokenStream stream) {
        super(stream);
        charTermAttribute = addAttribute(CharTermAttribute.class);
        positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
        this.categoryMap = new HashMap<>();
        loadCategories();
    }

    /**
     * Create a new {@link AddCategoryFilter}
     * @param stream input stream
     * @param categoryMap category map to re-use, if the size is 0 the categories are loaded into it
     */
    public AddCategoryFilter(TokenStream stream, Map<String, String> categoryMap) {
        super(stream);
        charTermAttribute = addAttribute(CharTermAttribute.class);
        positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
        this.categoryMap = categoryMap;
        if (categoryMap.size() == 0)
            loadCategories();
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (lastCategory != null) {
            charTermAttribute.setEmpty().append(lastCategory);
            lastCategory = null;
            positionIncrementAttribute.setPositionIncrement(0);
            return true;
        }

        if (!input.incrementToken()) return false;

        final var token = charTermAttribute.toString();
        lastCategory = categoryMap.get(token);
        positionIncrementAttribute.setPositionIncrement(1);
        return true;
    }

    /**
     * Custom parsing method for loading the categories inside {@code categoryMap}
     */
    private void loadCategories() {
        final String dirPath = Objects.requireNonNull(getClass().getResource("../../words.db/")).getPath();
        final var files = Objects.requireNonNull(new File(dirPath).listFiles((dir, name) -> new File(dir, name).isFile()));
        for (final File file: files) {
            final String category = file.getName().split("\\.")[1];
            try {
                final var fileIn = new FileReader(file);
                final var in = new BufferedReader(fileIn);
                in.lines().forEach(line -> {
                    var buff = new StringBuilder();
                    for (int i = 0; i < line.length(); i++) {
                        final char c = line.charAt(i);
                        if (c == '@') break;
                        if (Character.isLetterOrDigit(c)) {
                            buff.append(c);
                        } else if (!buff.isEmpty()) {
                            categoryMap.put(buff.toString(), category);
                            buff = new StringBuilder();
                        }
                    }
                    if (!buff.isEmpty()) categoryMap.put(buff.toString(), category);
                });
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
