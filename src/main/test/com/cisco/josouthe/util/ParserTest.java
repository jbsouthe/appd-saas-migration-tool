package com.cisco.josouthe.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    private static final Logger logger = LogManager.getFormatterLogger();

    @BeforeEach
    public void setUp() {
        Configurator.setAllLevels("", Level.ALL);
    }

    @Test
    void encode() {
        String input="Business Transaction Performance|Business Transactions|ProxyTier|Some Crazy Transaction|Average Response Time (ms)";
        String goodOutput = "Business+Transaction+Performance%7CBusiness+Transactions%7CProxyTier%7CSome+Crazy+Transaction%7CAverage+Response+Time+%28ms%29";
        assert goodOutput.equals(Parser.encode(input));
    }

    @Test
    void parseBTFromMetricName() {
        System.out.println(Parser.parseBTFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)"));
        assert Parser.parseBTFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)") == 4487611l;
    }

    @Test
    void parseComponentFromMetricName() {
        System.out.println(Parser.parseComponentFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)"));
        assert Parser.parseComponentFromMetricName("BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)") == 3019102l;
    }

    @Test
    void parseSEFromMetricName() {
        System.out.println(Parser.parseSEFromMetricName("BTM|BTs|SE:4487611|Component:3019102|Average Response Time (ms)"));
        assert Parser.parseSEFromMetricName("BTM|BTs|SE:4487611|Component:3019102|Average Response Time (ms)") == 4487611l;
    }
}