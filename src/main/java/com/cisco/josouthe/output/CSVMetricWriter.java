package com.cisco.josouthe.output;

import com.cisco.josouthe.controller.TargetController;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricValue;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
    private boolean appWritten = false;
    private boolean tierWritten = false;
    private boolean nodeWritten = false;

    public CSVMetricWriter(String outputDir, TargetController targetController ) {
        this.outputDirName = outputDir;
        File outputDirectory = new File(outputDirName);
        if( !outputDirectory.exists() ) outputDirectory.mkdirs();
        applicationComponentNodeFile = new File( outputDirectory, "blitz-application-component-node.csv");
        applicationNodeFile = new File( outputDirectory, "blitz-application-node.csv");
        applicationFile = new File( outputDirectory, "blitz-application.csv");
        this.targetController = targetController;
    }

    public List<File> getMetricFiles() {
        List<File> fileList = new ArrayList<>();
        if( appWritten ) fileList.add(applicationFile);
        if( tierWritten ) fileList.add(applicationComponentNodeFile);
        if( nodeWritten ) fileList.add(applicationNodeFile);
        return fileList;
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
            long counter=0;
            for (DatabaseMetricValue metricValue : metricValueCollection.getMetrics()) {
                switch (metricValue.getBlitzEntityTypeString()) { //this is found in the com.appdynamics.blitz.shared.hbase.dto.MetricValueLineFromCSV class
                    /*
                    try (Statement st = conn.createStatement()) {
                        String query =
                        "SELECT m.ts_min * 60000, m.metric_id, m.rollup_type, m.cluster_rollup_type, m.count_val, m"
                        + ".sum_val,"
                        +
                        " m.min_val, m.max_val, m.cur_val, m.weight_value_square, m.weight_value, a.account_id, ";

                        switch (tableName) {
                            case DataMigrationConstants.MYSQL_NODE_TABLE_NAME:
                                query += " 1, m.application_id, m.application_component_instance_id, m.node_id";
                                break;
                            case DataMigrationConstants.MYSQL_TIER_TABLE_NAME:
                                query += "m.group_count_val, m.application_id, m.application_component_instance_id";
                                break;
                            case DataMigrationConstants.MYSQL_APP_TABLE_NAME:
                                query += "m.group_count_val, m.application_id";
                                break;
                            default:
                              throw new IllegalArgumentException("invalid table " + tableName);
                        }

                        query += " INTO OUTFILE " + outputFile +
                        " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'" + " LINES TERMINATED BY '\\n'" +
                        "FROM " + tableName + " m, application a where m.application_id = a.id and ts_min >= " +
                        endTimeStamp / TimeUnit.MINUTES.toMillis(1) +
                        " and ts_min < " + startTimeStamp / TimeUnit.MINUTES.toMillis(1);
                        log.info("Executing the SQL query: " + query);
                        st.execute(query);
                    }
                     */
                        case "app": {
                            appWritten=true;
                            writeCSVValue(aPrinter, metricValue.ts_min*60000);
                            writeCSVValue(aPrinter, metricValue.metric_id);
                            writeCSVValue(aPrinter, metricValue.rollup_type);
                            writeCSVValue(aPrinter, metricValue.cluster_rollup_type);
                            writeCSVValue(aPrinter, metricValue.count_val);
                            writeCSVValue(aPrinter, metricValue.sum_val);
                            writeCSVValue(aPrinter, metricValue.min_val);
                            writeCSVValue(aPrinter, metricValue.max_val);
                            writeCSVValue(aPrinter, metricValue.cur_val);
                            writeCSVValue(aPrinter, metricValue.weight_value_square);
                            writeCSVValue(aPrinter, metricValue.weight_value);
                            writeCSVValue(aPrinter, metricValue.account_id);
                            writeCSVValue(aPrinter, metricValue.group_count_val);
                            writeCSVValue(aPrinter, metricValue.application_id, true);
                            break;
                        }
                        case "tier": {
                            tierWritten=true;
                            writeCSVValue(acnPrinter, metricValue.ts_min*60000);
                            writeCSVValue(acnPrinter, metricValue.metric_id);
                            writeCSVValue(acnPrinter, metricValue.rollup_type);
                            writeCSVValue(acnPrinter, metricValue.cluster_rollup_type);
                            writeCSVValue(acnPrinter, metricValue.count_val);
                            writeCSVValue(acnPrinter, metricValue.sum_val);
                            writeCSVValue(acnPrinter, metricValue.min_val);
                            writeCSVValue(acnPrinter, metricValue.max_val);
                            writeCSVValue(acnPrinter, metricValue.cur_val);
                            writeCSVValue(acnPrinter, metricValue.weight_value_square);
                            writeCSVValue(acnPrinter, metricValue.weight_value);
                            writeCSVValue(acnPrinter, metricValue.account_id);
                            writeCSVValue(acnPrinter, metricValue.group_count_val);
                            writeCSVValue(acnPrinter, metricValue.application_id);
                            writeCSVValue(acnPrinter, metricValue.application_component_instance_id, true);
                            break;
                        }
                        case "node": {
                            nodeWritten=true;
                            writeCSVValue(anPrinter, metricValue.ts_min*60000);
                            writeCSVValue(anPrinter, metricValue.metric_id);
                            writeCSVValue(anPrinter, metricValue.rollup_type);
                            writeCSVValue(anPrinter, metricValue.cluster_rollup_type);
                            writeCSVValue(anPrinter, metricValue.count_val);
                            writeCSVValue(anPrinter, metricValue.sum_val);
                            writeCSVValue(anPrinter, metricValue.min_val);
                            writeCSVValue(anPrinter, metricValue.max_val);
                            writeCSVValue(anPrinter, metricValue.cur_val);
                            writeCSVValue(anPrinter, metricValue.weight_value_square);
                            writeCSVValue(anPrinter, metricValue.weight_value);
                            writeCSVValue(anPrinter, metricValue.account_id);
                            writeCSVValue(anPrinter, 1);
                            writeCSVValue(anPrinter, metricValue.application_id);
                            writeCSVValue(anPrinter, metricValue.application_component_instance_id);
                            writeCSVValue(anPrinter, metricValue.node_id, true);
                            break;
                        }
                    }
                counter++;
            }
            flush();
            logger.info("Wrote %d of %d metrics to CSV files", counter, metricValueCollection.getMetrics().size());
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

    public synchronized void close() throws IOException {
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
