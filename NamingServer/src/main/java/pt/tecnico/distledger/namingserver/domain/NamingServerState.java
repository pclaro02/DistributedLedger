package pt.tecnico.distledger.namingserver.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class NamingServerState {

    private final Map<String, ServiceEntry> serviceEntries;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public NamingServerState() {
        serviceEntries = new HashMap<>();
    }

    public void register(String serviceName, String serverQualifier, String serverAddressAndPort) throws Exception {

        lock.writeLock().lock();

        try {
            if(serviceEntries.containsKey(serviceName) && serviceEntries
                    .get(serviceName)
                    .getAssociatedQualifiers()
                    .contains(serverQualifier)) {
                throw new Exception("Not possible to register the server");
            }

            if (!serviceEntries.containsKey(serviceName)) {
                serviceEntries.put(serviceName, new ServiceEntry(serviceName));
            }

            ServiceEntry serviceEntry = serviceEntries.get(serviceName);

            serviceEntry.addServerEntry(new ServerEntry(serverQualifier, serverAddressAndPort));

        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public List<ServerEntry> lookup(String serviceName, String serverQualifier) throws Exception {

        System.out.println("[LOG NamingServerState] Qualifier: " + serverQualifier);
        System.out.println("[LOG NamingServerState] Service Name: " + serviceName);

        lock.readLock().lock();

        try {
            if (!serviceEntries.containsKey(serviceName)) {
                throw new Exception("Service does not exist.");
            }

            ArrayList<ServerEntry> lookupResult = new ArrayList<>();

            if (serverQualifier.isEmpty()) {
                lookupResult.addAll(serviceEntries.get(serviceName).getServerEntries());
                return lookupResult;
            }

            lookupResult.addAll(serviceEntries.get(serviceName).getServerEntries().stream()
                    .filter(serverEntry -> serverEntry.getServerQualifier().equals(serverQualifier))
                    .collect(Collectors.toList()));

            return lookupResult;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void delete(String serviceName, String serverAddress) throws Exception {

        lock.writeLock().lock();

        try {
            if (!serviceEntries.containsKey(serviceName) ||
                    serviceEntries.get(serviceName).getServerEntries().stream()
                            .noneMatch(serverEntry -> serverEntry.getServerAddress().equals(serverAddress))) {
                throw new Exception("Not possible to remove the server.");
            }

            List<ServerEntry> entriesToBeDeleted = serviceEntries.get(serviceName).getServerEntries().stream()
                    .filter(serverEntry -> serverEntry.getServerAddress().equals(serverAddress))
                    .collect(Collectors.toList());

            for (ServerEntry entry : entriesToBeDeleted) {
                serviceEntries.get(serviceName).removeServerEntry(entry);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
