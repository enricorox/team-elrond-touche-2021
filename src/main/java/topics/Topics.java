package topics;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.apache.lucene.benchmark.quality.QualityQuery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Topics {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "topic")
    public List<Topic> topics;

    @Override
    public String toString() {
        final var build = new StringBuilder();
        build.append("Topics {\n");
        if (topics != null) {
            topics.forEach(item -> {
                build.append("    ");
                build.append(item);
                build.append("\n");
            });
        } else build.append("null\n");
        build.append("}");
        return build.toString();
    }

    public static Topics loadTopics(String path) throws IOException {
        final var file = new File(path);
        final var in = new FileInputStream(file);
        final var mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(in, Topics.class);
    }

    public QualityQuery[] getQualityQueries(){
        ArrayList<QualityQuery> qualityQueries= new ArrayList<>();
        topics.forEach((topic)->{
            var fields = new HashMap<String, String>();
            fields.put("title", topic.title);
            qualityQueries.add(new QualityQuery(String.valueOf(topic.number), fields));
        });
        return qualityQueries.toArray(new QualityQuery[0]);
    }
}
