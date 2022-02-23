#How to run this

java -jar SaaSMigrationTool-1.0-20220223.jar test-db.xml

this assumes the ./lib directory contains the jar files found in the package

##that config file looks like this:

    <Migration>
        <MigrationLevel>1</MigrationLevel> <!--only use 1 for now;  1, 2, or 3 KISS 1=Application level, 2= 1+Tier+BT, 3= 2+Node+Backends+anything else we can find -->
        <DaysToRetrieve>90</DaysToRetrieve> <!-- 90 days is enough for a weekly baseline -->
        <OutputDir>./test-data</OutputDir> <!-- this will be created if it does not exist -->
        <TargetController>
            <URL>https://southerland-test.saas.appdynamics.com</URL>
            <ClientID>saasMigration@southerland-test</ClientID> <!-- give this account owner and administrator roles -->
            <ClientSecret>XXXXX</ClientSecret>
        </TargetController>
        <Source>
            <Controller getAllDataForAllApplications="false" >
                <DBConnectionString>jdbc:mysql://localhost:3388/controller</DBConnectionString> <!-- point to the controller db -->
                <DBUser>root</DBUser>
                <DBPassword>appd</DBPassword>
                <Application>
                    <Name>BTPlaybook_Lab</Name>
                </Application>
            </Controller>
        </Source>
    </Migration>

##output is in a directory named ./test-data

This generates files needed for import into the saas blitz datastore
