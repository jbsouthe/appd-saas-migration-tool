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
        return sourceModel.getApplication(databaseMetricValue.application_id).getMetricDefinition(databaseMetricValue.metric_id);
    }
    public SourceModel getSourceModel() { return sourceModel; }
}
