package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class NamingServerMain {

    public static void main(String[] args) throws IOException, InterruptedException{

        BindableService namingServerServiceImpl = new NamingServerServiceImpl();

        final int port = 5001;

        // Create a new server to listen on port
        Server server = ServerBuilder
                .forPort(port)
                .addService(namingServerServiceImpl)
                .build();

        // start the server
        server.start();
        System.out.println("Server started on port " + port);

        // Wait until server is terminated to start the main thread
        server.awaitTermination();
    }
}
