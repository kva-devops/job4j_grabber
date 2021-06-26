package ru.job4j.grabber.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

public class SqlRuDateTimeParser implements DateTimeParser {

    Map<String, String> calendarMap = Map.ofEntries(
            entry("01", "янв"),
            entry("02", "фев"),
            entry("03", "мар"),
            entry("04", "апр"),
            entry("05", "май"),
            entry("06", "июн"),
            entry("07", "июл"),
            entry("08", "авг"),
            entry("09", "сен"),
            entry("10", "окт"),
            entry("11", "ноя"),
            entry("12", "дек")
    );

    public SqlRuDateTimeParser() {
    }

    @Override
    public LocalDateTime parse(String parse) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String[] buffDateTime = parse.split(" ");
        if ("вчера,".equals(buffDateTime[0])) {
            LocalDateTime dateTime = LocalDateTime.now().minusDays(1);
            DateTimeFormatter formatterForYesterday = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String[] buffChangeTime = dateTime.format(formatterForYesterday).split(" ");
            String rsl = buffChangeTime[0] + " " + buffDateTime[1];
            return LocalDateTime.parse(rsl, formatter);
        } else if("сегодня,".equals(buffDateTime[0])) {
            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter formatterForToday = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String[] buffChangeTime = dateTime.format(formatterForToday).split(" ");
            String rsl = buffChangeTime[0] + " " + buffDateTime[1];
            return LocalDateTime.parse(rsl, formatter);
        } else {
            Set<Map.Entry<String, String>> calSet = calendarMap.entrySet();
            for (Map.Entry<String, String> elem : calSet) {
                if (buffDateTime[1].equals(elem.getValue())) {
                    buffDateTime[1] = elem.getKey();
                }
            }
            if (buffDateTime[0].length() == 1) {
                String buff = buffDateTime[0];
                buffDateTime[0] = "0" + buff;
            }
            String rsl = "20" + buffDateTime[2].substring(0, buffDateTime[2].length() - 1)
                    + "-" + buffDateTime[1]
                    + "-" + buffDateTime[0]
                    + " "
                    + buffDateTime[3];
            return LocalDateTime.parse(rsl, formatter);
        }
    }
}