package pt.tecnico.distledger.namingserver;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;

import static io.grpc.Status.INVALID_ARGUMENT;

import java.util.List;
import java.util.stream.Collectors;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase {

    private final NamingServerState namingServerState;

    public NamingServerServiceImpl() {
        namingServerState = new NamingServerState();
    }

    @Override
    public void register(NamingServer.RegisterRequest request, StreamObserver<NamingServer.RegisterResponse> responseObserver) {

        System.err.println("[LOG NamingServerServiceImpl] register request: " + request.toString());

        try {
            namingServerState.register(
                    request.getServiceName(),
                    request.getServerQualifier(),
                    request.getServerAddress()
            );

            NamingServer.RegisterResponse response = NamingServer.RegisterResponse
                    .newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(Exception e) {
            responseObserver
                    .onError(INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asRuntimeException()
                    );
        }
    }

    @Override
    public void lookup(NamingServer.LookupRequest request, StreamObserver<NamingServer.LookupResponse> responseObserver) {

        System.err.println("[LOG NamingServerServiceImpl] Lookup request: " + request.toString());

        try {
            List<ServerEntry> lookupResult = namingServerState.lookup(
                    request.getServiceName(),request.getAssociatedQualifier());

            List<NamingServer.ServerLookupResult> serverLookupResults = lookupResult.stream().
                    map(result -> NamingServer.ServerLookupResult.newBuilder()
                            .setServerQualifier(result.getServerQualifier())
                            .setServerAddress(result.getServerAddress()).build())
                    .collect(Collectors.toList());

            System.out.println("[LOG NamingServerServiceImpl] Results: " + (serverLookupResults.isEmpty()));

            NamingServer.LookupResponse response = NamingServer.LookupResponse.newBuilder()
                    .addAllServers(serverLookupResults).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver
                    .onError(INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asRuntimeException()
                    );
        }
    }

    @Override
    public void delete(NamingServer.DeleteRequest request, StreamObserver<NamingServer.DeleteResponse> responseObserver) {

        System.err.println("[LOG NamingServerServiceImpl] Delete request: " + request.toString());

        try {
            namingServerState.delete(request.getServiceName(), request.getServerAddress());

            NamingServer.DeleteResponse response = NamingServer.DeleteResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver
                    .onError(INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asRuntimeException()
                    );
        }
    }
}
