package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class SqlRuParse {
    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.connect("http://www.sql.ru/forum/job-offers").get();
        Element table = doc.getElementsByTag("tbody").get(2);
        Elements ourTable = table.children();
        int i = 0;
        for (Element elem : ourTable) {
            if (i > 3) {
                Elements parseLink = elem.select(".postslisttopic");
                for (Element td : parseLink) {
                    Element href = td.child(0);
                    System.out.println(href.attr("href"));
                    System.out.println(href.text());
                }
                System.out.println(elem.children().get(5).select(".altCol").text());
            } else {
                i++;
            }
        }
    }
}
