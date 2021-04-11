package search;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

public interface BasicSearcher {
    void search() throws IOException, ParseException;
}
