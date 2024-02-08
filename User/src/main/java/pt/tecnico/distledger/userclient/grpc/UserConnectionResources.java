package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class UserConnectionResources {

    private final String serviceName;
    private final String associatedQualifier;
    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;
    private ManagedChannel serviceServerChannel;
    private UserServiceGrpc.UserServiceBlockingStub serviceServerStub;
    private String serverAddressInUse;

    public UserConnectionResources(NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub, String serviceName, String associatedQualifier) {
        this.serverAddressInUse = null;
        this.associatedQualifier = associatedQualifier;
        this.namingServerStub = namingServerStub;
        this.serviceName = serviceName;
        updateServerAddressInUse();
    }

    public UserServiceGrpc.UserServiceBlockingStub getServiceServerStub() {
        return serviceServerStub;
    }

    public void openServiceServerChannel() {
        this.serviceServerChannel = ManagedChannelBuilder.forTarget(this.serverAddressInUse).usePlaintext().build();
        this.serviceServerStub = UserServiceGrpc.newBlockingStub(serviceServerChannel);
    }

    public void closeServiceServerChannel() {
        if(serviceServerChannel != null)
            serviceServerChannel.shutdownNow();
    }

    //To be used when a lookup is made.
    public void updateServerAddressInUse() {

        if ((serverAddressInUse = getAddressFromLookup(serviceName, associatedQualifier)) == null) {
            //aborts operation
            return;
        }

        openServiceServerChannel();
    }

    private List<NamingServer.ServerLookupResult> lookup(String serviceName, String associatedQualifier) throws StatusRuntimeException {

        List<NamingServer.ServerLookupResult> lookupResults;

        NamingServer.LookupRequest request = NamingServer.LookupRequest
                .newBuilder()
                .setServiceName(serviceName)
                .setAssociatedQualifier(associatedQualifier)
                .build();

        NamingServer.LookupResponse response = namingServerStub.lookup(request);

        lookupResults = new ArrayList<>(response.getServersList());
        return lookupResults;
    }


    private String getAddressFromLookup(String serviceName, String associatedQualifier) {

        try {
            if (!lookup(serviceName, associatedQualifier).isEmpty()) {
                return lookup(serviceName, associatedQualifier).get(0).getServerAddress();
            }
        } catch (StatusRuntimeException e) {
            System.err.println("NOT OK: Lookup for " + serviceName + associatedQualifier + " failed\n" + e.getStatus().getDescription());
        }
        return null;
    }
}
