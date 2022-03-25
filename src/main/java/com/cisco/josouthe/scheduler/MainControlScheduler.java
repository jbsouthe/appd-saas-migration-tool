package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.ControllerDatabase;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.util.TimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.*;

public class MainControlScheduler {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Configuration configuration;
    private LinkedBlockingQueue<MetricValueCollection> dataToConvertLinkedBlockingQueue;
    private LinkedBlockingQueue<MetricValueCollection> dataToInsertLinkedBlockingQueue;
    private ThreadPoolExecutor executorFetchData;
    private ThreadPoolExecutor executorConvertData;
    private ThreadPoolExecutor executorInsertData;
    private Collection<Future<?>> futures = new LinkedList<Future<?>>();
    private static long incrementMS = 24*60*60*1000; //1 day

    public MainControlScheduler(Configuration config ) {
        this.configuration = config;
        dataToInsertLinkedBlockingQueue = new LinkedBlockingQueue<>();
        dataToConvertLinkedBlockingQueue = new LinkedBlockingQueue<>();
        executorFetchData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfDatabaseThreads", 15), new NamedThreadFactory("Database") );
        executorConvertData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfConverterThreads", 30), new NamedThreadFactory("Converter") );
        executorInsertData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfWriterThreads", 5), new NamedThreadFactory("CSVWriter") );
    }

    public void run() {
        this.configuration.setRunning(true);
        for( ControllerDatabase controllerDatabase : configuration.getControllerList() ) {
            switch (configuration.getMigrationLevel()) { //1 = App, 2 = 1+Tiers+nodes, 3= 2+BT+ALL
                case 3: { //TODO implement the deeper methods
                }
                case 2: {
                    long startTimestamp= TimeUtil.now();
                    long endTimestamp = startTimestamp - incrementMS;
                    while( startTimestamp > TimeUtil.getDaysBackTimestamp(configuration.getDaysToRetrieveData()) ) {
                        futures.addAll( submitJob(new MetricDatabaseReaderTask("node", controllerDatabase, startTimestamp, endTimestamp, configuration, dataToConvertLinkedBlockingQueue)));
                        futures.addAll( submitJob(new MetricDatabaseReaderTask("tier", controllerDatabase, startTimestamp, endTimestamp, configuration, dataToConvertLinkedBlockingQueue)));
                        startTimestamp = endTimestamp;
                        endTimestamp -= incrementMS;
                    }
                }
                case 1: {
                    futures.addAll( submitJob(new MetricDatabaseReaderTask( "app", controllerDatabase, configuration, dataToConvertLinkedBlockingQueue)));
                }

            }
        }
        logger.info("Scheduled a total of %d jobs to run fetching data for export to CSV", futures.size());
        for (Future<?> future:futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        logger.info("All Data Fetch jobs scheduled have completed, now waiting for the Workers to finish processing");
        configuration.setRunning(false);
        executorFetchData.shutdown();
        try {
            while (!executorFetchData.awaitTermination(300, TimeUnit.SECONDS)) {
                logger.info("Still waiting for Database Reader Tasks to finish");
            }
        } catch (InterruptedException ignored) {}
        executorConvertData.shutdown();
        try {
            while (!executorConvertData.awaitTermination(300, TimeUnit.SECONDS)) {
                logger.info("Still waiting for Data Conversion Tasks to finish");
            }
        } catch (InterruptedException ignored) {}
        executorInsertData.shutdown();
        try {
            while (!executorInsertData.awaitTermination(300, TimeUnit.SECONDS)) {
                logger.info("Still waiting for CSV Writer Tasks to finish");
            }
        } catch (InterruptedException ignored) {}
        logger.info("Everything is written, shutting everything down");
        try {
            configuration.getCSVMetricWriter().close();
        } catch (IOException e) {
            logger.warn("Error closing csv output files: %s",e.toString());
        }
    }

    private Collection<? extends Future<?>> submitJob(MetricDatabaseReaderTask metricDatabaseReaderTask) {
        Collection<Future<?>> futures = new LinkedList<>();
        futures.add(executorFetchData.submit(metricDatabaseReaderTask));
        futures.add(executorConvertData.submit(new DataConverterTask(configuration, dataToConvertLinkedBlockingQueue, dataToInsertLinkedBlockingQueue)));
        futures.add(executorInsertData.submit(new CSVWriterTask(configuration,dataToInsertLinkedBlockingQueue)));
        return futures;
    }

    private void sleep( long forMilliseconds ) {
        try {
            Thread.sleep( forMilliseconds );
        } catch (InterruptedException ignored) { }
    }
}
