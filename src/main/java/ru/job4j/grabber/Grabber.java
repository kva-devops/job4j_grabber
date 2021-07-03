package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.SqlRuDateTimeParser;
import ru.job4j.html.SqlRuParse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {

    public static int pageLimit;

    private final Properties cfg = new Properties();

    public Store store() throws SQLException {
        return new PsqlStore(cfg);
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public void cfg() throws IOException {
        try (InputStream in = SqlRuParse.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            cfg.load(in);
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        data.put("url", cfg.getProperty("rabbit.url.for.parsing"));
        data.put("limit", cfg.getProperty("rabbit.page.limit"));
        JobDetail job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(cfg.getProperty("rabbit.interval")))
                .repeatForever();
        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            String urlForParsing = (String) map.get("url");
            pageLimit = (Integer) map.get("limit");
            try {
                for (Post post : parse.list(urlForParsing)) {
                    store.save(post);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void web(Store store) {

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(
                    cfg.getProperty("rabbit.port")
            ))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        out.write("<html>".getBytes());
                        out.write("<body>".getBytes());
                        for (Post post : store.getAll()) {
                            out.write(post.getTitle().getBytes(Charset.forName("Windows-1251")));
                            out.write(post.getLink().getBytes(Charset.forName("Windows-1251")));
                            out.write(post.getCreated().toString().getBytes(Charset.forName("Windows-1251")));
                            out.write(post.getTitle().getBytes(Charset.forName("Windows-1251")));
                            out.write(post.getDescription().getBytes(Charset.forName("Windows-1251")));
                            out.write("<hr>".getBytes());
                        }
                        out.write("</body>".getBytes());
                        out.write("</html>".getBytes());
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public static void main(String[] args) throws Exception {
        Grabber grab = new Grabber();
        grab.cfg();
        Scheduler scheduler = grab.scheduler();
        Store store = grab.store();
        DateTimeParser dateTimeParser = new SqlRuDateTimeParser();
        grab.init(new SqlRuParse(dateTimeParser, pageLimit), store, scheduler);
        grab.web(store);
    }
}