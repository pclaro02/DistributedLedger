package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private ServerState serverState;

    public AdminServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void activate(AdminDistLedger.ActivateRequest request, StreamObserver<AdminDistLedger.ActivateResponse> responseObserver) {

        System.err.println("[LOG AdminServiceImpl] Request: activate");

        serverState.activate();

        ActivateResponse response = AdminDistLedger.ActivateResponse
                .newBuilder()
                .build();

        responseObserver.onNext(response);

        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(AdminDistLedger.DeactivateRequest request, StreamObserver<AdminDistLedger.DeactivateResponse> responseObserver) {

        System.err.println("[LOG AdminServiceImpl] Request: deactivate");

        serverState.deactivate();

        DeactivateResponse response = AdminDistLedger.DeactivateResponse
                .newBuilder()
                .build();

        responseObserver.onNext(response);

        responseObserver.onCompleted();
    }

    @Override
    public void getLedgerState(AdminDistLedger.getLedgerStateRequest request, StreamObserver<AdminDistLedger.getLedgerStateResponse> responseObserver) {

        System.err.println("[LOG AdminServiceImpl] Request: getLedgerState");

        List<Operation> operations = serverState.getLedgerState();

        List<DistLedgerCommonDefinitions.Operation> ledger_operations = new ArrayList<>();

        operations.stream().map(op -> {

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

        getLedgerStateResponse response = AdminDistLedger.getLedgerStateResponse
                .newBuilder().setLedgerState(ledgerState)
                .build();

        responseObserver.onNext(response);

        responseObserver.onCompleted();
    }

    @Override
    public void gossip(AdminDistLedger.GossipRequest request, StreamObserver<AdminDistLedger.GossipResponse> responseObserver) {

        System.err.println("[LOG AdminServiceImpl] Request: gossip");

        serverState.gossip();

        GossipResponse response = AdminDistLedger.GossipResponse
                .newBuilder()
                .build();

        responseObserver.onNext(response);

        responseObserver.onCompleted();
    }
}
