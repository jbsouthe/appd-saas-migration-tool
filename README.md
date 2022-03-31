# How to run this

java -jar SaaSMigrationTool-1.0-20220223.jar test-db.xml

this assumes the ./lib directory contains the jar files found in the package

## that config file looks like this:

    <Migration>
        <MigrationLevel>1</MigrationLevel> <!--only use 1 for now;  1, 2, or 3 KISS 1=Application level, 2= 1+Tier+Node+BT+SE, 3= 2+Custom Data+Backends+anything else we can find -->
        <DaysToRetrieve>90</DaysToRetrieve> <!-- 90 days is enough for a weekly baseline -->
        <NumberOfDatabaseThreads>15</NumberOfDatabaseThreads>
        <NumberOfConverterThreads>30</NumberOfConverterThreads>
        <NumberOfWriterThreads>5</NumberOfWriterThreads>
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
                <Application>
                    <Name>BTPlaybook_Dev</Name>
                    <NewName>BTPlaybook_Dev_New<NewName> <!-- this optional argument changes the name of the app -->
                </Application>
            </Controller>
        </Source>
    </Migration>

Be careful with this configuration. Remember that the target application and metrics must already exist or we will not be able to map them. In case of changing the application name on the target controller, do not try to do anything strange here. Just 1:1 on migration. If you think we can combine two applications into one, this would not be a valid assumption. We will instead generate duplicate blitz data files, and undefined resolution may occur.

## output is in a directory named ./test-data

This generates files needed for import into the saas blitz datastore and packs it into a zip file which contains some helpful run information.

## Proxy Configuration

If proxy support is required, set the following arguments before the -jar arguement:

     -Djava.net.useSystemProxies=true

or, to manually specify the host and port:

     -Dhttp.proxyHost=PROXY_HOST
     -Dhttp.proxyPort=PROXY_PORT

or, to manually specify the host, port, and basic user and password:

     -Dhttp.proxyHost=PROXY_HOST
     -Dhttp.proxyPort=PROXY_PORT
     -Dhttp.proxyUser=USERNAME
     -Dhttp.proxyPassword=PASSWORD

or, to manually specify the host, port, and NTLM authentication:

     -Dhttp.proxyHost=PROXY_HOST
     -Dhttp.proxyPort=PROXY_PORT
     -Dhttp.proxyUser=USERNAME
     -Dhttp.proxyPassword=PASSWORD
     -Dhttp.proxyWorkstation=HOSTNAME
     -Dhttp.proxyDomain=NT_DOMAIN
