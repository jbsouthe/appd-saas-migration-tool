package com.cisco.josouthe.controller;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.apidata.account.MyAccount;
import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.apidata.model.Application;
import com.cisco.josouthe.controller.apidata.model.Node;
import com.cisco.josouthe.controller.apidata.model.Tier;
import com.cisco.josouthe.exceptions.BadDataException;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;

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
        String metricName = convertMetricNameFromDatabaseFormToControllerAPIForm( databaseMetricName );
        logger.debug("Metric search: appid: %d db metric name '%s' controller metric path '%s'", targetApplicationId, databaseMetricName, metricName);
        Application application = getApplication(targetApplicationId);
        if( application != null ) {
            if( application.isControllerNull() ) application.setController(this);
            MetricData metricData = application.getControllerMetricData(metricName);
            logger.debug("Metric Id on target controller: %s(%d)", metricData.metricName, metricData.metricId);
            if( metricData != null ) return metricData.metricId;
        }
        return null;
    }

    private String convertMetricNameFromDatabaseFormToControllerAPIForm(String databaseMetricName) {
        if( databaseMetricName.startsWith("BTM|Application Summary|") )
            return databaseMetricName.replace("BTM|Application Summary|", "Overall Application Performance|");
        return databaseMetricName;
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
