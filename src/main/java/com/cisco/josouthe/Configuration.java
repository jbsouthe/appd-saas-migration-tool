package com.cisco.josouthe;

import com.cisco.josouthe.controller.ControllerDatabase;
import com.cisco.josouthe.controller.TargetController;
import com.cisco.josouthe.output.CSVMetricWriter;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import org.apache.commons.digester3.Digester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

public class Configuration {
    private static final Logger logger = LogManager.getFormatterLogger();
    private int daysToRetrieveData = 100;
    private int migrationLevel = 1; //1, 2, or 3 KISS 1=Application level, 2= 1+Tier+BT, 3= 2+Node+Backends+anything else we can find
    private String outputDir = "./csv-data";
    private CSVMetricWriter csvMetricWriter;
    private Properties properties = new Properties();
    private TargetController targetController;
    private ArrayList<ControllerDatabase> sourceControllers = new ArrayList<>();
    private ArrayList<String> applicationFilterList = new ArrayList<>();
    private Map<String,String> targetApplicationNameMap = new HashMap<>();
    private ArrayList<String> metrics = new ArrayList<>();
    private Set<String> applicationsToBuildModelsFrom = new HashSet<>();
    private boolean running=true;

    public String getProperty( String key ) {
        return getProperty(key, (String)null);
    }
    public String getProperty( String key , String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }
    public Boolean getProperty( String key, Boolean defaultBoolean) {
        return Boolean.parseBoolean( getProperty(key, defaultBoolean.toString()));
    }
    public Long getProperty( String key, Long defaultLong ) {
        return Long.parseLong( getProperty(key, defaultLong.toString()));
    }
    public Integer getProperty( String key, Integer defaultInteger ) {
        return Integer.parseInt( getProperty(key, defaultInteger.toString()));
    }

    public Set<String> getApplicationsToBuildModelsFrom() { return applicationsToBuildModelsFrom; }

    public synchronized CSVMetricWriter getCSVMetricWriter() {
        if( csvMetricWriter == null ) {
            this.csvMetricWriter = new CSVMetricWriter(outputDir, targetController);
        }
        return this.csvMetricWriter;
    }

    public int getDaysToRetrieveData() { return daysToRetrieveData; }

    public ControllerDatabase[] getControllerList() { return sourceControllers.toArray(new ControllerDatabase[0]); }
    public TargetController getTargetController() { return targetController; }

    public Configuration( String configFileName) throws Exception {
        logger.info("Processing Config File: %s", configFileName);
        Digester digester = new Digester();
        digester.push(this);
        int paramCounter = 0;

        digester.addCallMethod("Migration/MigrationLevel", "setMigrationLevel", 0);
        digester.addCallMethod("Migration/DaysToRetrieve", "setDaysToRetrieve", 0);
        digester.addCallMethod("Migration/OutputDir", "setOutputDir", 0);
        digester.addCallMethod("Migration/NumberOfDatabaseThreads", "setNumberOfDatabaseThreads", 0);
        digester.addCallMethod("Migration/NumberOfConverterThreads", "setNumberOfConverterThreads", 0);
        digester.addCallMethod("Migration/NumberOfWriterThreads", "setNumberOfWriterThreads", 0);

        //Target controller section, this is where we plan to create insertable data for
        digester.addCallMethod("Migration/TargetController", "addTargetController", 3);
        digester.addCallParam("Migration/TargetController/URL", paramCounter++);
        digester.addCallParam("Migration/TargetController/ClientID", paramCounter++);
        digester.addCallParam("Migration/TargetController/ClientSecret", paramCounter++);

        paramCounter=0;
        digester.addCallMethod("Migration/Source/Controller/Application", "addSourceControllerApplication", 2);
        digester.addCallParam("Migration/Source/Controller/Application/Name", paramCounter++);
        digester.addCallParam("Migration/Source/Controller/Application/NewName", paramCounter++);


        paramCounter=0;
        digester.addCallMethod("Migration/Source/Controller", "addSourceController", 5);
        digester.addCallParam("Migration/Source/Controller", paramCounter++, "getAllDataForAllApplications");
        digester.addCallParam("Migration/Source/Controller/DBConnectionString", paramCounter++);
        digester.addCallParam("Migration/Source/Controller/DBUser", paramCounter++);
        digester.addCallParam("Migration/Source/Controller/DBPassword", paramCounter++);
        digester.addCallParam("Migration/Source/Controller/NumberOfConnections", paramCounter++);

        digester.parse( new File(configFileName) );
        logger.info("Validating Configured Settings");
    }

    public void setMigrationLevel( String levelNumber ) throws InvalidConfigurationException {
        this.migrationLevel = Integer.parseInt(levelNumber);
    }

    public int getMigrationLevel() { return this.migrationLevel; }

    public void setDaysToRetrieve( String numberOfDays ) throws InvalidConfigurationException {
        this.daysToRetrieveData = Integer.parseInt(numberOfDays);
    }

    public void setNumberOfDatabaseThreads( String numberOfThreads ) throws InvalidConfigurationException {
        this.properties.setProperty("scheduler-NumberOfDatabaseThreads", numberOfThreads);
    }

    public void setNumberOfWriterThreads( String numberOfThreads ) throws InvalidConfigurationException {
        this.properties.setProperty("scheduler-NumberOfWriterThreads", numberOfThreads);
    }

    public void setNumberOfConverterThreads( String numberOfThreads ) throws InvalidConfigurationException {
        this.properties.setProperty("scheduler-NumberOfConverterThreads", numberOfThreads);
    }

    public void setOutputDir( String outputDir ) throws InvalidConfigurationException {
        this.outputDir = outputDir;
    }

    public void addTargetController( String url, String clientId, String clientSecret ) throws InvalidConfigurationException {
        this.targetController = new TargetController( url, clientId, clientSecret, false, this);
    }

    public void addSourceControllerApplication( String name, String newName ) throws InvalidConfigurationException {
        this.applicationFilterList.add( name );
        if( newName != null && !"".equals(newName) ) {
            targetApplicationNameMap.put(name, newName);
            applicationsToBuildModelsFrom.add(newName);
        } else {
            applicationsToBuildModelsFrom.add(name);
        }
    }

    public void addSourceController( String getAllDataForAllApplicationsFlag, String connectionString, String dbUser, String dbPassword, String numberOfDatabaseConnections ) throws InvalidConfigurationException {
        if( numberOfDatabaseConnections == null ) {
            int num = (getProperty("scheduler-NumberOfDatabaseThreads", 15) *3) + 1;
            numberOfDatabaseConnections= String.valueOf(num);
        }
        ControllerDatabase controllerDatabase = new ControllerDatabase( connectionString, dbUser, dbPassword,
                applicationFilterList, targetApplicationNameMap, numberOfDatabaseConnections );
        this.applicationFilterList.clear();
        this.targetApplicationNameMap.clear();
        this.sourceControllers.add(controllerDatabase);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean b) { this.running=b; }

    public String getOutputDir() { return outputDir; }
}
