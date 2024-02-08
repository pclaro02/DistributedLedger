package pt.tecnico.distledger.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.vectorclock.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerService {

    private final ManagedChannel namingServerChannel;
    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;
    private final Map<String, ServerConnectionResources> serversData;

    public ServerService(String namingServerHost, int namingServerPort) {

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
        serversData.values().forEach(ServerConnectionResources::closeServiceServerChannel);
    }

    public DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub getServerServiceStub(String serviceName, String associatedQualifier) {
        if(!serversData.containsKey(associatedQualifier)) {
            serversData.put(associatedQualifier, new ServerConnectionResources(namingServerStub, serviceName, associatedQualifier));
        }
        return serversData.get(associatedQualifier).getServiceServerStub();
    }

    public void register(String serviceName, String serverQualifier, String serverAddress) {

        RegisterRequest request = RegisterRequest
                .newBuilder()
                .setServiceName(serviceName)
                .setServerQualifier(serverQualifier)
                .setServerAddress(serverAddress)
                .build();

        try {
            RegisterResponse response = namingServerStub.register(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println("NOT OK!\nCaught exception with description: " +
                    e.getStatus().getDescription());
        }
    }

    public void delete(String serviceName, String serverAddress) {
        DeleteRequest request = DeleteRequest.newBuilder()
                .setServiceName(serviceName)
                .setServerAddress(serverAddress)
                .build();

        try {
            DeleteResponse response = namingServerStub.delete(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println("NOT OK!\nCaught exception with description: " +
                    e.getStatus().getDescription());
        }
    }

    private DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub getCrossServerStub (String serviceName, String associatedQualifier) {
        if(!serversData.containsKey(associatedQualifier)) {
      serversData.put(
          associatedQualifier,
          new ServerConnectionResources(namingServerStub, serviceName, associatedQualifier));
        }
        return serversData.get(associatedQualifier).getServiceServerStub();
    }

    public void propagateState(List<Operation> updateLog, VectorClock replicaTS, String serverQualifier) {

        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub crossServerStub;
        String serviceName = "DistLedger";

        List<DistLedgerCommonDefinitions.Operation> ledger_operations = new ArrayList<>();

        updateLog.stream().map(op -> {

            DistLedgerCommonDefinitions.Operation.Builder ledgerOperationBuilder = DistLedgerCommonDefinitions.Operation
                    .newBuilder()
                    .setTypeValue(op.getOperationType().getNumber())
                    .setUserId(op.getAccount())
                    .addAllPrevTS(op.getPrevTS().getTimeStamps())
                    .addAllTS(op.getOperationTS().getTimeStamps())
                    .setStable(op.isStable());

            if (op.getOperationType() == OperationType.OP_TRANSFER_TO) {
                ledgerOperationBuilder
                        .setAmount(((TransferOp) op).getAmount())
                        .setDestUserId(((TransferOp) op).getDestAccount());
            }
            return ledgerOperationBuilder.build();

        }).forEach(ledger_operations::add);

        DistLedgerCommonDefinitions.LedgerState ledgerState = DistLedgerCommonDefinitions
                .LedgerState
                .newBuilder()
                .addAllUpdateLog(ledger_operations)
                .build();

        PropagateStateRequest request = PropagateStateRequest.newBuilder()
                .setUpdateLog(ledgerState)
                .addAllReplicaTS(replicaTS.getTimeStamps())
                .build();

        try {
            if ((crossServerStub = getCrossServerStub(serviceName, serverQualifier)) == null) {
                //Retry initial connection
                serversData.get(serverQualifier).updateServerAddressInUse();
                crossServerStub = getCrossServerStub(serviceName, serverQualifier);
            }
            PropagateStateResponse response = crossServerStub.propagateState(request);
        }
        catch (StatusRuntimeException e) {

            // Exception related to connection issues
            if (e.getStatus().getDescription().equals("io exception")
                    || e.getStatus().getDescription().equals("Channel shutdown invoked")
                    ||  e.getStatus().getDescription().equals("UNAVAILABLE")) {

                serversData.get(serverQualifier).updateServerAddressInUse();
                // Tries to perform operation one more time after update
                try {
                    crossServerStub = getCrossServerStub(serviceName, serverQualifier);
                    PropagateStateResponse response = crossServerStub.propagateState(request);
                }
                catch (StatusRuntimeException secondExc) {
                    return;
                }
            } else
                return;

        } catch (NullPointerException nullExc) {
            return;
        }
    }

}
