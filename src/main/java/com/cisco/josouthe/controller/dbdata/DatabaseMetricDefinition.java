package com.cisco.josouthe.controller.dbdata;

import com.cisco.josouthe.util.Parser;

public class DatabaseMetricDefinition {
    public long metricId, appId, tierId, btId;
    public String metricName, applicationName, timeRollupType, clusterRollupType;
    public String tierName, nodeName, btName, seName;

    public void setMetricName( String name ) {
        this.metricName=name;
        Long btIdFromName = Parser.parseBTFromMetricName(name);
        if( btIdFromName != null ) this.btId = btIdFromName;
        Long tierIdFromName = Parser.parseComponentFromMetricName(name);
        if( tierIdFromName != null ) this.tierId = tierIdFromName;
    }

    public String toString() {
        return String.format("metric: %s(%d) app: %s(%d) tier: %s(%d) node: %s",metricName, metricId, applicationName, appId, tierName, tierId, nodeName);
    }
}
