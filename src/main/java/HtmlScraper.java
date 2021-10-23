import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HtmlScraper {
    private Document document;
    private String url;
    private StringBuilder subfolder;
    private Map<String, String> inputsAndValues;
    private ArrayList<String> hyperlinks;
    private ArrayList<Selector> selectors;
    private ArrayList<String> selectorsResult;
    private boolean containsGoodValues = false;

    public enum TYPE {
        WITH_TAG, WITHOUT_TAG
    }

    public HtmlScraper(String url) {
        this.hyperlinks = new ArrayList<>();
        this.inputsAndValues = new HashMap<>();
        this.url = url;
        this.subfolder = new StringBuilder("");
        this.selectorsResult = new ArrayList<>();
        this.selectors = new ArrayList<>();
    }

    public StringBuilder getSubfolder() {
        return subfolder;
    }

    public String getBaseUrl() {
        return this.url;
    }

    public String getFullUrl() {
        return buildUrl().toString();
    }

    public Map<String, String> getInputsAndValues() {
        return inputsAndValues;
    }


    public HtmlScraper setSubfolder(String subfolder) {
        clearSubfolder();
        this.subfolder.append(subfolder);
        return this;
    }

    public HtmlScraper setInputsAndValues(Map<String, String> inputsAndValues) {
        this.inputsAndValues = inputsAndValues;
        return this;
    }

    public HtmlScraper setUrl(String url) {
        this.url = url;
        return this;
    }

    public HtmlScraper setSelectors(ArrayList<Selector> selectors) {
        this.selectors = selectors;
        return this;
    }

    public HtmlScraper addSubfolder(String subfolder) {
        this.subfolder.append(subfolder);
        return this;
    }

    public HtmlScraper addInput(String input, String value) {
        inputsAndValues.put(input, value);
        return this;
    }

    public HtmlScraper addSelector(String tag) {
        selectors.add(new Selector(tag, null, null, -1));
        return this;
    }

    public HtmlScraper addSelector(String tag, int index) {
        selectors.add(new Selector(tag, null, null, index));
        return this;
    }

    public HtmlScraper addSelector(String tag, String attribute, int index) {
        selectors.add(new Selector(tag, attribute, null, index));
        return this;
    }

    public HtmlScraper addSelector(String tag, String attribute) {
        selectors.add(new Selector(tag, attribute, null, -1));
        return this;
    }

    public HtmlScraper addSelector(String tag, String attribute, String value) {
        selectors.add(new Selector(tag, attribute, value, -1));
        return this;
    }

    public HtmlScraper addSelector(String tag, String attribute, String value, int index) {
        selectors.add(new Selector(tag, attribute, value, index));
        return this;
    }

    public Document getDocument() {
        return document;
    }

    public HtmlScraper clearAttributes() {
        inputsAndValues.clear();
        return this;
    }

    public HtmlScraper clearSubfolder() {
        subfolder.replace(0, subfolder.length(), "");
        return this;
    }

    public HtmlScraper clearSelectorResults() {
        selectorsResult.clear();
        return this;
    }

    public HtmlScraper clearSelectory() {
        selectors.clear();
        return this;
    }

    public HtmlScraper clearAll() {
        clearAttributes();
        clearSubfolder();
        clearSelectorResults();
        clearSelectory();
        return this;
    }

    public String getHtml() {
        finalizeAndConnect();
        return document.html();
    }

    public ArrayList<String> getTags(String tag) {
        return getTags(tag, TYPE.WITH_TAG);
    }

    public ArrayList<String> getTags(String tag, TYPE scrapeType) {
        finalizeAndConnect();
        Elements elements = this.document.getElementsByTag(tag);
        ArrayList<String> tags = new ArrayList<>();
        for (Element e : elements) {
            if (scrapeType == TYPE.WITH_TAG) {
                tags.add(e.toString());
            } else {
                tags.add(e.html());
            }
        }

        if (tags.size() >= 1) {
            return tags;
        } else {
            System.out.println("No tag " + tag + " in URL " + url + " has been found.");
            return null;
        }
    }

    public ArrayList<String> getClass(String className) {
        return getClass(className, TYPE.WITH_TAG);
    }

    public ArrayList<String> getClass(String className, TYPE scrapeType) {
        finalizeAndConnect();
        Elements elements = this.document.getElementsByClass(className);
        ArrayList<String> classes = new ArrayList<>();
        for (Element e : elements) {
            if (scrapeType == TYPE.WITH_TAG) {
                classes.add(e.toString());
            } else {
                classes.add(e.html());
            }
        }
        if (classes.size() >= 1) {
            return classes;
        } else {
            System.out.println("No class " + className + " in URL " + url + " has been found.");
            return null;
        }
    }

    public String getId(String id) {
        return getId(id, TYPE.WITH_TAG);
    }

    public String getId(String id, TYPE scrapeType) {
        finalizeAndConnect();
        Element element = document.getElementById(id);
        if (element == null) {
            System.out.println("No element " + id + " in url " + buildUrl() + " has been found");
            return null;
        } else if (scrapeType == TYPE.WITH_TAG) {
            return element.toString();
        } else {
            return element.html();
        }
    }

    public String extractAttribute(int tagIndex, String tag, String attribute) {
        finalizeAndConnect();
        Elements elements = document.getElementsByTag(tag);
        Element element = elements.get(tagIndex);
        return element.attr(attribute);
    }

    public ArrayList<String> extractAttributes(String tag, String attribute, boolean duplicates) {
        finalizeAndConnect();
        Elements elements = document.getElementsByTag(tag);
        ArrayList<String> arguments = new ArrayList<>();
        for (Element e : elements) {
            String argument = e.attr(attribute);
            if (argument.equals("") || (!duplicates && arguments.contains(argument))) {
                continue;
            } else {
                arguments.add(argument);
            }
        }
        if (arguments.size() >= 1) {
            return arguments;
        } else {
            System.out.println("Unable to extract attribute " + attribute + " from URL " + buildUrl());
            return null;
        }
    }

    public ArrayList<String> getHyperlinks() {
        if (this.hyperlinks.isEmpty()) {
            ArrayList<String> hyperlinks = extractAttributes("a", "href", false);
            ArrayList<String> hyperlinksCorrecred = new ArrayList<>();
            for (String hyperlink : hyperlinks) {
                if (hyperlink.equals("/")) {
                    hyperlinksCorrecred.add(this.url + hyperlink);
                } else if (hyperlink.startsWith("/") && hyperlink.length() > 1) {
                    hyperlinksCorrecred.add(this.url + this.subfolder + hyperlink);
                } else {
                    hyperlinksCorrecred.add(hyperlink);
                }
            }
            this.hyperlinks = hyperlinksCorrecred;
        }
        return this.hyperlinks;
    }

    public ArrayList<String> getSelectorsResult() {
        if (selectors.isEmpty() && selectorsResult.isEmpty()) {
            System.out.println("No HTML selectors inserted");
            return null;
        } else if (!selectors.isEmpty() && selectorsResult.isEmpty()) {
            finalizeAndConnect();
            scrapeSelectorResult2(document.getAllElements(), 0);
        }
        return this.selectorsResult;
    }

    private void scrapeSelectorResult() {
        finalizeAndConnect();
        String selector = buildSelector();
        Elements elements = document.select(selector);
        for (Element e : elements) {
            selectorsResult.add(e.html());
        }
    }

    public void scrapeSelectorResult2(Elements e, int i) {
        if (i == selectors.size()) {
            for (Element el : e) {
                selectorsResult.add(el.html());
            }
            return;
        }
        Selector s = selectors.get(i);
        String tag = s.getTag();
        String attribute = s.getAttribute();
        String value = s.getValue();
        int index = s.getIndex();
        Elements elements = new Elements();
        Element element = null;


        if (tag != null && attribute == null && value == null && index == -1) {
            elements = e.select(tag);
            containsGoodValues = true;
        } else if (tag != null && attribute == null && value == null && index >= 0) {
            if (containsGoodValues) {
                for (Element el : e) {
                    element = el.select(tag).get(index);
                    elements.add(element);
                }
            } else {
                element = e.select(tag).get(index);
                elements.add(element);
                this.containsGoodValues = true;
            }

        } else if (tag != null && attribute != null && value == null && index == -1) {
            elements = e.select(tag + "[" + attribute + "]");
            containsGoodValues = true;
        } else if (tag != null && attribute != null && value == null && index >= 0) {
            if (containsGoodValues) {
                for (Element el : e) {
                    element = el.select(tag + "[" + attribute + "]").get(index);
                    elements.add(element);
                }
            } else {
                element = e.select(tag).get(index);
                elements.add(element);
                this.containsGoodValues = true;
            }

        } else if (tag != null && attribute != null && value != null && index == -1) {
            elements = e.select(tag + "[" + attribute + "='" + value + "']");
            containsGoodValues = true;
        } else {
            if (containsGoodValues) {
                for (Element el : e) {
                    element = el.select(tag + "[" + attribute + "$=" + value + "]").get(index);
                    elements.add(element);
                }
            } else {
                element = e.select(tag).get(index);
                elements.add(element);
                this.containsGoodValues = true;
            }

        }
        scrapeSelectorResult2(elements, i + 1);

    }


    private String buildSelector() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Selector t : selectors) {
            if (t.getAttribute() == null) {
                stringBuilder.append(t.getTag() + " > ");
            } else if (t.getValue() == null) {
                stringBuilder.append(t.getTag() + "[" + t.getAttribute() + "] > ");
            } else {
                stringBuilder.append(t.getTag() + "[" + t.getAttribute() + "$=" + t.getValue() + "] > ");
            }
        }
        return stringBuilder.replace(stringBuilder.length() - 3, stringBuilder.length(), "").toString();
    }

    private void finalizeAndConnect() {
        StringBuilder finalUrl = buildUrl();
        connect(finalUrl);
    }

    private void connect(StringBuilder finalUrl) {
        Connection connection = Jsoup.connect(finalUrl.toString()).timeout(10000);
        try {
            this.document = connection.get();
        } catch (IOException e) {
            System.out.println("Problem instantiating a HtmlScraper on " + buildUrl());
        }
    }

    private StringBuilder buildUrl() {
        StringBuilder finalUrl = new StringBuilder(url + subfolder + (inputsAndValues.size() > 0 ? "?" : ""));
        for (Map.Entry<String, String> entry : inputsAndValues.entrySet()) {
            finalUrl.append(entry.getKey() + "=" + entry.getValue() + "&");
        }
        if (this.inputsAndValues.size() > 0) {
            finalUrl.deleteCharAt(finalUrl.length() - 1);
        }
        return finalUrl;
    }

    public void automatizeSelectors(String path, int days, int hours, int minutes, int seconds, int ms) {
        long miliseconds = calculateMiliseconds(days, hours, minutes, seconds, ms);
        Path logFile = getPath(path);
        createPath(logFile);
        Runnable r = createRunnableForSelectors(logFile);
        automatize(miliseconds, r);
    }

    public void htmlSnapshots(String targetFolder, int days, int hours, int minutes, int seconds, int ms) {
        long miliseconds = calculateMiliseconds(days, hours, minutes, seconds, ms);
        Path target = getPath(targetFolder);
        createPath(target);
        Runnable r = createRunnableForSnapshots(target);
        automatize(miliseconds, r);
    }

    private Runnable createRunnableForSnapshots(Path targetFolder) {
        Runnable r = () -> {
            Timestamp timeStamp = getTimesStamp();
            try {
                String delimeter = FileSystems.getDefault().getSeparator();
                String inputPath = targetFolder + delimeter + timeStamp.toString().replaceAll(" ", "_") + ".html";
                Files.createFile(FileSystems.getDefault().getPath(inputPath));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(inputPath));
                String html = getHtml();

                bufferedWriter.write(html);

                bufferedWriter.close();
            } catch (IOException e) {
                System.out.println("Unable to write a log: " + e.getMessage());
                e.printStackTrace();
            }
        };
        return r;
    }

    private void automatize(long miliseconds, Runnable r) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(r, 0, miliseconds, TimeUnit.MILLISECONDS);
    }

    private Runnable createRunnableForSelectors(Path logFile) {
        Runnable r = () -> {
            Timestamp timeStamp = getTimesStamp();
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile.toString(), true));
                bufferedWriter.write(timeStamp + "");
                bufferedWriter.newLine();

                ArrayList<String> values = getSelectorsResult();
                for (String value : values) {
                    bufferedWriter.write(value);
                    bufferedWriter.newLine();
                }
                clearSelectorResults();
                bufferedWriter.close();
            } catch (IOException e) {
                System.out.println("Unable to write a log: " + e.getMessage());
                e.printStackTrace();
            }
        };
        return r;
    }

    private Timestamp getTimesStamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    private void createPath(Path logFile) {
        try {
            if (Files.isRegularFile(logFile) && !Files.exists(logFile)) {
                Files.createFile(logFile);
            } else if (Files.isDirectory(logFile) && !Files.exists(logFile)) {
                Files.createDirectories(logFile);
            }
        } catch (IOException e) {
            System.out.println("Problem with checking/instantiating log file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Path getPath(String path) {
        return FileSystems.getDefault().getPath(path);
    }

    private long calculateMiliseconds(int days, int hours, int minutes, int seconds, long ms) {
        return days * 86400000L + hours * 3600000L + minutes * 60000L + seconds * 1000L + ms;
    }
}
