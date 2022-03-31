package com.cisco.josouthe.controller.apidata.model;

import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricMatcherTest {
    private static final Logger logger = LogManager.getFormatterLogger();

    @Before
    public void setUp() {
        Configurator.setAllLevels("", Level.ALL);
    }

    @Test
    void matchesBTMetricTest() {
        MetricData btControllerMetricData = new MetricData();
        btControllerMetricData.metricName="BTM|BTs|BT:4487611|Component:3019102|Average Response Time (ms)";
        btControllerMetricData.metricPath="Business Transaction Performance|Business Transactions|ProxyTier|Some Crazy Transaction|Average Response Time (ms)";
        btControllerMetricData.metricId=144733715;

        DatabaseMetricDefinition matchingDatabaseMetricDefinition = new DatabaseMetricDefinition();
        matchingDatabaseMetricDefinition.tierId=3019102;
        matchingDatabaseMetricDefinition.btId=4487611;
        matchingDatabaseMetricDefinition.metricName=btControllerMetricData.metricName;

        DatabaseMetricDefinition wrongBTDatabaseMetricDefinition = new DatabaseMetricDefinition();
        wrongBTDatabaseMetricDefinition.tierId=3019102;
        wrongBTDatabaseMetricDefinition.btId=1000;
        wrongBTDatabaseMetricDefinition.metricName=btControllerMetricData.metricName;

        MetricMatcher metricMatcher = new MetricMatcher(btControllerMetricData);
        assert metricMatcher.matches(null) == false;
        assert metricMatcher.matches(matchingDatabaseMetricDefinition) == true;
        assert metricMatcher.matches(wrongBTDatabaseMetricDefinition) == false;
    }
}