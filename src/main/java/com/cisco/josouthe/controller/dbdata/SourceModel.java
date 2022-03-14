package com.cisco.josouthe.controller.dbdata;

import com.cisco.josouthe.controller.apidata.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceModel {
    private static final Logger logger = LogManager.getFormatterLogger();
    public Map<Long,Account> accounts;

    public SourceModel(ResultSet resultSet) throws SQLException {
        accounts = new HashMap<>();
        while( resultSet.next() ) {
            long accountId = resultSet.getLong("acc_id");
            String accountName = resultSet.getString("acc_name");
            Account account = accounts.get(accountId);
            if( account == null ) {
                account = new Account(accountId, accountName);
                accounts.put(accountId, account);
            }

            long appId = resultSet.getLong("app_id");
            String appName = resultSet.getString("app_name");
            Application application = account.getApplication(appName);
            if( application == null ) {
                application = new Application();
                application.name = appName;
                application.id = appId;
                account.applications.add(application);
                logger.debug("Created new application: %s(%d)", appName, appId);
            }

            long tierId = resultSet.getLong("tier_id");
            String tierName = resultSet.getString("tier_name");
            Tier tier = application.getTier(tierName);
            if( tier == null ) {
                tier = new Tier();
                tier.name = tierName;
                tier.id = tierId;
                application.tiers.add(tier);
            }

            long nodeId = resultSet.getLong("node_id");
            String nodeName = resultSet.getString("node_name");
            Node node = application.getNode(nodeName);
            if( node == null ) {
                node = new Node();
                node.name = nodeName;
                node.id = nodeId;
                node.tierId = tierId;
                application.nodes.add(node);
            }

        }

    }

    public int getApplicationCount() {
        int count =0;
        for( Account account : accounts.values() )
            for( Application application : account.applications )
                count++;
        return count;
    }

    public Application getApplication( long id ) {
        for( Account account : accounts.values() ) {
            Application application = account.getApplication(id);
            if( application != null ) return application;
        }
        //logger.warn("Application not found for id: %d", id);
        return null;
    }
    public String getApplicationName( long appId ) {
        Application application = getApplication(appId);
        if( application != null ) {
            return application.name;
        }
        logger.warn("Could not find an application name for %d, returning UNKNOWN_APP which is most likely not right", appId);
        return "UNKNOWN_APP";
    }

    public Application getApplication( String name ) {
        for( Account account : accounts.values() ) {
            Application application = account.getApplication(name);
            if( application != null ) return application;
        }
        return null;
    }

    public void addMetricDefinitions(ResultSet resultSet) throws SQLException {
        while( resultSet.next() ) {
            DatabaseMetricDefinition metricDefinition = new DatabaseMetricDefinition();
            metricDefinition.metricId = resultSet.getLong("metric_id");
            metricDefinition.setMetricName( resultSet.getString("metric_name") );
            metricDefinition.appId = resultSet.getLong("application_id");
            metricDefinition.applicationName = resultSet.getString("application_name");
            metricDefinition.timeRollupType = resultSet.getString("time_rollup_type");
            metricDefinition.clusterRollupType = resultSet.getString("cluster_rollup_type");
            Application application = getApplication(metricDefinition.appId);
            if( application == null ) { //this happens when no tiers or nodes are registered for an application, which is a good time to skip a data set
                logger.trace("Application not found for app id %d", metricDefinition.appId);
            } else {
                application.addMetricDefinition(metricDefinition);
            }
        }
    }

    public DatabaseMetricDefinition getMetricDefinition( DatabaseMetricValue databaseMetricValue ) {
        Application application = getApplication(databaseMetricValue.application_id);
        if( application != null )
            return application.getMetricDefinition(databaseMetricValue.metric_id);
        return null;
    }

    public String getApplicationTierName(Long application_id, Long application_component_instance_id) {
        Application application = getApplication(application_id);
        if( application != null ) {
            Tier tier = application.getTier(application_component_instance_id);
            if( tier != null ) return tier.name;
        }
        return "UNKNOWN_TIER";
    }

    public String getApplicationTierNodeName(Long application_id, Long node_id) {
        Application application = getApplication(application_id);
        if( application != null ) {
            Node node = application.getNode(node_id);
            if (node != null) return node.name;
        }
        return "UNKNOWN_NODE";
    }

    public void addServiceEndpoints(ResultSet resultSet) throws SQLException {
        while( resultSet.next() ){
            long applicationId = resultSet.getLong("app_id");
            long tierId = resultSet.getLong("tier_id");
            Application application = getApplication(applicationId);
            if( application != null ) {
                application.serviceEndpoints.add(new ServiceEndpoint(resultSet, application.getTier(tierId)));
            }
        }
    }
}
