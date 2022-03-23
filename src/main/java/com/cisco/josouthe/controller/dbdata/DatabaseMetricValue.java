package com.cisco.josouthe.controller.dbdata;

import com.cisco.josouthe.exceptions.BadDataException;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseMetricValue {
    //Level 1: metricdata_hour_agg_app
    public Long ts_min, metric_id, application_id;
    public String group_count_val, count_val, sum_val, min_val, max_val, cur_val, weight_value_square, weight_value, rollup_type, cluster_rollup_type;
    //Level 2: metricdata_hour_agg
    public Long application_component_instance_id;
    //Level 3: metricdata_hour
    public Long node_id;
    private String type;

    public DatabaseMetricValue(String type, ResultSet resultSet) throws SQLException, BadDataException {
        ts_min = resultSet.getLong("ts_min");
        metric_id = resultSet.getLong("metric_id");
        group_count_val = resultSet.getString("group_count_val");
        count_val = resultSet.getString("count_val");
        sum_val = resultSet.getString("sum_val");
        min_val = resultSet.getString("min_val");
        max_val = resultSet.getString("max_val");
        cur_val = resultSet.getString("cur_val");
        weight_value_square = resultSet.getString("weight_value_square");
        weight_value = resultSet.getString("weight_value");
        rollup_type = resultSet.getString("rollup_type");
        cluster_rollup_type = resultSet.getString("cluster_rollup_type");
        this.type=type;
        switch(type) {
            case "node":
                node_id = resultSet.getLong("node_id");
            case "tier": //if type is tier, then we don't have a node, but fall through because node has a tier as well
                application_component_instance_id = resultSet.getLong("application_component_instance_id");
            case "app": //this is always true, but i moved this here to make it clear the heiarchy of these...
                application_id = resultSet.getLong("application_id");
                break;
            default:
                throw new BadDataException(String.format("This metric type '%s' is not yet supported", type));
        }
    }

    public String getBlitzEntityTypeString() { //com.appdynamics.blitz.shared.op.dto.common.BlitzEntityType
        return type;
    }

}
