package pt.tecnico.distledger.namingserver.domain;

import java.util.Set;
import java.util.HashSet;

import java.util.List;
import java.util.stream.Collectors;

public class ServiceEntry {

    private String serviceName;

    Set<ServerEntry> serverEntries;

    public ServiceEntry(String serviceName) {
        this.serviceName = serviceName;
        this.serverEntries = new HashSet<>();
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void addServerEntry(ServerEntry serverEntry) throws Exception {
        if(serverEntries.contains(serverEntry)) {
            throw new Exception("ServerEntry already exists");
        }

        serverEntries.add(serverEntry);
    }

    public void removeServerEntry(ServerEntry serverEntry) throws Exception {
        if(!serverEntries.contains(serverEntry)) {
            throw new Exception("ServerEntry does not exist");
        }

        serverEntries.remove(serverEntry);
    }

    public Set<ServerEntry> getServerEntries() {
        return serverEntries;
    }

    public List<String> getAssociatedQualifiers() {
        return serverEntries.stream().map(ServerEntry::getServerQualifier).collect(Collectors.toList());
    }
}