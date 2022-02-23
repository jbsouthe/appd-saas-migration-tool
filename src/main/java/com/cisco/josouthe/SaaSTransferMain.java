package com.cisco.josouthe;

import com.cisco.josouthe.controller.Controller;
import com.cisco.josouthe.controller.ControllerDatabase;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.csv.CSVMetricWriter;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.IOException;

public class SaaSTransferMain {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static void main( String... args ) {
        logger.info("Initializing SaaS Transfer Tool version %s build date %s, please report any problems directly to %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP, MetaData.CONTACT_GECOS);
        String configFileName = "default-config.xml";
        if( args.length > 0 ) configFileName=args[0];
        Configuration config = null;
        try {
            config = new Configuration(configFileName);
        } catch (IOException e) {
            logger.fatal("Can not read configuration file: %s Exception: %s", configFileName, e.getMessage());
            return;
        } catch (SAXException e) {
            logger.fatal("XML Parser Error reading config file: %s Exception: %s", configFileName, e.getMessage());
            return;
        } catch (Exception e) {
            logger.fatal("A configuration exception was thrown that we can't handle, so we are quiting, Exception: ",e);
            return;
        }

        CSVMetricWriter csvMetricWriter = config.getCSVMetricWriter();
        for( ControllerDatabase controllerDatabase : config.getControllerList() ) {
            switch (config.getMigrationLevel()) { //1 = App, 2 = 1+Tiers+nodes, 3= 2+BT+ALL
                case 3: { //TODO implement the deeper methods
                }
                case 2: {
                    try {
                        csvMetricWriter.writeMetricsToFile(controllerDatabase.getAllMetrics("node", config.getMigrationLevel(), config.getDaysToRetrieveData()));
                    } catch (InvalidConfigurationException e) {
                        logger.warn("Could not get metrics for controller %s, because: %s", controllerDatabase.toString(), e.toString());
                    }
                    try {
                        csvMetricWriter.writeMetricsToFile(controllerDatabase.getAllMetrics("tier", config.getMigrationLevel(), config.getDaysToRetrieveData()));
                    } catch (InvalidConfigurationException e) {
                        logger.warn("Could not get metrics for controller %s, because: %s", controllerDatabase.toString(), e.toString());
                    }
                }
                case 1: {
                    try {
                        csvMetricWriter.writeMetricsToFile(controllerDatabase.getAllMetrics("app", config.getMigrationLevel(), config.getDaysToRetrieveData()));
                    } catch (InvalidConfigurationException e) {
                        logger.warn("Could not get metrics for controller %s, because: %s", controllerDatabase.toString(), e.toString());
                    }
                }

            }
        }
        try {
            csvMetricWriter.close();
        } catch (IOException e) {
            logger.warn("Error closing csv output files: %s",e.toString());
        }

    }
}
