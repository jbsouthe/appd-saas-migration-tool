package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.TargetController;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricValue;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.controller.dbdata.SourceModel;
import com.cisco.josouthe.exceptions.BadDataException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DataConverterTask implements Runnable {
    private static final Logger logger = LogManager.getFormatterLogger();
    private static final Logger metricLogger = LogManager.getFormatterLogger("METRIC_LOGGER");


    private Configuration configuration;
    private TargetController targetController;
    private LinkedBlockingQueue<MetricValueCollection> inputQueue, outputQueue;

    public DataConverterTask(Configuration configuration, LinkedBlockingQueue<MetricValueCollection> dataToConvertLinkedBlockingQueue, LinkedBlockingQueue<MetricValueCollection> dataToInsertLinkedBlockingQueue) {
        this.configuration=configuration;
        this.targetController = configuration.getTargetController();
        this.inputQueue=dataToConvertLinkedBlockingQueue;
        this.outputQueue=dataToInsertLinkedBlockingQueue;
    }

    @Override
    public void run() {
        boolean processedOne=false;
        while( !processedOne ) {
            try {
                MetricValueCollection metricValueCollection = inputQueue.poll(1, TimeUnit.MINUTES);
                if (metricValueCollection != null) {
                    long startTime = System.currentTimeMillis();
                    processedOne=true;
                    logger.info("Poll returned %d data elements to convert", metricValueCollection.getMetrics().size());
                    long targetAccountId = this.targetController.getAccountId();
                    SourceModel sourceModel = metricValueCollection.getSourceModel();
                    long counter = 0;
                    Long targetApplicationId = null;
                    List<DatabaseMetricValue> convertedMetrics = new ArrayList<>();
                    for (DatabaseMetricValue metricValue : metricValueCollection.getMetrics()) {
                        DatabaseMetricDefinition metricDefinition = metricValueCollection.getMetricDefinition(metricValue);
                        if (metricDefinition == null) continue;

                        switch (metricValue.getBlitzEntityTypeString()) {
                            case "node": {
                                metricDefinition.nodeName = sourceModel.getApplicationTierNodeName(metricValue.application_id, metricValue.node_id);
                            }
                            case "tier": {
                                metricDefinition.tierName = sourceModel.getApplicationTierName(metricValue.application_id, metricValue.application_component_instance_id);
                            }
                            case "app": {
                                String applicationName = sourceModel.getNewApplicationName(metricValue.application_id);
                                targetApplicationId = targetController.getEquivolentApplicationId(applicationName);
                                if (targetApplicationId == null) {
                                    logger.warn("We can not process this data, the application doesn't exist on the target controller!");
                                    continue;
                                }
                            }
                        }
                        Long targetMetricId = null;
                        try {
                            targetMetricId = targetController.getEquivolentMetricId(metricValue.getBlitzEntityTypeString(), targetApplicationId, metricDefinition);
                            metricValue.metric_id = targetMetricId;
                            if( targetMetricId == null ) continue;
                        } catch (BadDataException e) {
                            metricLogger.warn("Skipping this metric, we received a no data found metric: %s for metric: %s", e.toString(), metricDefinition);
                            continue;
                        }
                        switch (metricValue.getBlitzEntityTypeString()) {
                            case "node": {
                                String targetNodeName = sourceModel.getApplicationTierNodeName(metricValue.application_id, metricValue.node_id);
                                Long targetNodeId = targetController.getEquivolentNodeId(targetApplicationId, targetNodeName );
                                if (targetNodeId == null) {
                                    if( ! "UNKNOWN_NODE".equals(targetNodeName))
                                        logger.warn("Target Node Conversion Failed: %s(%d) not found on target controller", targetNodeName, metricValue.node_id);
                                    continue;
                                }
                                metricValue.node_id = targetNodeId;
                            }
                            case "tier": {
                                String targetTierName = sourceModel.getApplicationTierName(metricValue.application_id, metricValue.application_component_instance_id);
                                Long targetTierId = targetController.getEquivolentTierId(targetApplicationId, targetTierName);
                                if (targetTierId == null) {
                                    if( !"UNKNOWN_TIER".equals(targetTierName) )
                                        logger.warn("Target Tier Conversion Failed: %s(%d) not found on target controller", targetTierName, metricValue.application_component_instance_id);
                                    continue;
                                }
                                metricValue.application_component_instance_id = targetTierId;
                            }
                            case "app": {
                                metricValue.account_id = targetAccountId;
                                metricValue.application_id = targetApplicationId;
                            }
                        }
                        convertedMetrics.add(metricValue);
                    }
                    MetricValueCollection outputMetricCollection = new MetricValueCollection(sourceModel, convertedMetrics);
                    outputQueue.add(outputMetricCollection);
                    long runTime= System.currentTimeMillis() - startTime;
                    logger.info("Data conversion task processed %d metrics and produced %d metrics in %d milliseconds", metricValueCollection.getMetrics().size(), outputMetricCollection.getMetrics().size(), runTime);
                }
            } catch (InterruptedException e) {
                //ignore
            }

        }
        logger.debug("Shutting down data conversion worker Task");
    }
}
