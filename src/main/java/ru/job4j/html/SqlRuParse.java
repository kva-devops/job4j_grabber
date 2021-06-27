package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ru.job4j.grabber.Parse;
import ru.job4j.grabber.Post;
import ru.job4j.grabber.utils.SqlRuDateTimeParser;

public class SqlRuParse implements Parse {
    public static int id = 0;
    public String title = "";
    public LocalDateTime createdTime = null;

    public static void main(String[] args) throws IOException {
        SqlRuParse object = new SqlRuParse();
        List<Post> rsl = object.list("http://www.sql.ru/forum/job-offers/");
        System.out.println(rsl.toString());
    }

    public static String printDescription(String urlDescLink) throws IOException {
        Document docInner = Jsoup.connect(urlDescLink).get();
        Element tableInner = docInner.getElementsByTag("tbody").get(1);
        Elements outTableInner = tableInner.children();
        if (outTableInner.size() == 1) {
            tableInner = docInner.getElementsByTag("tbody").get(2);
            outTableInner = tableInner.children();
        }
        return outTableInner.get(1).select(".msgBody").get(1).text();
    }

    @Override
    public List<Post> list(String link) throws IOException {
        List<Post> result = new ArrayList<>();
        String urlDescription = "";
        int pageLimit = 1;
        for (int i = 1; i <= pageLimit; i++) {
            String urlMain = link + i;
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
                    title = href.text();
                }
                String publicTimeString = elem.children().get(5).select(".altCol").text();
                createdTime = new SqlRuDateTimeParser().parse(publicTimeString);
                Post element = detail(urlDescription);
                result.add(element);
            }
        }
        return result;
    }

    @Override
    public Post detail(String link) throws IOException {
        Post result = new Post();
        result.setId(id++);
        result.setTitle(title);
        result.setLink(link);
        result.setDescription(printDescription(link));
        result.setCrated(createdTime);
        return result;
    }
}
