package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;

import java.io.IOException;
public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        // receive and print args - debug mode
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check args and throw exceptions
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
            return;
        }

        //receives target (NamingServer) address
        ServerService service = new ServerService("localhost", 5001);

        // create new server to listen on port
        final int port = Integer.parseInt(args[0]);
        //final int port = 2001;
        final String qualifier = args[1];
        //final String qualifier = "A";

        ServerState serverState = new ServerState(qualifier, service);

        final BindableService userImpl = new UserServiceImpl(serverState);
        final BindableService adminImpl = new AdminServiceImpl(serverState);
        final BindableService crossServerImpl = new CrossServerServiceImpl(serverState);

        Server server = null;
        String serverAddress = null;
        // Create a new server to listen on port
        try {
            server = ServerBuilder
                    .forPort(port)
                    .addService(userImpl)
                    .addService(adminImpl)
                    .addService(crossServerImpl)
                    .build();

            // start the server
            server.start();

            System.out.println("Server started on port " + port);

            serverAddress = "localhost" + ":" + port;
            service.register("DistLedger", qualifier, serverAddress);

            // Wait until server is terminated.
            System.out.println("Press enter to shutdown");
            System.in.read();
            service.delete("DistLedger", serverAddress);
            service.closeAllChannels();
            server.shutdown();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            service.delete("DistLedger", serverAddress);
            service.closeAllChannels();
            assert server != null;
            server.shutdown();
            System.exit(0);
        }
    }

}

