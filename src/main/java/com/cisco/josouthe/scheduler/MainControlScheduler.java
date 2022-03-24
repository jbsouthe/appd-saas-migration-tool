package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.ControllerDatabase;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.util.TimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.*;

public class MainControlScheduler {
    private static final Logger logger = LogManager.getFormatterLogger();
    Configuration configuration;
    private LinkedBlockingQueue<MetricValueCollection> dataToInsertLinkedBlockingQueue;
    private ThreadPoolExecutor executorFetchData;
    private ThreadPoolExecutor executorInsertData;
    Collection<Future<?>> futures = new LinkedList<Future<?>>();

    public MainControlScheduler(Configuration config ) {
        this.configuration = config;
        dataToInsertLinkedBlockingQueue = new LinkedBlockingQueue<>();
        executorFetchData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfDatabaseThreads", 15), new NamedThreadFactory("Database") );
        executorInsertData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfWriterThreads", 3), new NamedThreadFactory("CSVWriter") );
    }

    public void run() {
        this.configuration.setRunning(true);
        for( int i=0; i < this.configuration.getProperty("scheduler-NumberOfWriterThreads", 3); i++) {
            executorInsertData.execute( new CSVWriterTask(configuration, dataToInsertLinkedBlockingQueue));
        }
        logger.info("Started %d CSV File Writer Tasks, all looking for work", executorInsertData.getPoolSize());
        for( ControllerDatabase controllerDatabase : configuration.getControllerList() ) {
            switch (configuration.getMigrationLevel()) { //1 = App, 2 = 1+Tiers+nodes, 3= 2+BT+ALL
                case 3: { //TODO implement the deeper methods
                }
                case 2: {
                    long startTimestamp= TimeUtil.now();
                    long endTimestamp = startTimestamp - (48*60*60*1000); //2 days at a time
                    while( startTimestamp > TimeUtil.getDaysBackTimestamp(configuration.getDaysToRetrieveData()) ) {
                        futures.add(executorFetchData.submit(new MetricDatabaseReaderTask("node", controllerDatabase, startTimestamp, endTimestamp, configuration, dataToInsertLinkedBlockingQueue)));
                        futures.add(executorFetchData.submit(new MetricDatabaseReaderTask("tier", controllerDatabase, startTimestamp, endTimestamp, configuration, dataToInsertLinkedBlockingQueue)));
                        startTimestamp = endTimestamp;
                        endTimestamp -= (48*60*60*1000); //2 days at a time
                    }
                }
                case 1: {
                    futures.add(executorFetchData.submit(new MetricDatabaseReaderTask( "app", controllerDatabase, configuration, dataToInsertLinkedBlockingQueue)));
                }

            }
        }
        for (Future<?> future:futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        while(!dataToInsertLinkedBlockingQueue.isEmpty()) {
            sleep(5000);
        }
        configuration.setRunning(false);
        sleep(10000); //so database workers can finish up
        executorInsertData.shutdown();
        executorFetchData.shutdown();
        /*
        while(configuration.isRunning() ) {
            for( Controller controller : configuration.getControllerList() ) {
                for(Application application : controller.applications ) {
                    logger.info("Running collector for %s@%s", application.getName(), controller.hostname);
                    executorFetchData.execute(new ApplicationMetricTask( application, dataToInsertLinkedBlockingQueue));
                    executorFetchData.execute( new ApplicationEventTask( application, dataToInsertLinkedBlockingQueue));
                }
            }

            for(Analytics analytic : configuration.getAnalyticsList() ) {
                executorFetchData.execute( new AnalyticsSearchTask( analytic, dataToInsertLinkedBlockingQueue) );
            }
            if( configuration.getProperty("scheduler-enabled", true) ) {
                logger.info("MainControlScheduler is enabled, so sleeping for %d minutes and running again", configuration.getProperty("scheduler-pollIntervalMinutes", 60L));
                sleep( configuration.getProperty("scheduler-pollIntervalMinutes", 60L) * 60000 );
                logger.info("MainControlScheduler is awakened and running once more");
            } else {
                sleep(5000);
                for( Controller controller : configuration.getControllerList() ) {
                   logger.debug("Waiting for Controller %s to finish initializing all %d applications",controller.hostname, controller.applications.length);
                   for (Application application : controller.applications) {
                        logger.debug("Waiting for Application %s to finish initializing",application.getName());
                        while( ! application.isFinishedInitialization() ) sleep(10000 );
                        logger.debug("Application %s finished initializing",application.getName());
                   }
                   logger.debug("Controller %s finished initializing",controller.hostname);
                }
                executorConfigRefresh.shutdownNow();
                sleep(5000);
                logger.info("MainControlScheduler is disabled, so exiting when database queue is drained");
                while(!dataToInsertLinkedBlockingQueue.isEmpty()) {
                    sleep(5000);
                }
                configuration.setRunning(false);
                sleep(10000); //so database workers can finish up
                executorInsertData.shutdown();
                executorFetchData.shutdown();
            }
        }

         */

    }

    private void sleep( long forMilliseconds ) {
        try {
            Thread.sleep( forMilliseconds );
        } catch (InterruptedException ignored) { }
    }
}
