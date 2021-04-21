package analyzers.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.*;
import java.util.*;

public class AddCategoryFilter extends TokenFilter {
    private final Map<String, String> categoryMap = new HashMap<>();
    private String lastCategory = null;

    private final CharTermAttribute charTermAttribute;
    private final PositionIncrementAttribute positionIncrementAttribute;

    public AddCategoryFilter(TokenStream stream) {
        super(stream);
        charTermAttribute = addAttribute(CharTermAttribute.class);
        positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
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
//        charTermAttribute.setEmpty().append(token);
        positionIncrementAttribute.setPositionIncrement(1);
        return true;
    }

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
        System.err.printf("Loaded %d categorized words%n", categoryMap.size());
    }
}
