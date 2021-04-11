package topics;

public class Topic {
    public int number;
    public String title;
    public String description;
    public String narrative;

    @Override
    public String toString() {
        return "Topic{" +
                "number=" + number +
                ", title='" + title + '\'' +
                '}';
    }
}
