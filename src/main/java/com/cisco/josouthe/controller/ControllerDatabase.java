package com.cisco.josouthe.controller;


import com.cisco.josouthe.controller.dbdata.DatabaseMetricValue;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.controller.dbdata.SourceModel;
import com.cisco.josouthe.exceptions.BadDataException;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.TimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ControllerDatabase {
    private static final Logger logger = LogManager.getFormatterLogger();
    private static final int MAX_RETRY_LIMIT = 3;
    private String connectionString, user, pass;
    private SourceModel cached_model = null;
    private static final String sqlSelectAllIds = "select acc.id acc_id, acc.name acc_name, app.id app_id, app.name app_name, tier.id tier_id, tier.name tier_name, node.id node_id, node.name node_name from account acc, application app, application_component tier, application_component_node node where acc.id = app.account_id and app.id = tier.application_id and tier.id = node.application_component_id ";
    private static final String sqlSelectMetricDefinitions = "select m.id as metric_id, m.name as metric_name, m.application_id as application_id, app.name as application_name, m.time_rollup_type as time_rollup_type, m.cluster_rollup_type as cluster_rollup_type from metric as m, application as app where app.id = m.application_id and m.name not like \"%To:%\" and m.name not like \"%Th:%\" ";
    private static final String sqlSelectMetricData_hour_agg_app = "select ts_min, metric_id, application_id, group_count_val, count_val, sum_val, min_val, max_val, cur_val, weight_value_square, weight_value from metricdata_hour_agg_app where ts_min < %d ";
    private static final String sqlSelectMetricData_hour_agg = "select ts_min, metric_id, application_id, group_count_val, count_val, sum_val, min_val, max_val, cur_val, weight_value_square, weight_value, application_component_instance_id from metricdata_hour_agg where ts_min < %d ";
    private static final String sqlSelectMetricData_hour = "select ts_min, metric_id, application_id, group_count_val, count_val, sum_val, min_val, max_val, cur_val, weight_value_square, weight_value, application_component_instance_id, node_id from metricdata_hour where ts_min < %d ";
    private static final String sqlWhereClauseLevelOne_ApplicationSummaryMetrics = " metric_id in (select id from metric where name like \"%BTM|Application Summary|%\" and name not like \"%|Component:%\") ";
    private String[] applicationsFilter;

    public ControllerDatabase(String connectionString, String dbUser, String dbPassword, List<String> applicationsFilterList) {
        this.connectionString=connectionString;
        this.user=dbUser;
        this.pass=dbPassword;
        if( applicationsFilterList != null && applicationsFilterList.size() > 0)
            this.applicationsFilter=applicationsFilterList.toArray(new String[0]);
        getModel();
        logger.info("Initialized Controller Database Connection. applications: %d", getModel().getApplicationCount() );
    }

    private boolean applyApplicationFilters() { return applicationsFilter != null; }

    private Connection getConnection() {
        int tries=0; boolean succeeded=false;
        Connection connection = null;
        while( !succeeded && tries < MAX_RETRY_LIMIT) {
            try {
                tries++;
                connection = DriverManager.getConnection(this.connectionString, this.user, this.pass);
                succeeded = true;
            } catch (SQLException e) {
                logger.warn("Error trying to connect to database, attempt %d of %d, message: %s", tries, MAX_RETRY_LIMIT, e.toString());
            }
        }
        return connection;
    }

    public SourceModel getModel() {
        if( cached_model == null ) {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlSelectAllIds); ) {
                logger.debug("Running Query: %s",sqlSelectAllIds);
                try (ResultSet resultSet = statement.executeQuery()) {
                    cached_model = new SourceModel(resultSet);
                    getMetricDefinitions(cached_model);
                }
            } catch (SQLException exception) {
                logger.warn("Exception building controller model from database: %s", exception.toString());
            }
        }
        return cached_model;
    }

    private void getMetricDefinitions( SourceModel sourceModel ) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlSelectMetricDefinitions); ) {
            logger.debug("Running Query: %s", sqlSelectMetricDefinitions);
            try (ResultSet resultSet = statement.executeQuery()) {
                sourceModel.addMetricDefinitions(resultSet);
            }
        } catch (SQLException exception) {
            logger.warn("Exception building controller model from database: %s", exception.toString());
        }
    }

    public MetricValueCollection getAllMetrics( String type, int level, int days ) throws InvalidConfigurationException {
        List<DatabaseMetricValue> metrics = new ArrayList<>();
        String sqlQuery = "";
        long startTimestampMin = TimeUtil.getDaysBackTimestamp(days)/60000;
        switch(type) {
            case "app": {
                sqlQuery = String.format(sqlSelectMetricData_hour_agg_app, startTimestampMin);
                break;
            }
            case "tier": {
                sqlQuery = String.format(sqlSelectMetricData_hour_agg, startTimestampMin);
                break;
            }
            case "node": {
                sqlQuery = String.format(sqlSelectMetricData_hour, startTimestampMin);
                break;
            }
            default: throw new InvalidConfigurationException(String.format("Can not run metric query for type %s",type));
        }

        if( level == 1 ) { //only get application level metrics without tiers
            sqlQuery += " and " + sqlWhereClauseLevelOne_ApplicationSummaryMetrics;
        }

        if( applyApplicationFilters() ) {
            StringBuilder sb = new StringBuilder(sqlQuery);
            sb.append(" and application_id in ( ");
            for( int i=0; i< this.applicationsFilter.length; i++ ) {
                sb.append( getModel().getApplication( this.applicationsFilter[i] ).id );
                if( i+1 < this.applicationsFilter.length ) sb.append(", ");
            }
            sb.append(") ");
            sqlQuery = sb.toString();
        }
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery); ) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    try {
                        metrics.add(new DatabaseMetricValue(type, resultSet));
                    } catch (BadDataException e) {
                        logger.warn("Error building data metric from row, message: %s", resultSet.getWarnings().getMessage());
                    }
                }
                return new MetricValueCollection( getModel(), metrics, days);
            }
        } catch (SQLException exception) {
            logger.warn("Exception collecting hourly metrics from database: %s", exception.toString());
        }
        return null;
    }
}
