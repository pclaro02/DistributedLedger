package pt.tecnico.distledger.namingserver.domain;

public class ServerEntry {

    private String serverQualifier;

    private String serverAddress;

    private String serverPort;

    public ServerEntry(String serverQualifier, String serverAddressAndPort) {
        setServerQualifier(serverQualifier);
        setServerAddress(serverAddressAndPort);
    }

    public String getServerQualifier() {
        return serverQualifier;
    }

    public void setServerQualifier(String serverQualifier) {
        this.serverQualifier = serverQualifier;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress.split(":")[0];
        this.serverPort = serverAddress.split(":")[1];
    }

    public String getServerAddress() {
        return serverAddress + ":" + serverPort;
    }

}