package analyzers.filters;

import java.io.*;
import java.util.*;

public class AddCategoryFilter {
    private final Map<String, String> categoryMap = new HashMap<>();

    public AddCategoryFilter() {
        loadCategories();
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

    public static void main(String[] args) {
        final var f = new AddCategoryFilter();
    }
}
