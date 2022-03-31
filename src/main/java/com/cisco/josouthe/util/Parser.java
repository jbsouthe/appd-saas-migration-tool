package com.cisco.josouthe.util;

import com.appdynamics.apm.appagent.api.DataScope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static final Logger logger = LogManager.getFormatterLogger();
    private static Pattern patternNodeNameInMetricPath = Pattern.compile(".*\\|Individual\\sNodes\\|(?<nodeName>[^\\|]+)\\|.*");
    private static Pattern patternBTIdInMetricName = Pattern.compile(".*\\|BT:(?<btId>\\d+)\\|.*");
    private static Pattern patternComponentIdInMetricName = Pattern.compile(".*\\|Component:(?<componentId>\\d+)\\|.*");
    private static Pattern patternSEIdInMetricName = Pattern.compile(".*\\|SE:(?<seId>\\d+)\\|.*");


    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static Set<DataScope> snapshotDatascope;

    public static String encode( String original ){
        /* this, of course, got rediculous, replacing with a utility external to code base */
        try {
            return URLEncoder.encode(original, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding Exception: %s", e.getMessage());
        }
        //falling back to the manual, because it is already written..
        return original.replace("%", "%25")
                .replace("|","%7C")
                .replace(" ", "%20")
                .replace(":","%3A")
                .replace(".", "%2E")
                .replace("-", "%2D")
                .replace("#", "%23")
                .replace("\"", "%22")
                .replace("/", "%2F")
                .replace("(", "%28")
                .replace(")", "%29")
                .replace("<", "%3C")
                .replace(">", "%3E")
                ;
    }

    public static Long parseBTFromMetricName( String name ) {
        Matcher matcher = patternBTIdInMetricName.matcher(name);
        if( matcher.matches() ) {
            return Long.parseLong(matcher.group("btId"));
        }
        return null;
    }

    public static Long parseComponentFromMetricName( String name ) {
        Matcher matcher = patternComponentIdInMetricName.matcher(name);
        if( matcher.matches() ) {
            return Long.parseLong(matcher.group("componentId"));
        }
        return null;
    }

    public static Long parseSEFromMetricName( String name ) {
        Matcher matcher = patternSEIdInMetricName.matcher(name);
        if( matcher.matches() ) {
            return Long.parseLong(matcher.group("seId"));
        }
        return null;
    }

}
