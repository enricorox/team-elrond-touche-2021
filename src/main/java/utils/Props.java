package utils;

import java.io.IOException;
import java.util.Properties;

public class Props {
    public static Properties loadProps(Class sourceClass, String name) {
        final var in = sourceClass.getResourceAsStream(name);
        if (in == null) {
            throw new IllegalArgumentException("Missing props %s".formatted(name));
        }
        final var prop = new Properties();
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return prop;
    }
}
