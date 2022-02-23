package com.cisco.josouthe.controller.apidata.metric;

import com.cisco.josouthe.controller.Controller;
import com.cisco.josouthe.controller.apidata.model.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MetricData {
    private static final Logger logger = LogManager.getFormatterLogger();
    public MetricData() {} //for GSON.fromJSON


    public long metricId;
    public String metricName, metricPath, frequency, hostname;
    public List<MetricValue> metricValues;
    public Controller controller;
    public Application application;

    public class MetricValue {
        public long startTimeInMillis, occurrences, current, min, max, count, sum, value;
        public boolean useRange;
        public double standardDeviation;
    }

}
