package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.ControllerDatabase;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class MetricDatabaseReaderTask implements Runnable {
    private static final Logger logger = LogManager.getFormatterLogger();
    private String dataType;
    private Long startTimestamp, endTimestamp;
    private ControllerDatabase controllerDatabase;
    private Configuration configuration;
    private LinkedBlockingQueue<MetricValueCollection> dataToInsertLinkedBlockingQueue;

    public MetricDatabaseReaderTask(String dataType, ControllerDatabase controllerDatabase, Long startTimestamp, Long endTimestamp, Configuration configuration, LinkedBlockingQueue<MetricValueCollection> dataToInsertLinkedBlockingQueue) {
        this(dataType,controllerDatabase,configuration,dataToInsertLinkedBlockingQueue);
        this.startTimestamp=startTimestamp;
        this.endTimestamp=endTimestamp;
    }

    public MetricDatabaseReaderTask(String dataType, ControllerDatabase controllerDatabase, Configuration configuration, LinkedBlockingQueue<MetricValueCollection> dataToInsertLinkedBlockingQueue) {
        this.dataType=dataType;
        this.controllerDatabase=controllerDatabase;
        this.startTimestamp=null;
        this.endTimestamp=null;
        this.configuration=configuration;
        this.dataToInsertLinkedBlockingQueue=dataToInsertLinkedBlockingQueue;
    }

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            MetricValueCollection metricValueCollection = null;
            long startTime = System.currentTimeMillis();
            if( startTimestamp == null ) {
                metricValueCollection = controllerDatabase.getAllMetrics(dataType, configuration.getMigrationLevel(), configuration.getDaysToRetrieveData());
            } else {
                metricValueCollection = controllerDatabase.getAllMetrics(dataType, configuration.getMigrationLevel(), startTimestamp, endTimestamp);
            }
            long runTime = System.currentTimeMillis() - startTime;
            logger.info("Fetched %d %s metrics from %s database for processing in %d milliseconds", metricValueCollection.getMetrics().size(), dataType, controllerDatabase.toString(), runTime);
            dataToInsertLinkedBlockingQueue.add( metricValueCollection );
        } catch (InvalidConfigurationException e) {
            logger.warn("Could not get metrics for controller %s, because: %s", controllerDatabase.toString(), e.toString());
        }

    }
}
