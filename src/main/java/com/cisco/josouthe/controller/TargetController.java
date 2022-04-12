package com.cisco.josouthe.controller;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.apidata.account.MyAccount;
import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.apidata.model.*;
import com.cisco.josouthe.controller.dbdata.DatabaseMetricDefinition;
import com.cisco.josouthe.exceptions.BadDataException;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Parser;

public class TargetController extends Controller{
    private long accountId = -1;

    public TargetController(String urlString, String clientId, String clientSecret, boolean getAllDataForAllApplicationsFlag, Configuration configuration) throws InvalidConfigurationException {
        super(urlString, clientId, clientSecret, getAllDataForAllApplicationsFlag, null, configuration);
        logger.info("");
    }


    public long getAccountId() {
        if( accountId == -1 ) {
            try {
                String json = getRequest("controller/api/accounts/myaccount?output=json");
                MyAccount account = gson.fromJson(json, MyAccount.class);
                this.accountId = Long.parseLong(account.id);
                logger.debug("Fetched the account id for '%s' controller in the SaaS: %d", account.name, accountId);
            } catch (ControllerBadStatusException e) {
                logger.warn("Could not get accountId, this means something is wrong with our controller communication, most likley the entire run will fail, returning -1");
            }
        }
        return accountId;
    }

    public Long getEquivolentApplicationId(String applicationName) {
        Application application = getModel().getApplication(applicationName);
        if( application != null ) return application.id;
        logger.warn("Application not found on the target controller: %s Creating :)", applicationName);
        application = createApplication(applicationName);
        getModel().addApplication( application );
        return null;
    }

    public Long getEquivolentMetricId(String blitzEntityTypeString, Long targetApplicationId, DatabaseMetricDefinition databaseMetricDefinition) throws BadDataException {
        Application application = getApplication(targetApplicationId);
        String metricNameOnController = convertMetricNameFromDatabaseToController( blitzEntityTypeString, application, databaseMetricDefinition );
        logger.debug("Metric search: appid: %d db metric name '%s' controller metric path '%s'", targetApplicationId, databaseMetricDefinition.metricName, metricNameOnController);
        if( application != null && metricNameOnController != null ) {
            if( application.isControllerNull() ) application.setController(this);
            MetricMatcher metricMatcher = application.getControllerMetricMatch(blitzEntityTypeString, metricNameOnController, databaseMetricDefinition);
            if( metricMatcher != null ) {
                logger.debug("Metric Id on target controller: %s(%d)", metricMatcher.metricName, metricMatcher.id);
                return metricMatcher.id;
            } else {
                throw new BadDataException(String.format("Metric Data is null for metric matcher: %s", metricMatcher));
            }
        }
        return null;
    }


    public String convertMetricNameFromDatabaseToController(String blitzEntityTypeString, Application application, DatabaseMetricDefinition databaseMetricDefinition) {
        logger.debug("convertMetricNameFromDatabaseToController type: %s application: %s metric: %s",blitzEntityTypeString, application.name, databaseMetricDefinition.metricName);
        String databaseMetricName = databaseMetricDefinition.metricName;
        Long optionalBTId = Parser.parseBTFromMetricName(databaseMetricName);
        Long optionalComponentId = Parser.parseComponentFromMetricName(databaseMetricName);
        Long optionalServiceEndpointId = Parser.parseSEFromMetricName(databaseMetricName);
        logger.debug("parsed from metric: %s BT: %d Component: %d SE: %d", databaseMetricName, optionalBTId, optionalComponentId, optionalServiceEndpointId);
        if (optionalBTId != null) {
            try {
                //Get target BTId from BT name
                BusinessTransaction businessTransaction = application.getBusinessTransaction(databaseMetricDefinition.btName);
                if( businessTransaction != null ) {
                    databaseMetricName = databaseMetricName.replaceAll("\\|BT:\\d+\\|", String.format("|BT:%d|", businessTransaction.id));
                } else {
                    return null;
                }
            }catch (NullPointerException nullPointerException ) {
                logger.warn("Null Pointer Exception in attempts to map BT metric parts to target controller: %s", nullPointerException.toString(), nullPointerException);
            }
        }
        if (optionalComponentId != null) {
            try {
                //get Target Tier Id with this tier name
                Tier tier = application.getTier(databaseMetricDefinition.tierName);
                if( tier != null ) {
                    databaseMetricName = databaseMetricName.replaceAll("\\|Component:\\d+\\|", String.format("|Component:%d|", tier.id));
                } else {
                    return null;
                }
            }catch (NullPointerException nullPointerException ) {
                logger.warn("Null Pointer Exception in attempts to map Component metric parts to target controller: %s", nullPointerException.toString(), nullPointerException);
            }
        }
        if (optionalServiceEndpointId != null ) {
            try {
                ServiceEndpoint targetServiceEndpoint = application.getServiceEndpoint(databaseMetricDefinition.seName, databaseMetricDefinition.tierName);
                if( targetServiceEndpoint != null ) {
                    databaseMetricName = databaseMetricName.replaceAll("\\|SE:\\d+\\|", String.format("|SE:%d|", targetServiceEndpoint.id));
                } else {
                    return null;
                }
            }catch (NullPointerException nullPointerException ) {
                logger.warn("Null Pointer Exception in attempts to map SE metric parts to target controller: %s", nullPointerException.toString(), nullPointerException);
            }
        }
        logger.debug("convertMetricNameFromDatabaseToController type: %s application: %s from: %s to: %s",blitzEntityTypeString, application.name, databaseMetricDefinition.metricName, databaseMetricName);
        return databaseMetricName; //give up, good luck
    }

    public Long getEquivolentTierId(Long targetApplicationId, String applicationTierName) {
        Application application = getApplication(targetApplicationId);
        if( application != null ) {
            Tier tier = application.getTier(applicationTierName);
            if( tier != null ) return tier.id;
        }
        logger.warn("Unable to find target tier id for appId: %d and tier: %s",targetApplicationId, applicationTierName);
        return null;
    }

    public Long getEquivolentNodeId(Long targetApplicationId, String applicationTierNodeName) {
        Application application = getApplication(targetApplicationId);
        if( application != null ) {
            Node node = application.getNode(applicationTierNodeName);
            if( node != null ) return node.id;
        }
        logger.warn("Unable to find target node id for appId: %d and node: %s",targetApplicationId, applicationTierNodeName);
        return null;
    }

    private Application createApplication( String name ) {
        try {
            String json = postRequest("controller/restui/allApplications/createApplication?applicationType=APM", String.format("{ \"name\": \"%s\", \"description\": \" Created by SaaS Migration Tool \"}",name));
            Application application = gson.fromJson(json, Application.class);
            application.setController(this);
            logger.info("Created Application %s(%d) on controller %s",application.name, application.id, this.url);
            return application;
        } catch (ControllerBadStatusException e) {
            logger.warn("Error creating a new application, this was probably a bad idea anyway, tell you what, create it manually and rerun this. Go make %s in the target controller %s", name, this.url);
        }
        return null;
    }
}
