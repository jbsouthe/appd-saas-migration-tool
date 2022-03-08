package com.cisco.josouthe.controller;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.controller.apidata.auth.AccessToken;
import com.cisco.josouthe.controller.apidata.metric.MetricData;
import com.cisco.josouthe.controller.apidata.model.*;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.HttpClientFactory;
import com.cisco.josouthe.util.Parser;
import com.cisco.josouthe.util.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.codec.Charsets;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Controller {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public String hostname;
    public URL url;
    private String clientId, clientSecret;
    private AccessToken accessToken = null;
    public Application[] applications = null;
    public Model controllerModel = null;
    protected Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private HttpClient client = null;
    protected Configuration configuration;
    protected final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
        private String uri = "Unset";
        public void setUri( String uri ) { this.uri=uri; }

        @Override
        public String handleResponse( final HttpResponse response) throws IOException {
            final int status = response.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_TEMPORARY_REDIRECT) {
                final HttpEntity entity = response.getEntity();
                try {
                    return entity != null ? EntityUtils.toString(entity) : null;
                } catch (final ParseException ex) {
                    throw new ClientProtocolException(ex);
                }
            } else {
                throw new ControllerBadStatusException(response.getStatusLine().toString(), EntityUtils.toString(response.getEntity()), uri);
            }
        }

    };

    public Controller( String urlString, String clientId, String clientSecret, boolean getAllDataForAllApplicationsFlag, Application[] applications, Configuration configuration ) throws InvalidConfigurationException {
        if( !urlString.endsWith("/") ) urlString+="/"; //this simplifies some stuff downstream
        try {
            this.url = new URL(urlString);
        } catch (Exception exception) {
            throw new InvalidConfigurationException(String.format("Bad url in configuration for a controller: %s exception: %s", urlString, exception.toString()));
        }
        this.hostname = this.url.getHost();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.applications = applications;
        this.client = HttpClientFactory.getHttpClient();
        this.configuration = configuration;
        getModel(); //initialize model
    }

    public Configuration getConfiguration() { return configuration; }

    public Application getApplication( long id ) {
        for( Application application : getModel().getApplications() )
            if( application.id == id ) return application;
        return null;
    }

    public String getBearerToken() {
        if( isAccessTokenExpired() && !refreshAccessToken()) return null;
        return "Bearer "+ accessToken.access_token;
    }

    private boolean isAccessTokenExpired() {
        long now = new Date().getTime();
        if( accessToken == null || accessToken.expires_at < now ) return true;
        return false;
    }

    private boolean refreshAccessToken() { //returns true on successful refresh, false if an error occurs
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);
        logger.trace("credentials configured: %s",credentials.toString());
        provider.setCredentials(AuthScope.ANY, credentials);
        logger.trace("provider configured: %s",provider.toString());
        HttpPost request = new HttpPost(url.toString()+"/controller/api/oauth/access_token");
        //request.addHeader(HttpHeaders.CONTENT_TYPE,"application/vnd.appd.cntrl+protobuf;v=1");
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add( new BasicNameValuePair("grant_type","client_credentials"));
        postParameters.add( new BasicNameValuePair("client_id",clientId));
        postParameters.add( new BasicNameValuePair("client_secret",clientSecret));
        try {
            request.setEntity(new UrlEncodedFormEntity(postParameters,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding Exception in post parameter encoding: %s",e.getMessage());
        }

        if( logger.isTraceEnabled()){
            logger.trace("Request to run: %s",request.toString());
            for( Header header : request.getAllHeaders())
                logger.trace("with header: %s",header.toString());
        }

        HttpResponse response = null;
        int tries=0;
        boolean succeeded=false;
        while( !succeeded && tries < 3 ) {
            try {
                response = client.execute(request);
                succeeded=true;
                logger.trace("Response Status Line: %s", response.getStatusLine());
            } catch (IOException e) {
                logger.error("Exception in attempting to get access token, Exception: %s", e.getMessage());
                tries++;
            } catch (IllegalStateException illegalStateException) {
                tries++;
                this.client = HttpClientFactory.getHttpClient(true);
                logger.warn("Caught exception on connection, building a new connection for retry, Exception: %s", illegalStateException.getMessage());
            }
        }
        if( !succeeded ) return false;
        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());
        String json = null;
        try {
            json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            logger.trace("JSON returned: %s",json);
        } catch (IOException e) {
            logger.warn("IOException parsing returned encoded string to json text: "+ e.getMessage());
            return false;
        }
        if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            logger.warn("Access Key retreival returned bad status: %s message: %s", response.getStatusLine(), json);
            return false;
        }
        this.accessToken = gson.fromJson(json, AccessToken.class); //if this doesn't work consider creating a custom instance creator
        this.accessToken.expires_at = new Date().getTime() + (accessToken.expires_in*1000); //hoping this is enough, worry is the time difference
        return true;
    }

    public MetricData[] getMetricValue(long applicationId, String metricPath, boolean rollup ) {
        MetricData[] metrics = null;

        int tries=0;
        boolean succeeded = false;
        while (! succeeded && tries < 3 ) {
            try {
                metrics = getMetricValue(String.format("%scontroller/rest/applications/%d/metric-data?metric-path=%s&time-range-type=BEFORE_NOW&duration-in-mins=60&output=JSON&rollup=%s",
                        this.url, applicationId, Parser.encode(metricPath), rollup)
                );
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                tries++;
                logger.warn("Attempt number %d failed, status returned: %s for request %s",tries,controllerBadStatusException.getMessage(), controllerBadStatusException.urlRequestString);
            }
        }
        if( !succeeded)
            logger.warn("Gave up after %d tries, not getting %s back", tries, metricPath);
        return metrics;
    }

    public MetricData[] getMetricValue( String urlString ) throws ControllerBadStatusException {
        MetricData[] metricData = null;
        if( urlString == null ) return null;
        logger.trace("metric url: %s",urlString);
        if( ! urlString.contains("output=JSON") ) urlString += "&output=JSON";
        HttpGet request = new HttpGet(urlString);
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.trace("HTTP Method: %s",request);
        String json = null;
        try {
            json = client.execute(request, this.responseHandler);
            logger.trace("Response JSON: '%s'",json);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            controllerBadStatusException.setURL(urlString);
            throw controllerBadStatusException;
        } catch (IOException e) {
            logger.error("Exception in attempting to get url, Exception: %s", e.getMessage());
            return null;
        }
        metricData = gson.fromJson(json, MetricData[].class);
        return metricData;
    }

    public TreeNode[] getApplicationMetricFolders(Application application, String path) {
        String json = null;

        int tries=0;
        boolean succeeded=false;
        while( !succeeded && tries < 3 ) {
            try {
                if ("".equals(path)) {
                    json = getRequest(String.format("controller/rest/applications/%s/metrics?output=JSON", Parser.encode(application.name)));
                } else {
                    json = getRequest(String.format("controller/rest/applications/%s/metrics?metric-path=%s&output=JSON", Parser.encode(application.name), Parser.encode(path)));
                }
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                tries++;
                logger.warn("Try %d failed for request to get app application metric folders for %s with error: %s",tries,application.name,controllerBadStatusException.getMessage());
            }
        }
        if(!succeeded) logger.warn("Failing on get of application metric folder, controller may be down");

        TreeNode[] treeNodes = null;
        try {
            treeNodes = gson.fromJson(json, TreeNode[].class);
        } catch (JsonSyntaxException jsonSyntaxException) {
            logger.warn("Error in parsing returned text, this may be a bug JSON '%s' Exception: %s",json, jsonSyntaxException.getMessage());
        }
        return treeNodes;
    }

    public MetricData[] getAllMetricsForAllApplications() {
        ArrayList<MetricData> metrics = new ArrayList<>();
        for( Application application : this.applications ) {
            metrics.addAll(getAllMetrics(application, null));
        }
        return metrics.toArray( new MetricData[0] );
    }

    public ArrayList<MetricData> getAllMetrics( Application application, LinkedBlockingQueue<Object[]> dataQueue ) {
        ArrayList<MetricData> metrics = new ArrayList<>();
        long startTimestamp = TimeUtil.getDaysBackTimestamp( configuration.getDaysToRetrieveData() );
        long endTimestamp = TimeUtil.now();
        for( String applicationMetricName : application.getMetricNames() ) {
            for( MetricData metricData : getMetricValue( application.id, applicationMetricName, false )) {
                if( "METRIC DATA NOT FOUND".equals(metricData.metricName) ) continue;
                metricData.controller = this;
                metricData.application = application;
                metrics.add(metricData);
                //getBaselineValue( metricData, application, startTimestamp, endTimestamp, dataQueue);
            }
            if( dataQueue != null ) {
                dataQueue.add(metrics.toArray(new MetricData[0]));
                metrics.clear();
            }
        }
        return metrics;
    }

    protected String postRequest( String requestUri, String body ) throws ControllerBadStatusException {
        HttpPost request = new HttpPost(String.format("%s%s", this.url.toString(), requestUri));
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.trace("HTTP Method: %s with body: '%s'",request, body);
        String json = null;
        try {
            request.setEntity( new StringEntity(body));
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-Type", "application/json");
            json = client.execute( request, this.responseHandler);
            logger.trace("Data Returned: '%s'", json);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            controllerBadStatusException.setURL(request.getURI().toString());
            throw controllerBadStatusException;
        } catch (IOException e) {
            logger.warn("Exception: %s",e.getMessage());
        }
        return json;
    }

    protected String getRequest( String formatOrURI, Object... args ) throws ControllerBadStatusException {
        if( args == null || args.length == 0 ) return getRequest(formatOrURI);
        return getRequest( String.format(formatOrURI,args));
    }

    protected String getRequest( String uri ) throws ControllerBadStatusException {
        HttpGet request = new HttpGet(String.format("%s%s", this.url.toString(), uri));
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.trace("HTTP Method: %s",request);
        String json = null;
        try {
            json = client.execute(request, this.responseHandler);
            logger.trace("Data Returned: '%s'",json);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            controllerBadStatusException.setURL(request.getURI().toString());
            throw controllerBadStatusException;
        } catch (IOException e) {
            logger.warn("Exception: %s",e.getMessage());
        }
        return json;
    }

    Map<String,Long> _applicationIdMap = null;
    public long getApplicationId( String name ) {
        logger.trace("Get Application id for %s",name);
        if( _applicationIdMap == null ) { //go get em
            try {
                String json = getRequest("controller/restui/applicationManagerUiBean/getApplicationsAllTypes");
                com.cisco.josouthe.controller.apidata.model.ApplicationListing applicationListing = gson.fromJson(json, com.cisco.josouthe.controller.apidata.model.ApplicationListing.class);
                _applicationIdMap = new HashMap<>();
                for (com.cisco.josouthe.controller.apidata.model.Application app : applicationListing.getApplications() )
                    if( app.active ) _applicationIdMap.put(app.name, app.id);
            } catch (ControllerBadStatusException controllerBadStatusException) {
                logger.warn("Giving up on getting application id, not even going to retry");
            }
        }
        if( !_applicationIdMap.containsKey(name) ) return -1;
        return _applicationIdMap.get(name);
    }

    public List<ServiceEndpoint> getServiceEndpoints( long applicationId ) throws ControllerBadStatusException {
        long timestamp = TimeUtil.now();
        return getServiceEndpoints(applicationId, timestamp-60000, timestamp);
    }

    public List<ServiceEndpoint> getServiceEndpoints( long applicationId, long startTimestamp, long endTimestamp ) throws ControllerBadStatusException {
        String postBody = String.format("{\"requestFilter\":{\"queryParams\":{\"applicationId\":%d,\"mode\":\"FILTER_EXCLUDED\"},\n" +
                "    \"searchText\":\"\",\n" +
                "    \"filters\":{\"sepPerfData\":{\"responseTime\":0,\"callsPerMinute\":0},\"type\":[],\"sepName\":[]}},\n" +
                "    \"columnSorts\":[{\"column\":\"NAME\",\"direction\":\"ASC\"}],\n" +
                "    \"timeRangeStart\":%d,\n" +
                "    \"timeRangeEnd\":%d}", applicationId, startTimestamp, endTimestamp);
        String json = postRequest("controller/restui/serviceEndpoint/list", postBody);
        ServiceEndpointResponse serviceEndpointResponse = gson.fromJson(json, ServiceEndpointResponse.class);
        if( serviceEndpointResponse != null ) return serviceEndpointResponse.data;
        return null;
    }

    public Model getModel() {
        if( this.controllerModel == null ) {
            try {
                String json = getRequest("controller/restui/applicationManagerUiBean/getApplicationsAllTypes");
                logger.trace("getApplicationsAllTypes returned: %s",json);
                this.controllerModel = new Model(gson.fromJson(json, ApplicationListing.class));
                for (Application application : this.controllerModel.getAPMApplications()) {
                    application.setController(this);
                    json = getRequest("controller/rest/applications/%d/tiers?output=JSON", application.id);
                    for( Tier tier : gson.fromJson(json, Tier[].class) )
                        application.tiers.add(tier);
                    json = getRequest("controller/rest/applications/%d/nodes?output=JSON", application.id);
                    for( Node node : gson.fromJson(json, Node[].class))
                        application.nodes.add(node);
                    json = getRequest("controller/rest/applications/%d/business-transactions?output=JSON", application.id);
                    for( BusinessTransaction businessTransaction : gson.fromJson(json, BusinessTransaction[].class))
                        application.businessTransactions.add(businessTransaction);
                    json = getRequest("controller/rest/applications/%d/backends?output=JSON", application.id);
                    for( Backend backend : gson.fromJson(json, Backend[].class))
                        application.backends.add(backend);
                    application.serviceEndpoints.addAll( getServiceEndpoints(application.id));
                }
            } catch (ControllerBadStatusException controllerBadStatusException) {
                logger.warn("Giving up on getting controller model, not even going to retry");
            }
        }
        return this.controllerModel;
    }



    public static void main( String... args ) throws Exception {
        Controller controller;
        if( args.length == 0 ) {
            controller = new Controller("https://southerland-test.saas.appdynamics.com/", "ETLClient@southerland-test", "869b6e71-230c-4e6f-918d-6713fb73b3ad", true, null, null);
        } else {
            controller = new Controller( args[0], args[1], args[2], true, null, null );
        }
        /*
        System.out.printf("%s Test 1: %s\n", Controller.class, controller.getBearerToken());
        MetricData[] metricData = controller.getMetricValue("https://southerland-test.saas.appdynamics.com/controller/rest/applications/Agent%20Proxy/metric-data?metric-path=Application%20Infrastructure%20Performance%7C*%7CJVM%7CProcess%20CPU%20Usage%20%25&time-range-type=BEFORE_NOW&duration-in-mins=60");
        System.out.printf("%s Test 2: %d elements\n", Controller.class, metricData.length);
        metricData = controller.getMetricValue("https://southerland-test.saas.appdynamics.com/controller/rest/applications/Agent%20Proxy/metric-data?metric-path=Business%20Transaction%20Performance%7CBusiness%20Transactions%7C*%7C*%7CAverage%20Response%20Time%20%28ms%29&time-range-type=BEFORE_NOW&duration-in-mins=60");
        System.out.printf("%s Test 3: %d elements\n", Controller.class, metricData.length);

        Search[] searches = controller.getAllSavedSearchesFromController();
        for( Search search : searches ) {
            System.out.println(String.format("<Search name=\"%s\" visualization=\"%s\" >%s</Search>",search.getName(), search.visualization, search.getQuery()));
        }
         */
    }

    public void discardToken() {
        this.accessToken=null;
    }

    public class ServiceEndpointResponse{
        public List<ServiceEndpoint> data;
    }
}
