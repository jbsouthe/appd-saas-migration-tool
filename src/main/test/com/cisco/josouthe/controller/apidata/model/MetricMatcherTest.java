package com.cisco.josouthe.controller.apidata.model;

import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetricMatcherTest extends TestCase {
    private static final Logger logger = LogManager.getFormatterLogger();

    public MetricMatcherTest() {}

    @Before
    public void setUp() {
        Configurator.setAllLevels("", Level.ALL);
    }

    @Test
    public void testMatchesBTMetric() throws Exception {
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
        wrongBTDatabaseMetricDefinition.metricName="BTM|BTs|BT:1000|Component:3019102|Average Response Time (ms)";

        MetricMatcher metricMatcher = new MetricMatcher(btControllerMetricData);
        assert metricMatcher.matches(null) == false;
        assert metricMatcher.matches(matchingDatabaseMetricDefinition) == true;
        assert metricMatcher.matches(wrongBTDatabaseMetricDefinition) == false;
    }
}