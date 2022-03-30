package com.cisco.josouthe.controller.apidata.model;

import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import com.cisco.josouthe.util.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MetricMatcher {
    private static final Logger logger = LogManager.getFormatterLogger();

    public long id;
    public String metricName, metricPath;
    private Long tierId, nodeId, btId, seId;

    public MetricMatcher(MetricData metricData) {
        this.id = metricData.metricId;
        this.metricName = metricData.metricName;
        this.metricPath = metricData.metricPath;
        tierId = Parser.parseComponentFromMetricName(metricName);
        btId = Parser.parseBTFromMetricName(metricName);
        seId = Parser.parseSEFromMetricName(metricName);
    }

    //this is to be used to match a metric AFTER it has been converted for this controller
    public boolean matches( DatabaseMetricDefinition databaseMetricDefinition ) {
        if(databaseMetricDefinition==null) {
            logger.debug("input metric to match is null");
            return false;
        }
        if( !metricName.equals(databaseMetricDefinition.metricName) ) {
            logger.debug("input metric name '%s' != '%s'",databaseMetricDefinition.metricName, metricName);
            return false;
        }
        logger.debug("metric matches so far: %s(%d) path: %s", metricName,id,metricPath);
        if( tierId != null && !tierId.equals(databaseMetricDefinition.tierId)) {
            logger.debug("Tier id found in matcher %d, but not a match in target %d",tierId, databaseMetricDefinition.tierId);
            return false;
        }
        if( btId != null && !btId.equals(databaseMetricDefinition.btId)) {
            logger.debug("BT id found in matcher %d, but not a match in target %d",btId, databaseMetricDefinition.btId);
            return false;
        }
        if( seId != null && !seId.equals(databaseMetricDefinition.seId)) {
            logger.debug("SE id found in matcher %d, but not a match in target %d",seId, databaseMetricDefinition.seId);
            return false;
        }
        logger.debug("'%s' this must be equal to '%s'(%d) path: '%s'",databaseMetricDefinition, metricName,id,metricPath);
        return true;
    }
}
