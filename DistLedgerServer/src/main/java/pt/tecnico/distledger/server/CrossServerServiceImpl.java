package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.vectorclock.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.tecnico.distledger.server.domain.operation.Operation;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import java.util.ArrayList;
import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;



public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase{

    private ServerState serverState;

    public CrossServerServiceImpl(ServerState serverState) {

        this.serverState = serverState;
    }


    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {

        try {

            System.err.println("[LOG CrossServerServiceImpl] Request: " + request);

            List<Operation> ledger_operations = new ArrayList<>();

            request.getUpdateLog().getUpdateLogList().stream().map(op -> {

                if (op.getType().equals(OperationType.OP_CREATE_ACCOUNT)) {
                    CreateOp createOp = new CreateOp(op.getUserId(), new VectorClock(op.getTSList()), new VectorClock(op.getPrevTSList()));
                    createOp.setStable(op.getStable());
                    return createOp;
                }

                else {
                    TransferOp transferOp = new TransferOp(op.getUserId(), op.getDestUserId(), op.getAmount(), new VectorClock(op.getTSList()), new VectorClock(op.getPrevTSList()));
                    transferOp.setStable(op.getStable());
                    return transferOp;
                }

            }).forEach(ledger_operations::add);

            serverState.updateState(ledger_operations, new VectorClock(request.getReplicaTSList()));

            PropagateStateResponse response =
                    CrossServerDistLedger.PropagateStateResponse.newBuilder().build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();
        }
        catch (Exception e) {
            responseObserver
                    .onError(INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asRuntimeException()
                    );
        }
    }

}
