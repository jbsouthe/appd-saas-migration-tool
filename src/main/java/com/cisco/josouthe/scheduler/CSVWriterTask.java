package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CSVWriterTask implements Runnable{
    private static final Logger logger = LogManager.getFormatterLogger();

    private Configuration configuration;
    private LinkedBlockingQueue<MetricValueCollection> dataQueue;

    public CSVWriterTask( Configuration configuration, LinkedBlockingQueue<MetricValueCollection> dataQueue ) {
        this.configuration=configuration;
        this.dataQueue=dataQueue;
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
        boolean processedOne=false;
        while( !processedOne ) {
            try {
                MetricValueCollection metricValueCollection = dataQueue.poll(5, TimeUnit.MINUTES);
                if( metricValueCollection != null ) {
                    processedOne=true;
                    logger.info("Poll returned %d data elements to write", metricValueCollection.getMetrics().size());
                    long startTime = System.currentTimeMillis();
                    configuration.getCSVMetricWriter().writeMetricsToFile(metricValueCollection);
                    long runTime = System.currentTimeMillis() - startTime;
                    logger.info("CSV Writer wrote %d metrics in %d milliseconds", metricValueCollection.getMetrics().size(), runTime);
                }
            } catch (InterruptedException ignored) {
                //ignore it
            }
        }
        logger.info("Shutting down CSV Writer Task");
    }
}
