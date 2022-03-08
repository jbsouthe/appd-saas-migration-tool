package com.cisco.josouthe.controller;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.apidata.account.MyAccount;
import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.apidata.model.*;
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

    public Long getEquivolentMetricId(Long targetApplicationId, String databaseMetricName) throws BadDataException {
        Application application = getApplication(targetApplicationId);
        String metricPath = getControllerMetricPathFromDatabaseMetricName( application, databaseMetricName );
        logger.debug("Metric search: appid: %d db metric name '%s' controller metric path '%s'", targetApplicationId, databaseMetricName, metricPath);
        if( application != null ) {
            if( application.isControllerNull() ) application.setController(this);
            MetricData metricData = application.getControllerMetricData(metricPath);
            if( metricData != null ) {
                logger.debug("Metric Id on target controller: %s(%d)", metricData.metricName, metricData.metricId);
            } else {
                throw new BadDataException(String.format("Metric Data is null for metric path: %s", metricPath));
            }
            if( metricData != null ) return metricData.metricId;
        }
        return null;
    }


    public String getControllerMetricPathFromDatabaseMetricName(Application application, String databaseMetricName) {
        Long optionalBTId = Parser.parseBTFromMetricName(databaseMetricName);
        Long optionalComponentId = Parser.parseComponentFromMetricName(databaseMetricName);
        Long optionalServiceEndpointId = Parser.parseSEFromMetricName(databaseMetricName);
        try {
            if (optionalBTId != null) {
                //Get BT name with this btId
                String name = getModel().getApplication(application.name).getBusinessTransaction(optionalBTId).name;
                //Get target BTId from BT name
                BusinessTransaction businessTransaction = application.getBusinessTransaction(name);
                databaseMetricName = databaseMetricName.replaceAll("|BT:\\d+|", String.format("|BT:%d|", businessTransaction.id));
            }
            if (optionalComponentId != null) {
                //Get Tier name with this component id
                String name = getModel().getApplication(application.name).getTier(optionalComponentId).name;
                //get Target Tier Id with this tier name
                Tier tier = application.getTier(name);
                databaseMetricName = databaseMetricName.replaceAll("|Component:\\d+|", String.format("|Component:%d|", tier.id));
            }
            if (optionalServiceEndpointId != null) {
                ServiceEndpoint serviceEndpoint = getModel().getApplication(application.name).getServiceEndpoint(optionalServiceEndpointId);
                ServiceEndpoint targetServiceEndpoint = application.getServiceEndpoint(serviceEndpoint);
                databaseMetricName = databaseMetricName.replaceAll("|SE:\\d+|", String.format("|SE:%d|", targetServiceEndpoint.id));
            }
        }catch (NullPointerException nullPointerException ) {
            logger.warn("Null Pointer Exception in attempts to map optional metric parts to target controller: %s, cause: %s",nullPointerException.toString(),nullPointerException.getCause().toString());
        }
        MetricData metricData = application.getControllerMetricData(databaseMetricName);
        if( metricData != null ) return metricData.metricPath;
        return databaseMetricName; //give up, good luck
    }

    public Long getEquivolentTierId(Long targetApplicationId, String applicationTierName) {
        Application application = getApplication(targetApplicationId);
        if( application != null ) {
            Tier tier = application.getTier(applicationTierName);
            if( tier != null ) return tier.id;
        }
        return null;
    }

    public Long getEquivolentNodeId(Long targetApplicationId, String applicationTierNodeName) {
        Application application = getApplication(targetApplicationId);
        if( application != null ) {
            Node node = application.getNode(applicationTierNodeName);
            if( node != null ) return node.id;
        }
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
