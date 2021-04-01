package topics;

public class Topic {
    public int number;
    public String title;

    @Override
    public String toString() {
        return "Topic{" +
                "number=" + number +
                ", title='" + title + '\'' +
                '}';
    }
}
