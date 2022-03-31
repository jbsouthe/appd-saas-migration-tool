package com.cisco.josouthe.output;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.MetaData;
import com.cisco.josouthe.controller.ControllerDatabase;
import com.cisco.josouthe.controller.apidata.model.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Date;

public class DetailsFile {
    protected static final Logger logger = LogManager.getFormatterLogger();

    private File file;
    private boolean completed=false;
    public DetailsFile(Configuration configuration, long startTimestamp, long finishTimestamp) {
        this.file = new File( configuration.getOutputDir(), "README.txt");
        try {
            logger.info("Creating detailed run description file: ", file);
            PrintWriter printer = new PrintWriter(new BufferedWriter(new FileWriter(this.file)));
            printer.println(String.format("Export started at %s and finished at %s", new Date(startTimestamp), new Date(finishTimestamp)));
            double durationMinutes = finishTimestamp - startTimestamp;
            durationMinutes /= 60000;
            printer.println(String.format("Run Duration: %.2f(minutes) Threads: database(%d) converters(%d) writers(%d)",
                    durationMinutes,
                    configuration.getProperty("scheduler-NumberOfDatabaseThreads", 15),
                    configuration.getProperty("scheduler-NumberOfConverterThreads", 30),
                    configuration.getProperty("scheduler-NumberOfWriterThreads", 5)
            ));
            printer.println(String.format("Target SaaS Controller for data files: %s",configuration.getTargetController().url));
            printer.println(String.format("This data set is for a level %d migration", configuration.getMigrationLevel()));
            printer.println(String.format("Numbers of days to export: %d", configuration.getDaysToRetrieveData()));
            printer.println(String.format("Applications with data being migrated:"));
            for(ControllerDatabase controllerDatabase : configuration.getControllerList()) {
                for( String application : controllerDatabase.getApplicationsFilters() )
                    printer.println(String.format("\t%s",application));
            }
            printer.println("Data Files to Import into BLITZ:");
            for( File dataFile : configuration.getCSVMetricWriter().getMetricFiles() )
                printer.println(String.format("\t%s",dataFile.getName()));
            printer.println(String.format("SaaS Exporter Version: %s Build: %s Author: %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.CONTACT_GECOS));
            printer.flush();
            printer.close();
            completed=true;
            logger.info("Succeeded in creating complete run description file: %s", file);
        } catch (IOException e) {
            logger.warn("Error creating export details log, will need to run without it, exception: %s", e.toString());
        }
    }

    public File getFile() {
        if( completed ) return file;
        return null;
    }
}
