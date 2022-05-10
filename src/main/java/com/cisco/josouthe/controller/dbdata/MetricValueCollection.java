package com.cisco.josouthe.controller.dbdata;

import com.cisco.josouthe.controller.apidata.model.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MetricValueCollection {
    private static final Logger logger = LogManager.getFormatterLogger();
    private SourceModel sourceModel;
    private List<DatabaseMetricValue> metricValueList;

    public MetricValueCollection( SourceModel model, List<DatabaseMetricValue> metrics ) {
        this.sourceModel = model;
        this.metricValueList = new ArrayList<>();
        this.metricValueList.addAll(metrics);
    }

    public List<DatabaseMetricValue> getMetrics() { return metricValueList; }
    public DatabaseMetricDefinition getMetricDefinition( DatabaseMetricValue databaseMetricValue ) {
        if( databaseMetricValue == null ) {
            logger.error("database metric value is null");
        }
        if( databaseMetricValue.application_id == null ) {
            logger.error("database metric value application id is null");
        }
        Application application = sourceModel.getApplication(databaseMetricValue.application_id);
        if( application == null ) {
            logger.error("Application not found in local database %s", databaseMetricValue.application_id);
        }
        return application.getMetricDefinition(databaseMetricValue.metric_id);
    }
    public SourceModel getSourceModel() { return sourceModel; }
}
