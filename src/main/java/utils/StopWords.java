package utils;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.WordlistLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility for loading stop words
 */
public class StopWords {
    /**
     * Load stop words
     * @param name name of the file
     * @return parsed {@link CharArraySet} from the stop file
     */
    public static CharArraySet loadStopWords(String name) {
        try {
            final var res = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
            if (res == null) throw new IllegalArgumentException("Resource %s not found".formatted(name));
            final var in = new InputStreamReader(res);
            final var reader = new BufferedReader(in);
            final var set = WordlistLoader.getWordSet(reader);
            reader.close();
            in.close();
            res.close();
            return set;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
