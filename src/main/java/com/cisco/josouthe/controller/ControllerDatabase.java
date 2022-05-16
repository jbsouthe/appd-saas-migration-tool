package com.cisco.josouthe.controller;


import com.cisco.josouthe.controller.dbdata.DatabaseMetricValue;
import com.cisco.josouthe.controller.dbdata.MetricValueCollection;
import com.cisco.josouthe.controller.dbdata.SourceModel;
import com.cisco.josouthe.exceptions.BadDataException;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.TimeUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerDatabase {
    private static final Logger logger = LogManager.getFormatterLogger();
    private static final int MAX_RETRY_LIMIT = 5;
    private static final String TABLE_APP_TEN_MIN = "metricdata_ten_min_agg_app";
    private static final String TABLE_APP_ONE_HOUR = "metricdata_hour_agg_app";
    private String connectionString, user, pass;
    private SourceModel cached_model = null;
    private static final String sqlSelectAllIds = "select acc.id acc_id, acc.name acc_name, app.id app_id, app.name app_name, tier.id tier_id, tier.name tier_name, node.id node_id, node.name node_name from account acc, application app, application_component tier, application_component_node node where acc.id = app.account_id and app.id = tier.application_id and tier.id = node.application_component_id ";
    private static final String sqlSelectMetricDefinitions = "select m.id as metric_id, m.name as metric_name, m.application_id as application_id, app.name as application_name, m.time_rollup_type as time_rollup_type, m.cluster_rollup_type as cluster_rollup_type from metric as m, application as app where app.id = m.application_id and m.name not like \"%To:%\" and m.name not like \"%Th:%\" ";
    private static final String sqlSelectMetricData_hour_agg_app = "select ts_min, metric_id, application_id, group_count_val, count_val, sum_val, min_val, max_val, cur_val, weight_value_square, weight_value, rollup_type, cluster_rollup_type  from metricdata_hour_agg_app where ts_min > %d and ts_min < %d ";
    private static final String sqlSelectMetricData_hour_agg = "select ts_min, metric_id, application_id, group_count_val, count_val, sum_val, min_val, max_val, cur_val, weight_value_square, weight_value, application_component_instance_id, rollup_type, cluster_rollup_type from metricdata_hour_agg where ts_min > %d and ts_min < %d ";
    private static final String sqlSelectMetricData_hour = "select ts_min, metric_id, application_id, 1 as group_count_val, count_val, sum_val, min_val, max_val, cur_val, weight_value_square, weight_value, application_component_instance_id, node_id, rollup_type, cluster_rollup_type from metricdata_hour where ts_min > %d and ts_min < %d ";
    private static final String sqlWhereClauseLevelOne_ApplicationSummaryMetrics = " metric_id in (select id from metric where name like \"%BTM|Application Summary|%\" and name not like \"%|Component:%\") ";
    private static final String sqlSelectAllServiceEndpoints = "select se.id as se_id, se.name as se_name, se.entry_point_type as se_type, se.entity_type as se_entity_type, tier.id as tier_id, tier.name as tier_name, app.id as app_id, app.name as app_name from service_endpoint_definition se, application_component tier, application app where tier.id = se.entity_id and app.id = tier.application_id ";
    private String[] applicationsFilter;
    private Map<String,String> applicationRenameMap;
    private HikariConfig hikariConfig;
    private HikariDataSource dataSource;

    public ControllerDatabase(String connectionString, String dbUser, String dbPassword, List<String> applicationsFilterList, Map<String, String> targetApplicationRenameMap, String numberOfDatabaseConnections) {
        this.connectionString=connectionString;
        this.user=dbUser;
        this.pass=dbPassword;
        if( applicationsFilterList != null && applicationsFilterList.size() > 0)
            this.applicationsFilter=applicationsFilterList.toArray(new String[0]);
        applicationRenameMap = new HashMap<>();
        for( String appName : targetApplicationRenameMap.keySet() ) {
            applicationRenameMap.put(appName, targetApplicationRenameMap.get(appName));
            logger.info("As part of Migration we will attempt to rename data from on premise application '%s' to SaaS Application '%s' on the target controller, " +
                    "please be careful with this. This is not to be used as a merge of two applications into a single SaaS application, or any other crazy idea. That will not end well."
                    , appName, applicationRenameMap.get(appName)
            );
        }
        this.hikariConfig = new HikariConfig();
        this.hikariConfig.setJdbcUrl(connectionString);
        this.hikariConfig.setUsername(user);
        this.hikariConfig.setPassword(pass);
        this.hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        this.hikariConfig.addDataSourceProperty("maximumPoolSize", numberOfDatabaseConnections);
        this.hikariConfig.addDataSourceProperty("connectionTimeout", "60000");
        this.hikariConfig.addDataSourceProperty("leakDetectionThreshold", "35000");
        this.hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        this.dataSource = new HikariDataSource(this.hikariConfig);
        getModel();
        logger.info("Initialized Controller Database Connection. applications: %d maximumPoolSize: %s maxRetryLimit: %d", getModel().getApplicationCount(), numberOfDatabaseConnections, MAX_RETRY_LIMIT );
    }

    public String toString() { return connectionString; }

    private boolean applyApplicationFilters() { return applicationsFilter != null; }
    public String[] getApplicationsFilters() { return applicationsFilter; }

    private Connection getConnection() {
        int tries=0; boolean succeeded=false;
        Connection connection = null;
        while( !succeeded && tries < MAX_RETRY_LIMIT) {
            try {
                tries++;
                connection = this.dataSource.getConnection();
                succeeded = true;
            } catch (SQLException e) {
                Level level = Level.WARN;
                if( tries == MAX_RETRY_LIMIT ) level = Level.ERROR;
                logger.log(level, "Error trying to connect to database, attempt %d of %d, message: %s", tries, MAX_RETRY_LIMIT, e.toString());
            }
        }
        if( connection == null ) {
            //just try to get a fallback connection, non connection pooled
            try {
                connection = DriverManager.getConnection(connectionString, user, pass);
                logger.warn("Finally got a connection, using the system driver, whew");
            } catch (SQLException e) {
                logger.error("Can't connect to this database, even with the system driver, FUBAR: %s", e.toString(), e);
            }
        }
        return connection;
    }

    public SourceModel getModel() {
        if( cached_model == null ) {
            logger.debug("Source Model is not yet cached, building it now");
            logger.trace("get connection and prepare statement for sqlSelectAllIds");
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlSelectAllIds); ) {
                logger.debug("Running Query: %s",sqlSelectAllIds);
                try (ResultSet resultSet = statement.executeQuery()) {
                    cached_model = new SourceModel(resultSet);
                    getMetricDefinitions(cached_model);
                }
                logger.trace("finished sqlSelectAllIds");
            } catch (SQLException exception) {
                logger.warn("Exception building controller model from database: %s", exception.toString());
            }
            logger.trace("get connection and prepare statement for sqlSelectAllServiceEndpoints");
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlSelectAllServiceEndpoints); ) {
                logger.debug("Running Query: %s",sqlSelectAllServiceEndpoints);
                try (ResultSet resultSet = statement.executeQuery()) {
                    cached_model.addServiceEndpoints(resultSet);
                }
                logger.trace("finished sqlSelectAllServiceEndpoints");
            } catch (SQLException exception) {
                logger.warn("Exception building controller model from database: %s", exception.toString());
            }
            cached_model.setApplicationRenameMap( applicationRenameMap );
        }
        return cached_model;
    }

    private void getMetricDefinitions( SourceModel sourceModel ) {
        logger.trace("get connection and prepare statement for sqlSelectMetricDefinitions");
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlSelectMetricDefinitions); ) {
            logger.debug("Running Query: %s", sqlSelectMetricDefinitions);
            try (ResultSet resultSet = statement.executeQuery()) {
                sourceModel.addMetricDefinitions(resultSet);
            }
            logger.trace("finished sqlSelectMetricDefinitions");
        } catch (SQLException exception) {
            logger.warn("Exception building controller model from database: %s", exception.toString());
        }
    }

    public MetricValueCollection getAllMetrics( String type, int level, int days ) throws InvalidConfigurationException {
        return getAllMetrics(type,level, TimeUtil.now(), TimeUtil.getDaysBackTimestamp(days) );
    }

    public MetricValueCollection getAllMetrics( String type, int level, long startTimestamp, long endTimestamp ) throws InvalidConfigurationException {
        List<DatabaseMetricValue> metrics = new ArrayList<>();
        String sqlQuery = "";
        long startTimestampMin = startTimestamp/60000;
        long endTimestampMin = endTimestamp/60000;
        switch(type) {
            case "app": {
                sqlQuery = String.format(sqlSelectMetricData_hour_agg_app, endTimestampMin, startTimestampMin);
                break;
            }
            case "tier": {
                sqlQuery = String.format(sqlSelectMetricData_hour_agg, endTimestampMin, startTimestampMin);
                break;
            }
            case "node": {
                sqlQuery = String.format(sqlSelectMetricData_hour, endTimestampMin, startTimestampMin);
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
            sb.append(" ) ");
            sqlQuery = sb.toString();
        }
        logger.trace("get connection and prepare statement for sqlSelectMetricData<Something>");
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
                logger.trace("finished sqlSelectMetricData<Something>");
                return new MetricValueCollection( getModel(), metrics);
            }
        } catch (SQLException exception) {
            logger.warn("SQL: '%s'",sqlQuery);
            logger.warn("Exception collecting hourly metrics from database: %s", exception.toString());
        }
        logger.debug("about to return null on getAllMetrics");
        return null;
    }
}
