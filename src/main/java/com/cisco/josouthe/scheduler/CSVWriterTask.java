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
        while( configuration.isRunning() ) {
            try {
                MetricValueCollection metricValueCollection = dataQueue.poll(5000, TimeUnit.MILLISECONDS);
                if( metricValueCollection != null ) {
                    logger.info("Poll returned %d data elements to write", metricValueCollection.getMetrics().size());
                    configuration.getCSVMetricWriter().writeMetricsToFile(metricValueCollection);
                }
            } catch (InterruptedException ignored) {
                //ignore it
            }
        }
        logger.info("Shutting down database Insert Task");
        try {
            configuration.getCSVMetricWriter().close();
        } catch (IOException e) {
            logger.warn("Error closing csv output files: %s",e.toString());
        }
    }
}
