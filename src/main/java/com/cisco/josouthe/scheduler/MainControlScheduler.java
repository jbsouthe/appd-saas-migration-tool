package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.MetaData;
import com.cisco.josouthe.controller.ControllerDatabase;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.output.DetailsFile;
import com.cisco.josouthe.output.ZipFileMaker;
import com.cisco.josouthe.util.TimeUtil;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
    private long startTimestamp;

    public MainControlScheduler(Configuration config ) {
        this.startTimestamp = System.currentTimeMillis();
        this.configuration = config;
        dataToInsertLinkedBlockingQueue = new LinkedBlockingQueue<>();
        dataToConvertLinkedBlockingQueue = new LinkedBlockingQueue<>();
        executorFetchData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfDatabaseThreads", 15), new NamedThreadFactory("Database") );
        executorConvertData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfConverterThreads", 30), new NamedThreadFactory("Converter") );
        executorInsertData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfWriterThreads", 5), new NamedThreadFactory("CSVWriter") );
    }

    public void run() {
        this.configuration.setRunning(true);
        long startTS= TimeUtil.now();
        long endTS = startTS - incrementMS;
        long maxInitTasks = 0;
        long maxRunTasks = 0;
        long maxZipTasks = 2;

        switch (configuration.getMigrationLevel()) {
            case 3:
            case 2: {
                maxZipTasks += 2;
                long saveStartTS = startTS;
                while (startTS > TimeUtil.getDaysBackTimestamp(configuration.getDaysToRetrieveData())) {
                    //maxInitTasks++;
                    maxRunTasks += 6;
                    startTS = endTS;
                    endTS -= incrementMS;
                }
                startTS = saveStartTS;
            }
            case 1: {
                maxZipTasks++;
                maxInitTasks++;
                while (startTS > TimeUtil.getDaysBackTimestamp(configuration.getDaysToRetrieveData())) {
                    maxRunTasks += 3;
                    startTS = endTS;
                    endTS -= incrementMS;
                }
            }
        }
        maxInitTasks = configuration.getControllerList().length;
        logger.info("Setting max init tasks to %d",maxInitTasks);
        long maxProgressBarTasks = maxInitTasks
                +(configuration.getControllerList().length * maxRunTasks)
                +29
                +maxZipTasks;
        ProgressBar progressBar = new ProgressBar("AppD Saas Export", maxProgressBarTasks);
        progressBar.start();
        progressBar.setExtraMessage("Initializing Workers....");
        //progressBar.maxHint( );
        long startTimestamp= TimeUtil.now();
        for( ControllerDatabase controllerDatabase : configuration.getControllerList() ) {
            progressBar.step();
            switch (configuration.getMigrationLevel()) { //1 = App, 2 = 1+Tiers+nodes, 3= 2+BT+ALL
                case 3: { //TODO implement the deeper methods
                }
                case 2: {
                    long saveStartTS = startTimestamp;
                    long endTimestamp = startTimestamp - incrementMS;
                    while( startTimestamp > TimeUtil.getDaysBackTimestamp(configuration.getDaysToRetrieveData()) ) {
                        futures.addAll( submitJob(new MetricDatabaseReaderTask("node", controllerDatabase, startTimestamp, endTimestamp, configuration, dataToConvertLinkedBlockingQueue)));
                        futures.addAll( submitJob(new MetricDatabaseReaderTask("tier", controllerDatabase, startTimestamp, endTimestamp, configuration, dataToConvertLinkedBlockingQueue)));
                        startTimestamp = endTimestamp;
                        endTimestamp -= incrementMS;
                    }
                    startTimestamp = saveStartTS;
                }
                case 1: {
                    long endTimestamp = startTimestamp - incrementMS;
                    while( startTimestamp > TimeUtil.getDaysBackTimestamp(configuration.getDaysToRetrieveData()) ) {
                        futures.addAll( submitJob(new MetricDatabaseReaderTask( "app", controllerDatabase, startTimestamp, endTimestamp, configuration, dataToConvertLinkedBlockingQueue)));
                        startTimestamp = endTimestamp;
                        endTimestamp -= incrementMS;
                    }
                }

            }
        }
        logger.info("Scheduled a total of %d jobs to run fetching data for export to CSV", futures.size());
        progressBar.setExtraMessage("Exporting Data.....");
        int maxFutures = futures.size();
        int counter=0;
        bigLoop: while(!futures.isEmpty()) {
            logger.info("Size of futures %d counter %d",futures.size(), counter);
            for (Future<?> future : futures) {
                if( counter >= maxFutures ) break bigLoop;
                try {
                    future.get(100, TimeUnit.MILLISECONDS);
                    progressBar.step();
                    counter++;
                } catch (InterruptedException e) {
                    logger.warn("Interrupted exception in future task wait: %s", e.toString(), e);
                } catch (ExecutionException | RuntimeException e) {
                    logger.fatal("Execution Exception in task wait: %s", e.toString(), e);
                    System.err.println("Fatal Error in run, one of the worker tasks had a really weird error, so we are going to stop. Please send the log file and any error on standard output to: " + MetaData.CONTACT_GECOS);
                    System.exit(1);
                } catch (TimeoutException e) {
                    //no op
                }
            }
        }
        progressBar.setExtraMessage("Completed, shutting down workers...");
        logger.info("All Data Fetch jobs scheduled have completed, now waiting for the Workers to finish processing");
        configuration.setRunning(false);
        executorFetchData.shutdown();
        try {
            while (!executorFetchData.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                progressBar.setExtraMessage("Waiting for Database Reader Tasks to finish");
                logger.info("Still waiting for Database Reader Tasks to finish");
            }
        } catch (InterruptedException ignored) {}
        progressBar.step();
        executorConvertData.shutdown();
        try {
            while (!executorConvertData.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                progressBar.setExtraMessage("Waiting for Data Conversion Tasks to finish");
                logger.info("Still waiting for Data Conversion Tasks to finish");
            }
        } catch (InterruptedException ignored) {}
        progressBar.step();
        executorInsertData.shutdown();
        try {
            while (!executorInsertData.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                progressBar.setExtraMessage("Waiting for CSV Writer Tasks to finish");
                logger.info("Still waiting for CSV Writer Tasks to finish");
            }
        } catch (InterruptedException ignored) {}
        progressBar.step();
        logger.info("Everything is written, shutting everything down");
        try {
            configuration.getCSVMetricWriter().close();
        } catch (IOException e) {
            logger.warn("Error closing csv output files: %s",e.toString());
        }
        progressBar.step();
        long finishTimestamp = System.currentTimeMillis();
        DetailsFile detailsFile = new DetailsFile(configuration, startTimestamp, finishTimestamp);
        List<File> fileList = configuration.getCSVMetricWriter().getMetricFiles();
        fileList.add(detailsFile.getFile());
        new ZipFileMaker(configuration.getOutputDir(), configuration.getTargetController().url.getHost(), fileList, progressBar);
        progressBar.setExtraMessage("Done!");
        progressBar.stepTo(maxProgressBarTasks);
        progressBar.stop();
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
