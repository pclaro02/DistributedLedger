package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;

public class UserClientMain {
    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        CommandParser parser = new CommandParser(new UserService("localhost", 5001));
        parser.parseInput();
    }
}
