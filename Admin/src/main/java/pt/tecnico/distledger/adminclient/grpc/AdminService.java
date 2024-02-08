package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;

import java.util.HashMap;
import java.util.Map;

public class AdminService {

    // build channel and stub
    private final ManagedChannel namingServerChannel;
    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;
    private final Map<String, AdminConnectionResources> serversData;

    public AdminService(String namingServerHost, int namingServerPort) {

        String target = namingServerHost + ":" + namingServerPort;

        this.namingServerChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);

        this.serversData = new HashMap<>();
    }

    public void closeAllChannels() {
        closeAllServiceServerConnections();
        namingServerChannel.shutdownNow();
    }

    private void closeAllServiceServerConnections() {
        serversData.values().forEach(AdminConnectionResources::closeServiceServerChannel);
    }

    private AdminServiceGrpc.AdminServiceBlockingStub getServerServiceStub (String serviceName, String associatedQualifier) {
        if(!serversData.containsKey(associatedQualifier)) {
            serversData.put(associatedQualifier, new AdminConnectionResources(namingServerStub, serviceName, associatedQualifier));
        }
        return serversData.get(associatedQualifier).getServiceServerStub();
    }


    // activate server
    public void activate(String serverQualifier) {

        ActivateRequest request = ActivateRequest.newBuilder().build();
        AdminServiceGrpc.AdminServiceBlockingStub stub;
        try {
            if((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {
                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            stub.activate(request);
            System.out.println("OK");

        } catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {
                    getServerServiceStub("DistLedger", serverQualifier).activate(request);
                    System.out.println("OK");

                } catch (StatusRuntimeException secondExc) {
                    System.out.println("NOT OK!\nCaught exception with description: " + secondExc.getStatus().getDescription());
                }
            } else {
                System.out.println("NOT OK!\nCaught exception with description: " + e.getStatus().getDescription());
            }
        } catch (NullPointerException nullExc) {
            // From stub == null
            System.out.println("NOT OK!\nServer not present in NamingServer");
        }
    }

    // deactivate server
    public void deactivate(String serverQualifier) {

        DeactivateRequest request = DeactivateRequest.newBuilder().build();
        AdminServiceGrpc.AdminServiceBlockingStub stub;

        try {
            if((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {
                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            stub.deactivate(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {

                    getServerServiceStub("DistLedger", serverQualifier).deactivate(request);
                    System.out.println("OK");

                } catch (StatusRuntimeException secondExc) {
                    System.out.println("NOT OK!\nCaught exception with description: " + secondExc.getStatus().getDescription());
                }
            } else {
                System.out.println("NOT OK!\nCaught exception with description: " + e.getStatus().getDescription());
            }
        } catch (NullPointerException nullExc) {
            // From stub == null
            System.out.println("NOT OK!\nServer not present in NamingServer");
        }
    }

    // get ledger state
    public void getLedgerState(String serverQualifier) {

        getLedgerStateRequest request = getLedgerStateRequest
                .newBuilder()
                .build();
        AdminServiceGrpc.AdminServiceBlockingStub stub;

        try {
            if ((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {
                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            getLedgerStateOperation(request, stub);

        } catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {

                    getLedgerStateOperation(request, getServerServiceStub("DistLedger", serverQualifier));

                } catch (StatusRuntimeException secondExc) {
                    System.out.println("NOT OK!\nCaught exception with description: " + secondExc.getStatus().getDescription());
                }
            } else {
                System.out.println("NOT OK!\nCaught exception with description: " + e.getStatus().getDescription());
            }
        } catch (NullPointerException nullExc) {
            // From stub == null
            System.out.println("NOT OK!\nServer not present in NamingServer");
        }
    }

    private void getLedgerStateOperation(getLedgerStateRequest request, AdminServiceGrpc.AdminServiceBlockingStub stub) {

        AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);
        DistLedgerCommonDefinitions.LedgerState updateLog = response.getLedgerState();
        System.out.println("OK");
        System.out.println(updateLog);
    }

    public void gossip(String serverQualifier) {

        GossipRequest request = GossipRequest
                .newBuilder()
                .build();
        AdminServiceGrpc.AdminServiceBlockingStub stub;

        try {
            if ((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {
                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            GossipResponse response = stub.gossip(request);
            System.out.println("OK");

        } catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {

                    GossipResponse response = getServerServiceStub("DistLedger", serverQualifier).gossip(request);
                    System.out.println("OK");

                } catch (StatusRuntimeException secondExc) {
                    System.out.println("NOT OK!\nCaught exception with description: " + secondExc.getStatus().getDescription());
                }
            } else {
                System.out.println("NOT OK!\nCaught exception with description: " + e.getStatus().getDescription());
            }
        } catch (NullPointerException nullExc) {
            // From stub == null
            System.out.println("NOT OK!\nServer not present in NamingServer");
        }
    }
}
