
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

       HtmlScraper htmlScraper = new HtmlScraper("https://jsoup.org/cookbook/extracting-data/selector-syntax").addSelector("div","class","wrap").addSelector("div","class","header");
        htmlScraper.getSelectorsResult().forEach(System.out::println);


        Connection c = Jsoup.connect("https://jsoup.org/cookbook/extracting-data/selector-syntax");
        Document d = c.get();

        System.out.println(d.select("div[class=breadcrumb]").size());


    }
}

