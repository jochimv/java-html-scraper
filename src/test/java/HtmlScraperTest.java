import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlScraperTest {
    private HtmlScraper example;
    private HtmlScraper google;

    @BeforeEach
    public void setup() {
        this.example = new HtmlScraper("http://example.org/");
        this.google = new HtmlScraper("https://www.google.com/");
    }

    @Test
    public void testClear() {
        example.addSelector("p").addSubfolder("search").addInput("q", "hot+dogs");
        example.clearAll();
        assertEquals("", example.getInputsAndValues());
        assertEquals("", example.getSubfolder());
        assertEquals(0, example.getSelectors().size());

    }

    @Test
    public void testGetUrl() {
        google.addInput("q", "cute+kittens").addSubfolder("search");
        assertEquals("https://www.google.com/search?q=cute+kittens", google.getFullUrl());
        google.clearAll();
        google.setUrl("https://www.google.com");
        assertEquals("https://www.google.com", google.getFullUrl());
        google.addInput("q", "cute+kittens").addSubfolder("search");
        assertEquals("https://www.google.com/search?q=cute+kittens", google.getFullUrl());


    }

    @Test
    public void testGetHtml() {
        String html = this.example.getHtml();
        assertNotNull(html);
        assertTrue(html.contains("head"));
    }

    @Test
    public void testGetTags() {
        List<String> tags = example.getTags("p");
        int numberOfP = tags.size();
        assertEquals(2, numberOfP);
        boolean scrapedWithTag = tags.get(0).startsWith("<p");
        assertTrue(scrapedWithTag);

        List<String> scrapedWithoutTag = example.getTags("a", HtmlScraper.TYPE.WITHOUT_TAG);
        assertEquals("More information...", scrapedWithoutTag.get(0));
    }

    @Test
    public void testGetClass() {
        List<String> classes = google.getClass("o3j99 n1xJcf Ne6nSd");
        int size = classes.size();
        assertEquals(1, size);

        List<String> anotherClass = google.getClass("o3j99 qarstb", HtmlScraper.TYPE.WITHOUT_TAG);
        boolean scrapedWithoudTag = anotherClass.get(0).startsWith("<div class=");
        assertFalse(scrapedWithoudTag);
    }

    @Test
    public void testGetId() {
        String anotherId = google.getId("gbwa", HtmlScraper.TYPE.WITHOUT_TAG);
        boolean scrapedWithoutTags = anotherId.startsWith("<div id=\"gwba\"");
        assertFalse(scrapedWithoutTags);
    }

    @Test
    public void testExtractAttributes() {
        String attribute = google.getAttributeValue(0, "body", "jsmodel");
        assertEquals("hspDDf", attribute);

        List<String> attributes = google.getAttributeValues("div", "class", false);
        assertNotEquals(0, attributes.size());
        boolean containsDuplicates = containsDuplicates(attributes);
        assertFalse(containsDuplicates);

        attributes = google.getAttributeValues("div", "class", true);
        containsDuplicates = containsDuplicates(attributes);
        assertTrue(containsDuplicates);
    }

    @Test
    public void testGetHyperlinks() {
        List<String> hyperlinks = example.getHyperlinks();
        String hyperlink = hyperlinks.get(0);

        assertEquals("https://www.iana.org/domains/example", hyperlink);
        assertEquals(1, hyperlinks.size());

        hyperlinks = google.getHyperlinks();
        boolean validHyperlinks = checkHyperlinksValidity(hyperlinks);
        assertTrue(validHyperlinks);
    }

    @Test
    public void testGetSelectorResult() {
        example.addSelector("p", 0);
        List<String> selectorsResult = example.getSelectorsResult();
        assertEquals("This domain is for use in illustrative examples in documents. You may use this " +
                "domain in literature without prior coordination or asking for permission.", selectorsResult.get(0));
        assertEquals(1, selectorsResult.size());

        google.addSubfolder("search").addInput("q", "prague+weather").addSelector("div", "id", "wob_loc");
        List<String> selectorResults = google.getSelectorsResult();
        assertEquals("Praha", selectorResults.get(0));
        google.clearSelectors();
        google.clearSelectorResults();
        google.addSelector("div", "id", 1);
        selectorResults = google.getSelectorsResult();
        assertEquals(1, selectorResults.size());

    }

    private boolean containsDuplicates(List<String> attributeList) {
        for (int i = 0; i < attributeList.size(); i++) {
            String attribute = attributeList.get(i);
            int numberOfDuplicates = 0;
            for (int z = 0; z < attributeList.size(); z++) {
                String a = attributeList.get(z);
                if (attribute.equals(a)) {
                    numberOfDuplicates++;
                }
            }
            if (numberOfDuplicates > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean checkHyperlinksValidity(List<String> hyperlinks) {
        for (String hyperlink : hyperlinks) {
            if (!hyperlink.startsWith("http")) {
                return false;
            }
        }
        return true;
    }
}