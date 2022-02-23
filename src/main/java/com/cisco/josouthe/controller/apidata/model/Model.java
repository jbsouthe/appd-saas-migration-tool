package com.cisco.josouthe.controller.apidata.model;

import java.util.List;

public class Model {
    private ApplicationListing applicationListing;
    public Model( ApplicationListing applicationListing ) {
        this.applicationListing = applicationListing;
    }
    public Application getApplication( String name ) {
        for( Application application : applicationListing.getApplications() )
            if( application.name.equals(name)) return application;
        return null;
    }

    public List<Application> getApplications() { return applicationListing.getApplications(); }
    public List<Application> getAPMApplications() { return applicationListing.apmApplications; }

    public void addApplication(Application application) {
        applicationListing.apmApplications.add(application);
        applicationListing._allApplications.add(application);
    }
}
