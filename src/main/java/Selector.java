public class Selector {
    private String tag;
    private String attribute;
    private String value;
    private int index;

    public Selector(String tag, String attribute, String value, int index) {
        this.tag = tag;
        this.attribute = attribute;
        this.value = value;
        this.index = index;
    }

    public String getTag() {
        return tag;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
