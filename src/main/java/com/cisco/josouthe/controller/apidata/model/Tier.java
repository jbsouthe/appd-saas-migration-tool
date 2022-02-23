package com.cisco.josouthe.controller.apidata.model;

public class Tier implements Comparable<Tier> {
    public String name, type, agentType;
    public long id, numberOfNodes;

    @Override
    public int compareTo( Tier o ) {
        if(o==null) return -1;
        if( o.name.equals(name) ) return 0;
        return 1;
    }

    public boolean equals( Tier o ) {
        return compareTo(o) == 0;
    }
}
