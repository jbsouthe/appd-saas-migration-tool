package com.cisco.josouthe.controller.apidata.model;

public class Node implements Comparable<Node> {
    public String name, type, tierName, machineName, machineOSType, machineAgentVersion, appAgentVersion, agentType;
    public long id, tierId, machineId;
    public boolean machineAgentPresent, appAgentPresent;

    @Override
    public int compareTo( Node o ) {
        if( o==null) return -1;
        if( o.name.equals(name) && o.tierName.equals(tierName)  ) return 0;
        return -1;
    }

    public boolean equals( Node o ) {
        return compareTo(o) == 0;
    }
}
