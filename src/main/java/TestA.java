import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilterFactory;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.InputStreamReader;
import java.util.TreeMap;

public class TestA {
    public static void main(String[] args) throws Exception {
        final var synonFilterMap = new TreeMap<String, String>();
        synonFilterMap.put("synonyms", "wn_s.pl");
        synonFilterMap.put("format", "wordnet");
        final Analyzer a = CustomAnalyzer.builder()
                .withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
//                .addTokenFilter(EnglishPossessiveFilterFactory.class)
                .addTokenFilter(EnglishMinimalStemFilterFactory.class)
//                .addTokenFilter(PorterStemFilterFactory.class)
//                .addTokenFilter(StopFilterFactory.class)
//                .addTokenFilter(SynonymGraphFilterFactory.class, synonFilterMap)
                .build();
        var s = a.tokenStream("body", new InputStreamReader(System.in));
        CharTermAttribute att = s.getAttribute(CharTermAttribute.class);
        s.reset();
        while (s.incrementToken()) {
            System.out.println(att.toString());
        }
    }
}
