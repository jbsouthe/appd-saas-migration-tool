package com.cisco.josouthe.util;

import junit.framework.TestCase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest extends TestCase {
    private static final Logger logger = LogManager.getFormatterLogger();

    public ParserTest() {}

    @Before
    public void setUp() {
        Configurator.setAllLevels("", Level.ALL);
    }

    @Test
    public void testEncode() {
        String input="Business Transaction Performance|Business Transactions|ProxyTier|Some Crazy Transaction|Average Response Time (ms)";
        String goodOutput = "Business+Transaction+Performance%7CBusiness+Transactions%7CProxyTier%7CSome+Crazy+Transaction%7CAverage+Response+Time+%28ms%29";
        assert goodOutput.equals(Parser.encode(input));
    }

    @Test
    public void testParseBTFromMetricName() {
        System.out.println(Parser.parseBTFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)"));
        assert Parser.parseBTFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)") == 4487611l;
    }

    @Test
    public void testParseComponentFromMetricName() {
        System.out.println(Parser.parseComponentFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)"));
        assert Parser.parseComponentFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)") == 3019102l;
    }

    @Test
    public void testParseSEFromMetricName() {
        System.out.println(Parser.parseSEFromMetricName("BTM|BTs|SE:4487611|Component:3019102|Average Response Time (ms)"));
        assert Parser.parseSEFromMetricName("BTM|BTs|SE:4487611|Component:3019102|Average Response Time (ms)") == 4487611l;
    }
}