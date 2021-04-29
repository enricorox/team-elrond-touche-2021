package Analizer;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.WordlistLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StopWord {
    public static CharArraySet loadStopWords(String name) {
        final var globalSet = new CharArraySet(100, true);
        try {
            final var res = StopWord.class.getResourceAsStream("../"+name);
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
