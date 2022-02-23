package com.cisco.josouthe.csv;

import com.cisco.josouthe.controller.TargetController;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricValue;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.controller.dbdata.SourceModel;
import com.cisco.josouthe.exceptions.BadDataException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class CSVMetricWriter {
    protected static final Logger logger = LogManager.getFormatterLogger();

    private String outputDirName;
    private File applicationComponentNodeFile;
    private File applicationNodeFile;
    private File applicationFile;
    private BufferedWriter acnWriter, anWriter, aWriter;
    private PrintWriter acnPrinter, anPrinter, aPrinter;
    private TargetController targetController;
    private boolean isOpen = false;

    public CSVMetricWriter(String outputDir, TargetController targetController ) {
        this.outputDirName = outputDir;
        File outputDirectory = new File(outputDirName);
        if( !outputDirectory.exists() ) outputDirectory.mkdirs();
        applicationComponentNodeFile = new File( outputDirectory, "blitz-application-component-node.csv");
        applicationNodeFile = new File( outputDirectory, "blitz-application-node.csv");
        applicationFile = new File( outputDirectory, "blitz-application.csv");
        this.targetController = targetController;
    }

    public void open() throws IOException {
        if( !isOpen ) {
            acnWriter = new BufferedWriter(new FileWriter(applicationComponentNodeFile));
            acnPrinter = new PrintWriter(acnWriter);
            anWriter = new BufferedWriter(new FileWriter(applicationNodeFile));
            anPrinter = new PrintWriter(anWriter);
            aWriter = new BufferedWriter(new FileWriter(applicationFile));
            aPrinter = new PrintWriter(aWriter);
            isOpen = true;
        }
    }

    public synchronized void writeMetricsToFile(MetricValueCollection metricValueCollection) {

        try {
            open();
            long targetAccountId = this.targetController.getAccountId();
            SourceModel sourceModel = metricValueCollection.getSourceModel();
            for (DatabaseMetricValue metricValue : metricValueCollection.getMetrics()) {
                DatabaseMetricDefinition metricDefinition = metricValueCollection.getMetricDefinition(metricValue);
                String applicationName = sourceModel.getApplicationName(metricValue.application_id);
                Long targetApplicationId = targetController.getEquivolentApplicationId(applicationName);
                if( targetApplicationId == null ) {
                    logger.warn("We can not process this data, the application doesn't exist on the target controller!");
                    continue;
                }
                Long targetMetricId = null;
                try {
                    targetMetricId = targetController.getEquivolentMetricId(targetApplicationId, metricDefinition.metricName);
                } catch (BadDataException e) {
                    logger.warn("Skipping this metric, we received a no data found metric: %s",e.toString());
                    continue;
                }
                switch (metricValue.getBlitzEntityTypeString()) { //this is found in the com.appdynamics.blitz.shared.hbase.dto.MetricValueLineFromCSV class
                    /*
                    case APPLICATION:
                        this.timeStamp = Long.toString(Long.parseLong(csvLine.get(0)) * MIN_TO_MILLIS_FACTOR);
                        this.metricId = csvLine.get(1);
                        this.applicationId = csvLine.get(2);
                        this.accountId = csvLine.get(3);
                        this.groupCount = csvLine.get(4);
                        this.count = csvLine.get(5);
                        this.sum = csvLine.get(6);
                        this.min = csvLine.get(7);
                        this.max = csvLine.get(8);
                        this.current = csvLine.get(9);
                        if (includeWeightedValues) {
                            this.weightValueSquare = csvLine.get(10);
                            this.weightValue = csvLine.get(11);
                        }
                        this.timeRollupType = csvLine.get(12);
                        this.clusterRollupType = csvLine.get(13);
                        break;
                     */
                        case "app": {
                            writeCSVValue(aPrinter, metricValue.ts_min);
                            writeCSVValue(aPrinter, targetMetricId);
                            writeCSVValue(aPrinter, targetApplicationId);
                            writeCSVValue(aPrinter, targetAccountId);
                            writeCSVValue(aPrinter, metricValue.group_count_val);
                            writeCSVValue(aPrinter, metricValue.count_val);
                            writeCSVValue(aPrinter, metricValue.sum_val);
                            writeCSVValue(aPrinter, metricValue.min_val);
                            writeCSVValue(aPrinter, metricValue.max_val);
                            writeCSVValue(aPrinter, metricValue.cur_val);
                            writeCSVValue(aPrinter, metricValue.weight_value_square);
                            writeCSVValue(aPrinter, metricValue.weight_value);
                            writeCSVValue(aPrinter, metricDefinition.timeRollupType);
                            writeCSVValue(aPrinter, metricDefinition.clusterRollupType, true);
                            break;
                        }
                    /*
                    case APPLICATION_COMPONENT:
                        this.timeStamp = Long.toString(Long.parseLong(csvLine.get(0)) * MIN_TO_MILLIS_FACTOR);
                        this.metricId = csvLine.get(1);
                        this.tierId = csvLine.get(2);
                        this.applicationId = csvLine.get(3);
                        this.accountId = csvLine.get(4);
                        this.groupCount = csvLine.get(5);
                        this.count = csvLine.get(6);
                        this.sum = csvLine.get(7);
                        this.min = csvLine.get(8);
                        this.max = csvLine.get(9);
                        this.current = csvLine.get(10);
                        if (includeWeightedValues) {
                            this.weightValueSquare = csvLine.get(11);
                            this.weightValue = csvLine.get(12);
                        }
                        this.timeRollupType = csvLine.get(13);
                        this.clusterRollupType = csvLine.get(14);
                        break;
                     */
                        case "tier": {
                            Long targetTierId = targetController.getEquivolentTierId(targetApplicationId, sourceModel.getApplicationTierName(metricValue.application_id, metricValue.application_component_instance_id));
                            writeCSVValue(acnPrinter, metricValue.ts_min);
                            writeCSVValue(acnPrinter, targetMetricId);
                            writeCSVValue(acnPrinter, targetTierId);
                            writeCSVValue(acnPrinter, targetApplicationId);
                            writeCSVValue(acnPrinter, targetAccountId);
                            writeCSVValue(acnPrinter, metricValue.group_count_val);
                            writeCSVValue(acnPrinter, metricValue.count_val);
                            writeCSVValue(acnPrinter, metricValue.sum_val);
                            writeCSVValue(acnPrinter, metricValue.min_val);
                            writeCSVValue(acnPrinter, metricValue.max_val);
                            writeCSVValue(acnPrinter, metricValue.cur_val);
                            writeCSVValue(acnPrinter, metricValue.weight_value_square);
                            writeCSVValue(acnPrinter, metricValue.weight_value);
                            writeCSVValue(acnPrinter, metricDefinition.timeRollupType);
                            writeCSVValue(acnPrinter, metricDefinition.clusterRollupType, true);
                            break;
                        }
                    /*
                    case APPLICATION_COMPONENT_NODE:
                        this.timeStamp = Long.toString(Long.parseLong(csvLine.get(0)) * MIN_TO_MILLIS_FACTOR);
                        this.metricId = csvLine.get(1);
                        this.timeRollupType = csvLine.get(2);
                        this.clusterRollupType = csvLine.get(3);
                        this.nodeId = csvLine.get(4);
                        this.tierId = csvLine.get(5);
                        this.applicationId = csvLine.get(6);
                        this.accountId = csvLine.get(7);
                        this.count = csvLine.get(8);
                        this.sum = csvLine.get(9);
                        this.min = csvLine.get(10);
                        this.max = csvLine.get(11);
                        this.current = csvLine.get(12);
                        if (includeWeightedValues) {
                            this.weightValueSquare = csvLine.get(13);
                            this.weightValue = csvLine.get(14);
                        }
                        break;
                     */
                        case "node": {
                            Long targetTierId = targetController.getEquivolentTierId(targetApplicationId, sourceModel.getApplicationTierName(metricValue.application_id, metricValue.application_component_instance_id));
                            Long targetNodeId = targetController.getEquivolentNodeId(targetApplicationId, sourceModel.getApplicationTierNodeName(metricValue.application_id, metricValue.node_id));
                            writeCSVValue(anPrinter, metricValue.ts_min);
                            writeCSVValue(anPrinter, targetMetricId);
                            writeCSVValue(anPrinter, metricDefinition.timeRollupType);
                            writeCSVValue(anPrinter, metricDefinition.clusterRollupType);
                            writeCSVValue(anPrinter, targetNodeId);
                            writeCSVValue(anPrinter, targetTierId);
                            writeCSVValue(anPrinter, targetApplicationId);
                            writeCSVValue(anPrinter, targetAccountId);
                            writeCSVValue(anPrinter, metricValue.count_val);
                            writeCSVValue(anPrinter, metricValue.sum_val);
                            writeCSVValue(anPrinter, metricValue.min_val);
                            writeCSVValue(anPrinter, metricValue.max_val);
                            writeCSVValue(anPrinter, metricValue.cur_val);
                            writeCSVValue(anPrinter, metricValue.weight_value_square);
                            writeCSVValue(anPrinter, metricValue.weight_value, true);
                            break;
                        }
                    }
            }
            flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeCSVValue( PrintWriter pw, Object value ) {
        writeCSVValue(pw,value, false);
    }
    private void writeCSVValue( PrintWriter pw, Object value, boolean endOfLine) {
        pw.print(value);
        if( endOfLine ) {
            pw.println();
        } else {
            pw.print(",");
        }
    }

    private void flush() throws IOException {
        if( isOpen ) {
            acnPrinter.flush();
            anPrinter.flush();
            aPrinter.flush();
        }
    }

    public void close() throws IOException {
        if( isOpen ) {
            acnPrinter.flush();
            acnWriter.close();
            anPrinter.flush();
            anWriter.close();
            aPrinter.flush();
            aWriter.close();
            isOpen=false;
        }
    }
}
