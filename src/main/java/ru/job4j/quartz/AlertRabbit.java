package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.sql.*;
import java.util.Properties;
import java.sql.Date;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {
    public static void main(String[] args) {
        try {
            try (Connection connection = readProperties("/rabbit.properties")) {
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                JobDataMap data = new JobDataMap();
                data.put("connect", connection);
                JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
                SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(10)
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
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    public static Connection readProperties(String fileProperties) {
        try {
            Properties rabbitProperties = new Properties();
            rabbitProperties.load(AlertRabbit.class.getResourceAsStream(fileProperties));
            Class.forName(rabbitProperties.getProperty("rabbit.driver"));
            return DriverManager.getConnection(
                    rabbitProperties.getProperty("rabbit.url"),
                    rabbitProperties.getProperty("rabbit.username"),
                    rabbitProperties.getProperty("rabbit.password"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Connection connection = (Connection) context
                    .getJobDetail()
                    .getJobDataMap()
                    .get("connect");
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into rabbit (created_date) values (?)"
            )) {
                statement.setDate(1, new Date(System.currentTimeMillis()));
                statement.execute();
                System.out.println("write into database completed");
            } catch (Exception e) {

            }
        }
    }
}