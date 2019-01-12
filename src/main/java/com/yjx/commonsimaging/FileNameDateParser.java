package com.yjx.commonsimaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-12 15:17
 */
@Slf4j
public class FileNameDateParser {
    public final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Pattern pattern;
    private boolean match;
    private String text;
    private Matcher matcher;

    public FileNameDateParser(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public boolean isMatch(String text) {
        if (StringUtils.equals(text, this.text)) {
            return this.match;
        }
        this.text = text;
        this.matcher = pattern.matcher(this.text);
        this.match = matcher.matches();
        return this.match;
    }

    public String getDate() {
        if (!match) {
            log.warn("没有发现日期 pattern {} text {}", pattern.pattern(), text);
            return null;
        }
        String time = matchGroup(matcher, "time");
        String date = matchGroup(matcher, "date");
        String dateTime = date + time;
        String formatDate = null;
        if (dateTime.length() == 13) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(new Date(Long.valueOf(dateTime)).toInstant(), ZoneId.systemDefault());
            formatDate = localDateTime.format(FORMATTER);
        } else if (dateTime.length() == 14) {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            formatDate = localDateTime.format(FORMATTER);
        } else {
            log.error("未识别的日期 {}", text);
        }
        return formatDate;
    }

    private String matchGroup(Matcher matcher, String type) {
        String group = "";
        try {
            group = matcher.group(type);
        } catch (Exception e) {
            //nothing
        }
        return group;
    }
}
