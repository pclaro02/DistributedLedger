package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.vectorclock.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import java.util.ArrayList;
import java.util.Map;

import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private ServerState serverState;

    public UserServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void balance(UserDistLedger.BalanceRequest request, StreamObserver<UserDistLedger.BalanceResponse> responseObserver) {

        System.err.println("[LOG UserServiceImpl] Request: " + request);

        String username = request.getUserId();
        VectorClock prevTS = new VectorClock(request.getPrevTSList());

        try {

            Map.Entry<Integer, VectorClock> result = new ArrayList<>(serverState.balance(username, prevTS).entrySet()).get(0);

            Integer balance = result.getKey();
            VectorClock newTS = result.getValue();

            System.err.println("[LOG UserServiceImpl] Balance operation newTS = " + newTS.getTimeStamps().toString());

            UserDistLedger.BalanceResponse response = UserDistLedger.BalanceResponse
                    .newBuilder()
                    .setValue(balance)
                    .addAllNewTS(newTS.getTimeStamps())
                    .build();

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
    @Override
    public void createAccount(UserDistLedger.CreateAccountRequest request, StreamObserver<UserDistLedger.CreateAccountResponse> responseObserver) {

        System.err.println("[LOG UserServiceImpl] Request: " + request);

        String username = request.getUserId();
        VectorClock prevTS = new VectorClock(request.getPrevTSList());

        try {
            VectorClock newTS = serverState.checkIn(prevTS);

            UserDistLedger.CreateAccountResponse response = UserDistLedger.CreateAccountResponse
                    .newBuilder()
                    .addAllNewTS(newTS.getTimeStamps())
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

            //serverState.createAccount(username, prevTS, newTS);
            serverState.executeOperation(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT, username,"", 0, prevTS, newTS, false);
        }
        catch (Exception e) {
            responseObserver
                .onError(INVALID_ARGUMENT
                        .withDescription(e.getMessage())
                        .asRuntimeException()
                );
        }
    }

    @Override
    public void deleteAccount(UserDistLedger.DeleteAccountRequest request, StreamObserver<UserDistLedger.DeleteAccountResponse> responseObserver) {

        System.err.println("[LOG UserServiceImpl] Request: " + request);

        String username = request.getUserId();

        try {
            serverState.deleteAccount(username);

            UserDistLedger.DeleteAccountResponse response = UserDistLedger.DeleteAccountResponse
                    .newBuilder()
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();
        }
        catch (Exception e) {
            responseObserver
                    .onError(NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException()
                    );
        }
    }

    @Override
    public void transferTo(UserDistLedger.TransferToRequest request, StreamObserver<UserDistLedger.TransferToResponse> responseObserver) {

        System.err.println("[LOG UserServiceImpl] Request: " + request);

        String accountFrom = request.getAccountFrom();
        String accountTo = request.getAccountTo();
        int amount = request.getAmount();
        VectorClock prevTS = new VectorClock(request.getPrevTSList());

        try {
            VectorClock newTS = serverState.checkIn(prevTS);

            UserDistLedger.TransferToResponse response = UserDistLedger.TransferToResponse
                    .newBuilder()
                    .addAllNewTS(newTS.getTimeStamps())
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

            serverState.executeOperation(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO, accountFrom,accountTo, amount, prevTS, newTS, false);

        }
        catch (Exception e) {
            responseObserver
                    .onError(NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException()
                    );
        }
    }
}
