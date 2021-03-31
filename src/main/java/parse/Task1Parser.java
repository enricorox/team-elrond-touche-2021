package parse;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class Task1Parser extends DocumentParser {
    private ParsedDocument document = null;

    private static final int BODY_SIZE = 1024 * 8;

    private final ObjectMapper objectMapper;
    private final JsonParser jsonParser;

    private boolean startedReading = false;

    public Task1Parser(Reader in) {
        super(new BufferedReader(in));
        objectMapper = new ObjectMapper();
        try {
            jsonParser = objectMapper.createParser(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read from Reader", e);
        }
    }

    @Override
    protected ParsedDocument parse() {
        return document;
    }

    @Override
    public boolean hasNext() {
        try {
            if (!startedReading) {
                startedReading = true;
                while (jsonParser.nextToken() != JsonToken.FIELD_NAME || !jsonParser.getCurrentName().equals("arguments"))
                    ; //empty while body
                if (jsonParser.nextToken() != JsonToken.START_ARRAY)
                    throw new IllegalArgumentException("should be an array");
                jsonParser.nextToken();
            } else if (jsonParser.nextToken() == JsonToken.END_ARRAY) {
                // end of the documents for this file
                jsonParser.close();
                in.close();
                return false;
            }

            if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
                System.err.printf("--> %s%n", jsonParser.currentToken());
                throw new IllegalArgumentException("should be an object");
            }


            //now we are at the start of the documents array
            //and the current token is '{'

            while (true) { // to give the ability to skip a document if the id is missing
                final JsonNode root = objectMapper.readTree(jsonParser);
                if (root == null) throw new IllegalArgumentException("Not valid json");

                final String id;
                if (root.hasNonNull("id")) id = root.get("id").asText();
                else continue;

                final var body = new StringBuilder(BODY_SIZE);

                if (root.hasNonNull("context")) {
                    final var context = root.get("context");

                    //domain
                    if (context.hasNonNull("sourceDomain")) {
                        body.append(context.get("sourceDomain").asText());
                        body.append(" ");
                    }

                    // title
                    if (context.hasNonNull("discussionTitle")) {
                        body.append(context.get("discussionTitle").asText());
                        body.append(" ");
                    }
                }

                // text content
                if (root.hasNonNull("premises") ){
                    root.get("premises").elements().forEachRemaining(childNode -> {
                        if (childNode.hasNonNull("text")) {
                            body.append(childNode.get("text").asText());
                            body.append(" ");
                        }
                    });
                }

                document = new ParsedDocument(
                        id,
                        body.toString()
                );

                return true;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Read failed", e);
        }
    }
}
