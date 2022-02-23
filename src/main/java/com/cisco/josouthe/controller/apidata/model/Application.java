package com.cisco.josouthe.controller.apidata.model;

import com.cisco.josouthe.controller.Controller;
import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import com.cisco.josouthe.exceptions.BadDataException;
import com.cisco.josouthe.util.TimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Application implements Comparable<Application> {
    private static final Logger logger = LogManager.getFormatterLogger();
    public String name, accountGuid;
    public long id;
    public boolean active;
    public List<Tier> tiers = new ArrayList<>();
    public List<Node> nodes = new ArrayList<>();
    public List<BusinessTransaction> businessTransactions = new ArrayList<>();
    public List<Backend> backends = new ArrayList<>();
    private Controller controller;
    private Map<String,MetricData> controllerMetricMap = new HashMap<>();
    private Map<Long, DatabaseMetricDefinition> metricsMap;
    public boolean getAllAvailableMetrics=true;
    private boolean finishedInitialization=false;

    public Application() {} //for GSON

    public Application( String name ) {
        this.name = name;
    }

    public void setController( Controller controller ) {
        this.controller=controller;
        init();
    }

    public Set<String> getMetricNames() {
        init();
        return controllerMetricMap.keySet();
    }

    public MetricData getControllerMetricData( String metricName ) throws BadDataException {
        init();
        if( metricName.contains("*") )
            logger.warn("Metric name contains a wildcard, this means it could return many metrics, but this method is only looking for the first metric in that set, we are not expecting this: %s", metricName);
        MetricData metricData = controllerMetricMap.get(metricName);
        if( metricData == null ) {
            for( MetricData metric : controller.getMetricValue(this.id, metricName, true) ) {
                if( metric.metricPath.equals(metricName) ) metricData = metric;
            }
            /* if( "METRIC DATA NOT FOUND".equals(metricData.metricName) ) { //oh man, this isn't good
                throw new BadDataException(String.format("Got a METRIC DATA NOT FOUND while trying to find a metric id, this may be a problem, metric returned: %s(%d)", metricData.metricName, metricData.metricId));
            } */
            if( metricData != null )
                controllerMetricMap.put(metricName,metricData);
        }
        return metricData;
    }


    public Tier getTier( String name ) {
        for( Tier tier : tiers )
            if( tier.name.equals(name) ) return tier;
        return null;
    }

    public Tier getTier( Tier o ) {
        for( Tier tier : tiers )
            if( tier.equals(o) ) return tier;
        return null;
    }

    public Tier getTier( long id ) {
        for( Tier tier : tiers )
            if( tier.id == id ) return tier;
        return null;
    }

    public Node getNode( String name ) {
        for( Node node : nodes )
            if( node.name.equals(name) ) return node;
        return null;
    }

    public Node getNode( Node o ) {
        for( Node node : nodes )
            if( node.equals(o) ) return node;
        return null;
    }

    public Node getNode( long id ) {
        for( Node node : nodes )
            if( node.id == id ) return node;
        return null;
    }

    public BusinessTransaction getBusinessTransaction( String name ) {
        for( BusinessTransaction businessTransaction : businessTransactions )
            if( businessTransaction.name.equals(name) ) return businessTransaction;
        return null;
    }

    public BusinessTransaction getBusinessTransaction( BusinessTransaction o ) {
        for( BusinessTransaction businessTransaction : businessTransactions )
            if( businessTransaction.equals(o) ) return businessTransaction;
        return null;
    }

    public Backend getBackend( String name ) {
        for( Backend backend : backends )
            if( backend.name.equals(name) ) return backend;
        return null;
    }

    public Backend getBackend( Backend o ) {
        for( Backend backend : backends )
            if( backend.equals(o) ) return backend;
        return null;
    }

    @Override
    public int compareTo( Application o ) {
        if( o == null ) return 1;
        if( o.name.equals(name) ) return 0;
        return -1;
    }

    public boolean equals( Application o ) {
        return compareTo(o) == 0;
    }

    public boolean isFinishedInitialization() { return finishedInitialization; }
    public void init() {
        if( !isFinishedInitialization() ) {
            synchronized (this.controllerMetricMap) {
                if( isFinishedInitialization() ) return; //only let the first one run, all others return quickly once unblocked
                if (getAllAvailableMetrics) {
                    TreeNode[] folders = controller.getApplicationMetricFolders(this, "");
                    logger.debug("Found %d folders we can go into", (folders == null ? "0" : folders.length));
                    findMetrics(controller, folders, "");
                } else {
                    TreeNode[] folders = controller.getApplicationMetricFolders(this, "Application Infrastructure Performance");
                    logger.debug("Found %d folders we can go into", (folders == null ? "0" : folders.length));
                    findMetrics(controller, folders, "");
                }
                this.finishedInitialization = true; //setting this here because we want to continue, even if partial data
            }
        }
    }

    private void findMetrics(Controller controller, TreeNode[] somethings, String path) {
        if( somethings == null || somethings.length == 0 ) return;
        if( !"".equals(path) ) path += "|";

        for( TreeNode something : somethings ) {
            if( something.isFolder() ) {
                findMetrics( controller, controller.getApplicationMetricFolders(this, path+something.name), path+something.name);
            } else if( "Custom Metrics".contains(path + something.name)){
                logger.debug("Adding metric: %s%s",path,something.name);
                controllerMetricMap.put(path+something.name, null);
            }
        }
    }
    public String getName() { return this.name; }

    public void addMetricDefinition(DatabaseMetricDefinition metricDefinition) {
        if( metricsMap == null ) metricsMap = new HashMap<>();
        metricsMap.put(metricDefinition.metricId, metricDefinition);
    }

    public DatabaseMetricDefinition getMetricDefinition( long id ) {
        return metricsMap.get(id);
    }

    public boolean isControllerNull() { return controller == null; }
}
