package com.cisco.josouthe.controller.apidata.model;

import com.cisco.josouthe.controller.Controller;
import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Application implements Comparable<Application> {
    private static final Logger logger = LogManager.getFormatterLogger();
    public String name, accountGuid;
    public long id;
    public boolean active;
    public List<Tier> tiers = new ArrayList<>();
    public List<Node> nodes = new ArrayList<>();
    public List<BusinessTransaction> businessTransactions = new ArrayList<>();
    public List<Backend> backends = new ArrayList<>();
    public List<ServiceEndpoint> serviceEndpoints = new ArrayList<>();
    private Controller controller;
    private Map<String,MetricData> controllerMetricMap = new HashMap<>();
    private Map<String,Integer> controllerMetricLookupCountMap = new HashMap<>();
    private Map<Long, DatabaseMetricDefinition> metricsMap;
    public boolean getAllAvailableMetrics=false;
    private boolean finishedInitialization=false;

    public Application() {} //for GSON

    public Application( String name ) {
        this.name = name;
    }

    public String toString() {
        return String.format("%s Tiers: %d Nodes: %d Business Transactions: %d Backends: %d Service Endpoints: %d", name, tiers.size(), nodes.size(), businessTransactions.size(), backends.size(), serviceEndpoints.size());
    }

    public void setController( Controller controller ) {
        this.controller=controller;
    }

    public Set<String> getMetricNames() {
        init();
        return controllerMetricMap.keySet();
    }

    public MetricData getControllerMetricData(String blitzEntityTypeString, String metricName, DatabaseMetricDefinition databaseMetricDefinition) {
        init();
        MetricData metricData = controllerMetricMap.get(metricName);
        if( metricData == null && getControllerMetricLookupCount(metricName) <= 3 ) {
            outerLoop: for( String metricPath : readMetricPathsForType(blitzEntityTypeString) ) {
                innerLoop: for (MetricData metric : controller.getMetricValue(this.id, metricPath, true)) {
                    if (metric.metricName.equals(metricName)) {
                        switch (blitzEntityTypeString) {
                            case "node": { if( !metric.metricPath.contains(databaseMetricDefinition.nodeName) ) continue innerLoop; }
                            case "tier": { if( !metric.metricPath.contains(databaseMetricDefinition.tierName) ) continue innerLoop; }
                            case "app": { /*keeping this for completeness, but their is no app specific test */ }
                        }
                        metricData = metric;
                        break outerLoop;
                    }
                }
            }
            if( metricData != null ) {
                controllerMetricMap.put(metricName, metricData);
                logger.info("Metric %s returning metric id %d", metricName, metricData.metricId);
            } else {
                controllerMetricLookupCountMap.put(metricName, getControllerMetricLookupCount(metricName)+1);
                logger.warn("Metric not found after deep search of controller, type: %s name: %s metric definition: %s", blitzEntityTypeString, metricName, databaseMetricDefinition);
            }
        }
        return metricData;
    }

    private int getControllerMetricLookupCount(String metricName) {
        return controllerMetricLookupCountMap.getOrDefault(metricName, 0);
    }


    public Tier getTier( String name ) {
        init();
        for( Tier tier : tiers )
            if( tier.name.equals(name) ) return tier;
        return null;
    }

    public Tier getTier( Tier o ) {
        init();
        for( Tier tier : tiers )
            if( tier.equals(o) ) return tier;
        return null;
    }

    public Tier getTier( long id ) {
        init();
        for( Tier tier : tiers )
            if( tier.id == id ) return tier;
        return null;
    }

    public Node getNode( String name ) {
        init();
        for( Node node : nodes )
            if( node.name.equals(name) ) return node;
        return null;
    }

    public Node getNode( Node o ) {
        init();
        for( Node node : nodes )
            if( node.equals(o) ) return node;
        return null;
    }

    public Node getNode( long id ) {
        init();
        for( Node node : nodes )
            if( node.id == id ) return node;
        return null;
    }

    public BusinessTransaction getBusinessTransaction( String name ) {
        init();
        for( BusinessTransaction businessTransaction : businessTransactions )
            if( businessTransaction.name.equals(name) ) return businessTransaction;
        return null;
    }

    public BusinessTransaction getBusinessTransaction( BusinessTransaction o ) {
        init();
        for( BusinessTransaction businessTransaction : businessTransactions )
            if( businessTransaction.equals(o) ) return businessTransaction;
        return null;
    }

    public BusinessTransaction getBusinessTransaction(Long btId) {
        init();
        for( BusinessTransaction businessTransaction : businessTransactions )
            if( businessTransaction.id == btId ) return businessTransaction;
        return null;
    }

    public Backend getBackend( String name ) {
        init();
        for( Backend backend : backends )
            if( backend.name.equals(name) ) return backend;
        return null;
    }

    public Backend getBackend( Backend o ) {
        init();
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
        if( isControllerNull() ) return;
        if( !isFinishedInitialization() ) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = null;
            try {
                json = controller.getRequest("controller/rest/applications/%d/tiers?output=JSON", id);
                Tier[] tiersList = gson.fromJson(json, Tier[].class);
                if (tiersList != null) {
                    for (Tier tier : tiersList)
                        tiers.add(tier);
                    logger.info("Added %d Tiers to Application: %s", tiersList.length, name);
                }
            } catch (ControllerBadStatusException controllerBadStatusException ) {
                logger.error("Error initializing Application %s, message: %s", name, controllerBadStatusException.getMessage());
            }
            try {
                json = controller.getRequest("controller/rest/applications/%d/nodes?output=JSON", id);
                Node[] nodesList = gson.fromJson(json, Node[].class);
                if( nodesList != null ) {
                    for (Node node : nodesList)
                        nodes.add(node);
                    logger.info("Added %d Nodes to Application: %s", nodesList.length, name);
                }
            } catch (ControllerBadStatusException controllerBadStatusException ) {
                logger.error("Error initializing Application %s, message: %s", name, controllerBadStatusException.getMessage());
            }
            try {
                json = controller.getRequest("controller/rest/applications/%d/business-transactions?output=JSON", id);
                BusinessTransaction[] businessTransactionsList = gson.fromJson(json, BusinessTransaction[].class);
                if (businessTransactionsList != null) {
                    for (BusinessTransaction businessTransaction : businessTransactionsList)
                        businessTransactions.add(businessTransaction);
                    logger.info("Added %d Business Transactions to Application: %s", businessTransactionsList.length, name);
                }
            } catch (ControllerBadStatusException controllerBadStatusException ) {
                logger.error("Error initializing Application %s, message: %s", name, controllerBadStatusException.getMessage());
            }
            try {
                json = controller.getRequest("controller/rest/applications/%d/backends?output=JSON", id);
                Backend[] backendsList = gson.fromJson(json, Backend[].class);
                if (backendsList != null) {
                    for (Backend backend : backendsList)
                        backends.add(backend);
                    logger.info("Added %d Backends to Application: %s", backendsList.length, name);
                }
            } catch (ControllerBadStatusException controllerBadStatusException ) {
                logger.error("Error initializing Application %s, message: %s", name, controllerBadStatusException.getMessage());
            }
            try {
                serviceEndpoints.addAll(controller.getServiceEndpoints(id));
            } catch (ControllerBadStatusException controllerBadStatusException ) {
                logger.error("Error initializing Application %s, message: %s", name, controllerBadStatusException.getMessage());
            }
            logger.info("Added %d Service Endpoints to Application: %s", serviceEndpoints.size(), name);
            synchronized (this.controllerMetricMap) {
                if( isFinishedInitialization() ) return; //only let the first one run, all others return quickly once unblocked
                if (getAllAvailableMetrics) {
                    TreeNode[] folders = controller.getApplicationMetricFolders(this, "");
                    logger.debug("Application %s Found %d folders we can go into", this.name, (folders == null ? "0" : folders.length));
                    findMetrics(controller, folders, "");
                } else {
                    switch (controller.getConfiguration().getMigrationLevel() ) {
                        case 2: {
                            /*
                            TreeNode[] folders = controller.getApplicationMetricFolders(this, "Application Infrastructure Performance");
                            logger.debug("Found %d folders we can go into", (folders == null ? "0" : folders.length));
                            findMetrics(controller, folders, "");
                             */
                            try {
                                for( MetricData metricData : initializeMetricCache("Application", "Tier", "Node", "Business Transaction") ) {
                                    controllerMetricMap.put(metricData.metricName, metricData);
                                }
                            } catch (IOException e) {
                                logger.warn("IOError initializing metrics from MetricPaths.csv, exception: %s", e.toString());
                            }
                            break;
                        }
                        case 1: {
                            try {
                                for( MetricData metricData : initializeMetricCache("Application") ) {
                                    controllerMetricMap.put(metricData.metricName, metricData);
                                }
                            } catch (IOException e) {
                                logger.warn("IOError initializing metrics from MetricPaths.csv, exception: %s", e.toString());
                            }
                            break;
                        }
                        default: {
                            logger.warn("Unsupported Level configuration: %d Rethink your configuration file settings", controller.getConfiguration().getMigrationLevel());
                        }

                    }
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
            } else if( "Custom Metrics".contains(path + something.name)) {
                logger.debug("Adding metric: %s%s",path,something.name);
                controllerMetricMap.put(path+something.name, null);
            }
        }
    }
    public String getName() { return this.name; }

    public void addMetricDefinition(DatabaseMetricDefinition metricDefinition) {
        if( metricsMap == null ) metricsMap = new HashMap<>();
        if( metricDefinition.btId != null ) {
            BusinessTransaction businessTransaction = getBusinessTransaction(metricDefinition.btId);
            if( businessTransaction != null )
                metricDefinition.btName = businessTransaction.name;
        }
        if( metricDefinition.tierId != null ) {
            Tier tier = getTier( metricDefinition.tierId );
            if( tier != null )
                metricDefinition.tierName = tier.name;
        }
        if( metricDefinition.seId != null ) {
            ServiceEndpoint serviceEndpoint = getServiceEndpoint(metricDefinition.seId);
            if( serviceEndpoint != null ) {
                metricDefinition.seName = serviceEndpoint.name;
            }
        }
        metricsMap.put(metricDefinition.metricId, metricDefinition);
    }

    public DatabaseMetricDefinition getMetricDefinition( long id ) {
        return metricsMap.get(id);
    }

    public boolean isControllerNull() { return controller == null; }

    private List<MetricData> initializeMetricCache( String... types ) throws IOException {
        List<MetricData> metricDataList = new ArrayList<>();
        for( String type : types)
            for( String metricPath : readMetricPathsForType(type) ) {
                for( MetricData metricData : controller.getMetricValue(this.id, metricPath, true) )
                    metricDataList.add(metricData);
            }
        return metricDataList;
    }

    private static ConcurrentHashMap<String,List<String>> _readMetricPathsForTypeCache = null;
    private static synchronized List<String> readMetricPathsForType(String type) {
        if( _readMetricPathsForTypeCache == null ) _readMetricPathsForTypeCache = new ConcurrentHashMap<>();
        List<String> metricPaths = _readMetricPathsForTypeCache.get(type);
        if( metricPaths != null ) return metricPaths;

        metricPaths = new ArrayList<>();
        BufferedReader reader = new BufferedReader( new InputStreamReader( type.getClass().getResourceAsStream("/MetricPaths.csv")));
        String line = null;
        try {
            line = reader.readLine();
        } catch (IOException e) {
            logger.error("Error reading metric path expressions, we need to stop and figure this out! Exception: %s",e);
        }
        while( line!=null && !line.isEmpty() ) {
            String[] parts = line.split(",");
            if( type.equalsIgnoreCase(parts[0])) {
                metricPaths.add(parts[1]);
            }
            try {
                line = reader.readLine();
            } catch (IOException e) {
                logger.error("Error reading metric path expressions, we need to stop and figure this out! Exception: %s",e);
                line=null;
            }
        }

        _readMetricPathsForTypeCache.put(type,metricPaths);
        return metricPaths;
    }

    public ServiceEndpoint getServiceEndpoint(Long serviceEndpointId) {
        for( ServiceEndpoint serviceEndpoint : serviceEndpoints )
            if( serviceEndpoint.id == serviceEndpointId ) return serviceEndpoint;
        return null;
    }

    public ServiceEndpoint getServiceEndpoint(ServiceEndpoint sourceServiceEndpoint) {
        return getServiceEndpoint(sourceServiceEndpoint.name, sourceServiceEndpoint.applicationComponent.name);
    }

    public ServiceEndpoint getServiceEndpoint(String serviceEndpointName, String componentName) {
        for( ServiceEndpoint serviceEndpoint : serviceEndpoints ) {
            if( serviceEndpoint.name.equals(serviceEndpointName)
                    && serviceEndpoint.applicationComponent.name.equals(componentName))
                return serviceEndpoint;
        }
        return null;
    }

    private static ConcurrentHashMap<String,List<MetricMatcher>> _getMetricMatcherAppCache = null;
    private static ConcurrentHashMap<String,List<MetricMatcher>> _getMetricMatcherTierCache = null;
    private static ConcurrentHashMap<String,List<MetricMatcher>> _getMetricMatcherNodeCache = null;
    private static ConcurrentHashMap<String,List<MetricMatcher>> getMetricMatcherCache( String type ) {
        switch (type) {
            case "node": { return _getMetricMatcherNodeCache; }
            case "tier": { return _getMetricMatcherTierCache; }
            case "app":  { return _getMetricMatcherAppCache; }
        }
        return null;
    }
    private static void setMetricMatcherCache( String type, ConcurrentHashMap<String,List<MetricMatcher>> map ) {
        switch (type) {
            case "node": {  _getMetricMatcherNodeCache=map; break; }
            case "tier": {  _getMetricMatcherTierCache=map; break; }
            case "app":  {  _getMetricMatcherAppCache=map; break; }
        }
    }
    public synchronized MetricMatcher getControllerMetricMatch(String blitzEntityTypeString, String metricNameOnController, DatabaseMetricDefinition databaseMetricDefinition) {
        if( metricNameOnController == null ) return null;
        ConcurrentHashMap<String,List<MetricMatcher>> _getMetricMatcherCache = getMetricMatcherCache(blitzEntityTypeString);
        if( _getMetricMatcherCache == null ) {
            _getMetricMatcherCache = new ConcurrentHashMap<>();
            logger.info("Initializing Metric Matcher for Application %s type %s",name, blitzEntityTypeString);
            int counter=0;
            for( String metricPath : readMetricPathsForType(blitzEntityTypeString)) {
                logger.debug("Metric Path: %s",metricPath);
                for( MetricData metricData : controller.getMetricValue(id, metricPath, true)) {
                    logger.debug("Metric Data: %s",metricData);
                    counter++;
                    List<MetricMatcher> metricMatcherList = _getMetricMatcherCache.get(metricData.metricName);
                    if( metricMatcherList == null ) metricMatcherList = new ArrayList<>();
                    metricMatcherList.add( new MetricMatcher(metricData));
                    _getMetricMatcherCache.put(metricData.metricName, metricMatcherList);
                }
            }
            logger.info("Initialized Metric Matchers for Application %s type %s, %d matchers", name, blitzEntityTypeString, counter);
            setMetricMatcherCache(blitzEntityTypeString, _getMetricMatcherCache);
        }
        List<MetricMatcher> metricMatchers = _getMetricMatcherCache.get(metricNameOnController);
        if( metricMatchers != null ) {
            for( MetricMatcher metricMatcher : metricMatchers ) {
                if( metricMatcher.matches(databaseMetricDefinition) ) return metricMatcher;
            }
        }
        logger.debug("Metric didn't match anything we have on the controller, this will be missing: %s", databaseMetricDefinition);
        return null;
    }
}
