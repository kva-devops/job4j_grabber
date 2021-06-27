package ru.job4j.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.Grab;
import ru.job4j.grabber.Parse;
import ru.job4j.grabber.Post;
import ru.job4j.grabber.Store;
import ru.job4j.grabber.utils.SqlRuDateTimeParser;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class SqlRuParse implements Parse, Store, Grab {
    public static int id = 0;
    public String title = "";
    public SqlRuDateTimeParser parserDateAndTime;
    public String publicTime = "";
    private Connection cn;

    public SqlRuParse(SqlRuDateTimeParser parserDateAndTime) {
        this.parserDateAndTime = parserDateAndTime;
    }

    public static void main(String[] args) throws SchedulerException {
        SqlRuParse object = new SqlRuParse(new SqlRuDateTimeParser());
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        object.init(object, object, scheduler);
        object.initConnection();
        System.out.println(object.getAll());
        System.out.println(object.findById(10));

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
                publicTime = elem.children().get(5).select(".altCol").text();
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
        result.setCreated(parserDateAndTime.parse(publicTime));
        return result;
    }

    public Connection initConnection() {
        try (InputStream in = SqlRuParse.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            Properties config = new Properties();
            config.load(in);
            Class.forName(config.getProperty("rabbit.driver"));
            cn = DriverManager.getConnection(
                    config.getProperty("rabbit.url"),
                    config.getProperty("rabbit.username"),
                    config.getProperty("rabbit.password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return cn;
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement = cn.prepareStatement(
                "insert into post (name, text, link, created) values (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setObject(4, post.getCreated());
            statement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> result = new ArrayList<>();
        try (PreparedStatement statement = cn.prepareStatement(
                "select * from post"
        )) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new Post(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("link"),
                            resultSet.getString("text"),
                            resultSet.getTimestamp("created").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Post findById(int id) {
        Post result = new Post();
        try (PreparedStatement statement = cn.prepareStatement(
                "select * from post where id = ?"
        )) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.setId(resultSet.getInt("id"));
                    result.setTitle(resultSet.getString("name"));
                    result.setLink(resultSet.getString("link"));
                    result.setDescription(resultSet.getString("text"));
                    result.setCreated(resultSet.getTimestamp("created").toLocalDateTime());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        if (cn != null) {
            cn.close();
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        try (Connection connection = initConnection()) {
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connect", connection);
            data.put("object", parse);
            JobDetail job = newJob(Grabbing.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInMinutes(20)
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Grabbing implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Connection connection = (Connection) context
                    .getJobDetail()
                    .getJobDataMap()
                    .get("connect");
            SqlRuParse object = (SqlRuParse) context
                    .getJobDetail()
                    .getJobDataMap()
                    .get("object");
            try (Statement statement = connection.createStatement()) {
                String sql = "drop table if exists post;" + System.lineSeparator()
                        + "create table if not exists post (" + System.lineSeparator()
                        + "id serial primary key," + System.lineSeparator()
                        + "name varchar(255)," + System.lineSeparator()
                        + "text text," + System.lineSeparator()
                        + "link varchar(255) unique," + System.lineSeparator()
                        + "created timestamp" + System.lineSeparator()
                        + ");";
                statement.execute(sql);
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<Post> rsl = new ArrayList<>();
            try {
                rsl = object.list("http://www.sql.ru/forum/job-offers/");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            for (Post post : rsl) {
                object.save(post);
            }
        }
    }
}
