package ru.job4j.grabber.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.util.Map.entry;

public class SqlRuDateTimeParser implements DateTimeParser {

    Map<String, String> calendarMap = Map.ofEntries(
            entry("янв", "01"),
            entry("фев", "02"),
            entry("мар", "03"),
            entry("апр", "04"),
            entry("май", "05"),
            entry("июн", "06"),
            entry("июл", "07"),
            entry("авг", "08"),
            entry("сен", "09"),
            entry("окт", "10"),
            entry("ноя", "11"),
            entry("дек", "12")
    );

    public SqlRuDateTimeParser() {
    }

    @Override
    public LocalDateTime parse(String parse) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String[] buffDateTime = parse.split(" ");
        String rsl;
        LocalDateTime dateTime;
        if ("вчера,".equals(buffDateTime[0])) {
            dateTime = LocalDateTime.now().minusDays(1);
        } else if ("сегодня,".equals(buffDateTime[0])) {
            dateTime = LocalDateTime.now();
        } else {
            buffDateTime[1] = calendarMap.get(buffDateTime[1]);
            if (buffDateTime[0].length() == 1) {
                String buff = buffDateTime[0];
                buffDateTime[0] = "0" + buff;
            }
            rsl = "20" + buffDateTime[2].substring(0, buffDateTime[2].length() - 1)
                    + "-" + buffDateTime[1]
                    + "-" + buffDateTime[0]
                    + " "
                    + buffDateTime[3];
            return LocalDateTime.parse(rsl, formatter);
        }
        DateTimeFormatter formatterForYesterdayAndToday = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String[] buffChangeTime = dateTime.format(formatterForYesterdayAndToday).split(" ");
        rsl = buffChangeTime[0] + " " + buffDateTime[1];
        return LocalDateTime.parse(rsl, formatter);
    }
}