package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ru.job4j.grabber.utils.SqlRuDateTimeParser;

public class SqlRuParse {
    public static void main(String[] args) throws IOException {
        String urlTemplate = "http://www.sql.ru/forum/job-offers/";
        int pageLimit = 5;
        for (int i = 1; i < pageLimit; i++) {
            System.out.printf("===============Page %d===============", i);
            System.out.println();
            String urlMain = urlTemplate + i;
            Document doc = Jsoup.connect(urlMain).get();
            Element table = doc.getElementsByTag("tbody").get(2);
            Elements ourTable = table.children();
            List<Element> afterSkip = new ArrayList<>();
            ourTable.stream().skip(4).forEach(afterSkip::add);
            for (Element elem : afterSkip) {
                Elements parseLink = elem.select(".postslisttopic");
                for (Element td : parseLink) {
                    Element href = td.child(0);
                    System.out.println(href.attr("href"));
                    System.out.println(href.text());
                }
                String publicTimeString = elem.children().get(5).select(".altCol").text();
                System.out.println(new SqlRuDateTimeParser().parse(publicTimeString));
            }
        }
    }
}
