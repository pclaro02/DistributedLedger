package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.tecnico.distledger.vectorclock.VectorClock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {

    private final ManagedChannel namingServerChannel;
    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;
    private final Map<String, UserConnectionResources> serversData;
    private VectorClock prevTS;

    public UserService(String namingServerHost, int namingServerPort) {

        String target = namingServerHost + ":" + namingServerPort;

        this.namingServerChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);

        this.serversData = new HashMap<>();

        this.prevTS = new VectorClock();
    }

    public void closeAllChannels() {
        closeAllServiceServerConnections();
        namingServerChannel.shutdownNow();
    }

    private void closeAllServiceServerConnections() {
        serversData.values().forEach(UserConnectionResources::closeServiceServerChannel);
    }

    private UserServiceGrpc.UserServiceBlockingStub getServerServiceStub (String serviceName, String associatedQualifier) {
        if(!serversData.containsKey(associatedQualifier)) {
            serversData.put(associatedQualifier, new UserConnectionResources(namingServerStub, serviceName, associatedQualifier));
        }
        return serversData.get(associatedQualifier).getServiceServerStub();
    }

    public void createAccount(String serverQualifier, String username) {

        CreateAccountRequest request = CreateAccountRequest
                .newBuilder()
                .setUserId(username)
                .addAllPrevTS(this.prevTS.getTimeStamps())
                .build();
        UserServiceGrpc.UserServiceBlockingStub stub;

        try {
            if((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {
                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            System.err.println("[LOG UserService] BEFORE Balance operation prev = " + prevTS.getTimeStamps().toString());

            UserDistLedger.CreateAccountResponse response = stub.createAccount(request);
            List<Integer> newTSList = response.getNewTSList();
            prevTS.mergeWith(new VectorClock(newTSList));
            System.out.println("OK");

            System.err.println("[LOG UserService] AFTER Balance operation prev = " + prevTS.getTimeStamps().toString());

        } catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {

                    getServerServiceStub("DistLedger", serverQualifier).createAccount(request);
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

    public void deleteAccount(String serverQualifier, String username) {

        DeleteAccountRequest request = DeleteAccountRequest
                .newBuilder()
                .setUserId(username)
                .build();
        UserServiceGrpc.UserServiceBlockingStub stub;

        try {
            if((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {
                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            stub.deleteAccount(request);
            System.out.println("OK");

        } catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {

                    getServerServiceStub("DistLedger", serverQualifier).deleteAccount(request);
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

    public void balance(String serverQualifier, String username) {

        BalanceRequest request = BalanceRequest.newBuilder()
                .setUserId(username)
                .addAllPrevTS(this.prevTS.getTimeStamps())
                .build();
        UserServiceGrpc.UserServiceBlockingStub stub;

        try {
            if ((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {

                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            balanceOperation(request, stub);

        } catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {

                    balanceOperation(request, getServerServiceStub("DistLedger", serverQualifier));

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

    private void balanceOperation(BalanceRequest request, UserServiceGrpc.UserServiceBlockingStub stub) {

        System.err.println("[LOG UserService] BEFORE Balance operation prev = " + prevTS.getTimeStamps().toString());

        BalanceResponse response = stub.balance(request);
        Integer value = response.getValue();
        List<Integer> newTSList = response.getNewTSList();
        prevTS.mergeWith(new VectorClock(newTSList));
        System.out.println("OK");

        System.err.println("[LOG UserService] AFTER Balance operation prev = " + prevTS.getTimeStamps().toString());

        if (value != 0) {
            System.out.println(value);
        }
    }

    public void transferTo(String serverQualifier, String from, String dest, int amount) {

        TransferToRequest request = TransferToRequest
                .newBuilder()
                .setAccountFrom(from)
                .setAccountTo(dest)
                .setAmount(amount)
                .addAllPrevTS(this.prevTS.getTimeStamps())
                .build();
        UserServiceGrpc.UserServiceBlockingStub stub;

        try {
            if ((stub = getServerServiceStub("DistLedger", serverQualifier)) == null) {

                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                stub = getServerServiceStub("DistLedger", serverQualifier);
            }

            System.err.println("[LOG UserService] BEFORE transferTo operation prev = " + prevTS.getTimeStamps().toString());

            UserDistLedger.TransferToResponse response = stub.transferTo(request);
            List<Integer> newTSList = response.getNewTSList();
            prevTS.mergeWith(new VectorClock(newTSList));

            System.err.println("[LOG UserService] AFTER transferTo operation prev = " + prevTS.getTimeStamps().toString());

            System.out.println("OK");
        } catch (StatusRuntimeException e) {

        // Exception related to connection issues
        if (e.getStatus().getDescription().equals("io exception")
                || e.getStatus().getDescription().equals("Channel shutdown invoked")
                ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

            serversData.get(serverQualifier).updateServerAddressInUse();
            // Tries to perform operation one more time after update
            try {

                getServerServiceStub("DistLedger", serverQualifier).transferTo(request);
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
