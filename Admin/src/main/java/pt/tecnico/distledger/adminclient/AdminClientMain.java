package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

public class AdminClientMain {
    public static void main(String[] args) {

        System.out.println(AdminClientMain.class.getSimpleName());

        CommandParser parser = new CommandParser(new AdminService("localhost", 5001));
        parser.parseInput();
    }
}
