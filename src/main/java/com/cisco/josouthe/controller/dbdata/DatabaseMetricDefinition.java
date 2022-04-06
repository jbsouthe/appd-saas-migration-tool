package com.cisco.josouthe.controller.dbdata;

import com.cisco.josouthe.util.Parser;

public class DatabaseMetricDefinition {
    public Long metricId, appId, tierId, btId, seId;
    public String metricName, applicationName, timeRollupType, clusterRollupType;
    public String tierName, nodeName, btName, seName;

    public void setMetricName( String name ) {
        this.metricName=name;
        Long btIdFromName = Parser.parseBTFromMetricName(name);
        if( btIdFromName != null ) this.btId = btIdFromName;
        Long tierIdFromName = Parser.parseComponentFromMetricName(name);
        if( tierIdFromName != null ) this.tierId = tierIdFromName;
        Long seFromName = Parser.parseSEFromMetricName(name);
        if( seFromName != null ) this.seId = seFromName;
    }

    public String toString() {
        return String.format("metric: %s(%d) app: %s(%d) tier: %s(%d) node: %s",metricName, metricId, applicationName, appId, tierName, tierId, nodeName);
    }
}
