package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import ru.job4j.grabber.utils.SqlRuDateTimeParser;

public class SqlRuParse {
    public static void main(String[] args) throws IOException {
        String urlTemplate = "http://www.sql.ru/forum/job-offers/";
        String urlDescription = "";
        int pageLimit = 5;
        for (int i = 1; i <= pageLimit; i++) {
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
                    urlDescription = href.attr("href");
                    System.out.println(urlDescription);
                    System.out.println(href.text());
                }
                String publicTimeString = elem.children().get(5).select(".altCol").text();
                System.out.println(new SqlRuDateTimeParser().parse(publicTimeString));
                printDescription(urlDescription);
            }

        }
    }

    public static void printDescription(String urlDescLink) throws IOException {
        Document docInner = Jsoup.connect(urlDescLink).get();
        Element tableInner = docInner.getElementsByTag("tbody").get(1);
        Elements outTableInner = tableInner.children();
        if (outTableInner.size() == 1) {
            tableInner = docInner.getElementsByTag("tbody").get(2);
            outTableInner = tableInner.children();
        }
        System.out.println(outTableInner.get(1).select(".msgBody").get(1).text());
        String[] buffDesc = outTableInner.get(2).select(".msgFooter").get(0).text().split(", ");
        String rslTime = buffDesc[0] + ", ";
        Optional<String> timeOpt = Arrays.stream(buffDesc[1].split(" ")).findFirst();
        if (timeOpt.isPresent()) {
            rslTime += timeOpt.get();
        }
        System.out.println(new SqlRuDateTimeParser().parse(rslTime));
    }
}
