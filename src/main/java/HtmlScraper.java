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
    private ArrayList<String> subfolders;
    private Map<String, String> inputsAndValues;
    private ArrayList<Selector> selectors;

    private ArrayList<String> hyperlinks;
    private ArrayList<String> selectorsResult;
    private boolean containsGoodValues = false;

    /**
     * TYPE is an enum which specifies whether we want to scrape elements with, or without a tag.
     1. with tags <p>lorem ipsum</p>
     2. without tags, Lorem ipsum
     */
    public enum TYPE {
        WITH_TAG, WITHOUT_TAG
    }

    public HtmlScraper(String url) {
        this.hyperlinks = new ArrayList<>();
        this.inputsAndValues = new HashMap<>();
        this.url = url;
        this.subfolders = new ArrayList<>();
        this.selectorsResult = new ArrayList<>();
        this.selectors = new ArrayList<>();
    }

    public String getSubfolder() {
        return buildSubfolder();
    }

    public String getBaseUrl() {
        return this.url;
    }

    public String getFullUrl() {
        return buildUrl().toString();
    }

    public String getInputsAndValues() {
        return buildInputsAndValues();
    }


    public HtmlScraper setSubfolder(ArrayList<String> subfolders) {
        clearSubfolders();
        this.subfolders = subfolders;
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
        this.subfolders.add(subfolder);
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

    public List<Selector> getSelectors() {
        return selectors;
    }

    public HtmlScraper clearInputs() {
        inputsAndValues.clear();
        return this;
    }

    public HtmlScraper clearSubfolders() {
        subfolders.clear();
        return this;
    }

    public HtmlScraper clearSelectorResults() {
        selectorsResult.clear();
        containsGoodValues = false;
        return this;
    }

    public HtmlScraper clearSelectors() {
        selectors.clear();
        containsGoodValues = false;
        return this;
    }

    public HtmlScraper clearAll() {
        clearInputs();
        clearSubfolders();
        clearSelectorResults();
        clearSelectors();
        containsGoodValues = false;
        return this;
    }

    /**
     * A method that gets the HTML code of the website and return it as a String
     */
    public String getHtml() {
        finalizeAndConnect();
        return document.html();
    }

    /**
     * A method that gets the all certain tags of the HTML document and return it as a list of strings. By default, it will scrape the elements
     * with the tag, e.g. <p>lorem ipsum</p>
     */
    public List<String> getTags(String tag) {
        return getTags(tag, TYPE.WITH_TAG);
    }

    /**
     * A method that gets the all certain tags of the HTML document and return it as a list of strings.
     * @param scrapeType specifies whether do we want to scrape the tags
     *                   1. with keeping tags <p>lorem ipsum</p>
     *                   2. without keeping the tags, Lorem ipsum
     */
    public List<String> getTags(String tag, TYPE scrapeType) {
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

    /**
     * A method that gets all elements having a certain class in an HTML document and return it as a list of strings.
     * By default, it will scrape the elements with the tag, e.g. <p class="xyz">lorem ipsum</p>
     */
    public List<String> getClass(String className) {
        return getClass(className, TYPE.WITH_TAG);
    }

    /**
     * A method that gets all elements having a certain class in an HTML document and return it as a list of strings.
     *
     * @param scrapeType specifies whether do we want to scrape the tags
     *                   1. with keeping tags <p class="xyz">lorem ipsum</p>
     *                   2. without keeping the tags, Lorem ipsum
     */
    public List<String> getClass(String className, TYPE scrapeType) {
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

    /**
     * A method that gets an element having a certain id in a nHTML document and return it as a string.
     * By default, it will scrape the element with the tag, e.g. <p id="xyz">lorem ipsum</p>
     */
    public String getId(String id) {
        return getId(id, TYPE.WITH_TAG);
    }

    /**
     * A method that gets an element having a certain id in an HTML document and return it as a string.
     *
     * @param scrapeType specifies whether do we want to scrape the tags
     *                   1. with keeping tags <p id="xyz">lorem ipsum</p>
     *                   2. without keeping the tags, Lorem ipsum
     */
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

    /**
     * A method for extracting an attribute value from an n-th certain tag of an HTML document. It is returned as a String.
     */
    public String getAttributeValue(int tagIndex, String tag, String attribute) {
        finalizeAndConnect();
        Elements elements = document.getElementsByTag(tag);
        Element element = elements.get(tagIndex);
        return element.attr(attribute);
    }

    /**
     * A method for extracting all attribute values of a certain tag, for example we can scrape all text colors used in a webpage, and see which one
     * is used the most.
     *
     * @param duplicates specifies whether we want to have dduplicate values in the result list
     */
    public List<String> getAttributeValues(String tag, String attribute, boolean duplicates) {
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

    /**
     * A method that is invoked only if we haven't scraped the hyperlinks yet (because of high time complexity),
     * or if the hyperlink results have been cleared. It automatically converts the relative paths to absolute paths,
     * and filters out the empty values.
     */
    public List<String> getHyperlinks() {
        if (this.hyperlinks.isEmpty()) {
            List<String> hyperlinks = getAttributeValues("a", "href", false);
            ArrayList<String> hyperlinksCorrecred = new ArrayList<>();
            for (String hyperlink : hyperlinks) {
                if (hyperlink.equals("/")) {
                    hyperlinksCorrecred.add(this.url + hyperlink);
                } else if (hyperlink.startsWith("/") && hyperlink.length() > 1) {
                    hyperlinksCorrecred.add(this.url + buildSubfolder() + hyperlink);
                } else if (!hyperlink.equals("/") && hyperlink.length() <= 1) {
                    continue;
                } else {
                    hyperlinksCorrecred.add(hyperlink);
                }
            }
            this.hyperlinks = hyperlinksCorrecred;
        }
        return this.hyperlinks;
    }

    /**
     * A method that is invoked only if we haven't scraped the selector results yet (because of high time complexity)
     * or if the selector results have been cleared.
     */
    public List<String> getSelectorsResult() {
        if (selectors.isEmpty() && selectorsResult.isEmpty()) {
            System.out.println("No HTML selectors inserted");
            return null;
        } else if (!selectors.isEmpty() && selectorsResult.isEmpty()) {
            finalizeAndConnect();
            scrapeSelectorResult(document.getAllElements(), 0);
        }
        return this.selectorsResult;
    }

    /**
     * A method for managing the recursion of scrapeSelectorResults.
     *
     * @param elements at first turn are the all elements of the HTML document,
     *                 on the second and every following turn are the elements that meets the n-th tag selector.
     *                 On the last turn - if the given index is bigger than the last selector index (there are no more selectors),
     *                 the remaining elements are saved into the selector results.
     */
    private void scrapeSelectorResult(Elements elements, int index) {
        if (index == selectors.size()) {
            for (Element el : elements) {
                selectorsResult.add(el.html());
            }
            return;
        }
        Selector s = selectors.get(index);
        Elements scrapedElements = getRightElementsForSelector(elements, s);

        scrapeSelectorResult(scrapedElements, index + 1);
    }

    /**
     * A method for scraping the elements from the webpage by given selectors.
     * The elements will be selected based on order of the selectors, eg:
     * 'div > p' will select all p elements inside div. This method checks for "right values" in case of a specified index -
     * because it uses recursion and the elements at the start of recursion are all elements of the document. It needs to select all elements
     * with the given tag, and then get the element with a specified index. Otherwise, It would loop through all the document elements,
     * and select all theirs n-th element. If an index is specified on the second and every next selector, the program knows that it contains good elements,
     * and can loop through the given list, getting the n-th element of the second tag.
     */
    private Elements getRightElementsForSelector(Elements givenElements, Selector selector) {
        Elements scrapedElements = new Elements();
        String tag = selector.getTag();
        String attribute = selector.getAttribute();
        String value = selector.getValue();
        int index = selector.getIndex();
        Element element;

        if (tag != null && attribute == null && value == null && index == -1) {
            scrapedElements = givenElements.select(tag);
            containsGoodValues = true;
        } else if (tag != null && attribute == null && value == null && index >= 0) {
            if (containsGoodValues) {
                for (Element el : givenElements) {
                    try {
                        element = el.select(tag).get(index);
                        scrapedElements.add(element);
                    } catch (Exception exception) {
                    }
                }
            } else {
                element = givenElements.select(tag).get(index);
                scrapedElements.add(element);
                this.containsGoodValues = true;
            }

        } else if (tag != null && attribute != null && value == null && index == -1) {
            scrapedElements = givenElements.select(tag + "[" + attribute + "]");
            containsGoodValues = true;
        } else if (tag != null && attribute != null && value == null && index >= 0) {
            if (containsGoodValues) {
                for (Element el : givenElements) {
                    element = el.select(tag + "[" + attribute + "]").get(index);
                    scrapedElements.add(element);
                }
            } else {
                element = givenElements.select(tag).get(index);
                scrapedElements.add(element);
                this.containsGoodValues = true;
            }

        } else if (tag != null && attribute != null && value != null && index == -1) {
            scrapedElements = givenElements.select(tag + "[" + attribute + "='" + value + "']");
            containsGoodValues = true;
        } else {
            if (containsGoodValues) {
                for (Element el : givenElements) {
                    element = el.select(tag + "[" + attribute + "=" + value + "]").get(index);
                    scrapedElements.add(element);
                }
            } else {
                element = givenElements.select(tag).get(index);
                scrapedElements.add(element);
                this.containsGoodValues = true;
            }
        }
        return scrapedElements;
    }

    /**
     * A method that is composed of two steps:
     * 1. step is building the final URL
     * 2. step is connecting to the URL, and getting the document, which contains the HTML code
     * along with all elements of the website
     */
    private void finalizeAndConnect() {
        String finalUrl = buildUrl();
        connect(finalUrl);
    }

    /**
     * A method that connects to the given URL, and gets the document, which contains the HTML code
     * along with all elements of the website
     */
    private void connect(String finalUrl) {
        Connection connection = Jsoup.connect(finalUrl).timeout(10000);
        try {
            this.document = connection.get();
        } catch (IOException e) {
            System.out.println("Problem instantiating an HtmlScraper on " + buildUrl());
            e.printStackTrace();
        }
    }

    /**
     * A method that builds the full URL from the given URL, subfolders, and inputs with values
     */
    private String buildUrl() {
        String subfolder = buildSubfolder();
        String inputsAndValues = buildInputsAndValues();
        return url + subfolder + inputsAndValues;
    }

    /**
     * A method, that builds input and value combos.
     * The final String will be passed to the URL, which is used for passing the values to the input fields
     */
    private String buildInputsAndValues() {
        StringBuilder inputsAndValues = new StringBuilder("?");
        if (this.inputsAndValues.size() > 0) {
            for (Map.Entry<String, String> entry : this.inputsAndValues.entrySet()) {
                inputsAndValues.append(entry.getKey() + "=" + entry.getValue() + "&");
            }
        } else {
            return "";
        }
        return inputsAndValues.substring(0, inputsAndValues.length() - 1);
    }

    /**
     * A method, that builds subfolder path on current webpage.
     */
    private String buildSubfolder() {
        StringBuilder builtSubfolder = new StringBuilder("/");
        for (String subfolder : subfolders) {
            builtSubfolder.append(subfolder + "/");
        }
        if (url.endsWith("/") && subfolders.size() != 0) {
            return builtSubfolder.substring(1, builtSubfolder.length() - 1);
        } else if (!url.endsWith("/") && subfolders.size() != 0) {
            return builtSubfolder.substring(0, builtSubfolder.length() - 1);
        } else {
            return "";
        }
    }

    /**
     * A method for scraping-by-selectors automatization.
     * The user specifies a log file path, and how often the webpage scraping should happen. Then, the scraping will happen every given amount of time.
     * The scraped data is saved into the log file along with a timestamp, every value is on the new line.
     * If a log file path is not found, a file will be created on this path.
     */
    public void automatizeSelectors(String path, int days, int hours, int minutes, int seconds, int ms) {
        long miliseconds = calculateMiliseconds(days, hours, minutes, seconds, ms);
        Path logFile = getPath(path);
        createPath(logFile);
        Runnable r = createRunnableForSelectors(logFile);
        automatize(miliseconds, r);
    }

    /**
     * A method for periodical full HTML document scraping. An HTML snapshot will be created after given period of time.
     * Method accepts a folder path as an argument, and if no folder is found on this path, it will be automatically created.
     * The method creates a new file for every snapshot. The name of the file is the timestamp.html
     * This method does not download external CSS. It is recommended to put a webpage url as a target folder, for a better classification. .
     */
    public void htmlSnapshots(String targetFolder, int days, int hours, int minutes, int seconds, int ms) {
        long miliseconds = calculateMiliseconds(days, hours, minutes, seconds, ms);
        Path target = getPath(targetFolder);
        createPath(target);
        Runnable r = createRunnableForSnapshots(target);
        automatize(miliseconds, r);
    }

    /**
     * A method that creates Runnable for HTML snapshotting. This runnable is then triggered every given period of time,
     * by a ScheduledExecutorService.
     */
    private Runnable createRunnableForSnapshots(Path targetFolder) {
        return () -> writeHtml(targetFolder);
    }

    /**
     * A method for creating a file in the specified folder and writing out the Html-snapshot to this file.
     */
    private void writeHtml(Path targetFolder) {
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
    }


    /**
     * A method for invoking a runnable every given, specified amount of time.
     */
    private void automatize(long miliseconds, Runnable r) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(r, 0, miliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * A method that creates Runnable for scraping-by-selectors. This runnable is then triggered every given period of time,
     * by a ScheduledExecutorService.
     */
    private Runnable createRunnableForSelectors(Path logFile) {
        return () -> writeSelectors(logFile);
        
    }

    /**
     * A method for writing out the get-by-selectors result to a specified file. At the end, the selector result list is cleared,
     * in order to scrape the elements again.
     */
    private void writeSelectors(Path logFile) {
        Timestamp timeStamp = getTimesStamp();
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile.toString(), true));
            bufferedWriter.write(timeStamp + "");
            bufferedWriter.newLine();

            List<String> values = getSelectorsResult();
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
    }

    /**
     * A method that is used for a path validation, optionally creation of the file/folder on a specified path.
     */
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

    private Timestamp getTimesStamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    private long calculateMiliseconds(int days, int hours, int minutes, int seconds, long ms) {
        return days * 86400000L + hours * 3600000L + minutes * 60000L + seconds * 1000L + ms;
    }
}
