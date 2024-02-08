package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.ServerService;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.vectorclock.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import static pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;



public class ServerState {
    private final List<Operation> ledger;
    private final Map<String, Integer> userAccounts;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private boolean activated;
    private final String qualifier;
    private final ServerService serverService;
    private VectorClock valueTS;
    private VectorClock replicaTS;


    public ServerState(String qualifier, ServerService serverService) {
        this.ledger = new ArrayList<>();
        this.userAccounts = new HashMap<>();
        this.userAccounts.put("broker", 1000);
        activated = true;
        this.qualifier = qualifier;
        this.serverService = serverService;
        valueTS = new VectorClock();
        replicaTS = new VectorClock();
    }

    private boolean isBackup(String qualifier) {
        return qualifier.equals("B");
    }

    public VectorClock checkIn(VectorClock prevTS) throws Exception {

        lock.writeLock().lock();

        try {
            if (!activated) {
                throw new Exception("UNAVAILABLE");
            }
            incrementReplicaTS();
            VectorClock operationTS = new VectorClock(replicaTS.getTimeStamps());
            operationTS.mergeWith(prevTS);
            return operationTS;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void createAccount(String username, VectorClock prevTS, VectorClock operationTS) {

        CreateOp createOp = new CreateOp(username, operationTS, prevTS);
        createOp.updateStable(prevTS, valueTS);
        this.ledger.add(createOp);

        if (createOp.isStable())
            create(username, operationTS);
    }

    private void create(String username, VectorClock operationTS) {
        if (username.equals("broker")) {
            return;
        }

        if (this.userAccounts.containsKey(username)) {
            return;
        }

        this.userAccounts.put(username, 0);

        valueTS.mergeWith(operationTS);
    }

    public void deleteAccount(String username) throws Exception {

        if (isBackup(this.qualifier)) {
            throw new Exception("FORBIDDEN: Can't delete account on backup server");
        }

        lock.writeLock().lock();

        try {
            if (!activated) {
                throw new Exception("UNAVAILABLE");
            }

            if (username.equals("broker")) {
                throw new Exception("Can't delete broker!");
            }

            if (!this.userAccounts.containsKey(username)) {
                throw new Exception("User doesn't exists");
            }

            if (!(this.userAccounts.get(username) == 0)) {
                throw new Exception("Can't delete account without null balance");
            }

            DeleteOp deleteOp = new DeleteOp(username, new VectorClock(), new VectorClock());

            delete(username, deleteOp);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteAccountPropagate(String username) throws Exception{

        lock.writeLock().lock();

        try{
            if (!activated) {
                throw new Exception("UNAVAILABLE");
            }
            DeleteOp deleteOp = new DeleteOp(username, new VectorClock(), new VectorClock());
            delete(username, deleteOp);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void delete(String username, DeleteOp deleteOp) {
        this.userAccounts.remove(username);
        this.ledger.add(deleteOp);
    }

    public Map<Integer, VectorClock> balance(String username, VectorClock prevTS) throws Exception {

    lock.readLock().lock();
    try {
        if (!activated) {
            throw new Exception("UNAVAILABLE");
        }

        if (!valueTS.greaterOrEqual(prevTS)) {
            throw new Exception("Server is behind user");
        }

        if (!this.userAccounts.containsKey(username)) {
            throw new Exception("User doesn't exist");
        }

        VectorClock newTS = new VectorClock(valueTS.getTimeStamps());
        return getBalanceReturn(this.userAccounts.get(username), newTS);
    }
    finally {
        lock.readLock().unlock();
    }
}

    private Map<Integer, VectorClock> getBalanceReturn(Integer balance, VectorClock newTS) {

        Map<Integer, VectorClock> balanceReturn = new HashMap<>();

        balanceReturn.put(balance, newTS);

        return balanceReturn;
    }

    private void transferTo(String fromUsername, String destUsername, int amount, VectorClock prevTS, VectorClock operationTS) {

        lock.writeLock().lock();

        try {
            TransferOp transferOp = new TransferOp(fromUsername, destUsername, amount, operationTS, prevTS);
            transferOp.updateStable(prevTS, valueTS);
            this.ledger.add(transferOp);

            if (transferOp.isStable())
                transfer(fromUsername, destUsername, amount, operationTS);

        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void transfer(String fromUsername, String destUsername, int amount, VectorClock operationTS) {
        if (!(amount > 0)) {
            return;
        }

        if (!this.userAccounts.containsKey(fromUsername)) {
            return;
        }

        if (!this.userAccounts.containsKey(destUsername)) {
            return;
        }

        if (fromUsername.equals(destUsername)) {
            return;
        }

        if (!(this.userAccounts.get(fromUsername) >= amount)) {
            return;
        }

        // Updates origin account balance
        int initialOriginBalance = this.userAccounts.get(fromUsername);
        this.userAccounts.put(fromUsername, initialOriginBalance - amount);

        // Updates destination account balance
        int initialDestBalance = this.userAccounts.get(destUsername);
        this.userAccounts.put(destUsername, initialDestBalance + amount);

        valueTS.mergeWith(operationTS);
    }

    public void activate() {

        lock.writeLock().lock();
        try {
            this.activated = true;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void deactivate() {

        lock.writeLock().lock();
        try {
            this.activated = false;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public List<Operation> getLedgerState() {

        lock.readLock().lock();
        try {
            return new ArrayList<>(this.ledger);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public void executeOperation(OperationType OperationType, String fromUsername, String destUsername, int amount, VectorClock prevTS, VectorClock operationTS, Boolean executeOnly) {

        lock.writeLock().lock();

        try {
            if (executeOnly) {
                if (OperationType == OP_CREATE_ACCOUNT) {
                    this.create(fromUsername, operationTS);
                }
                else if (OperationType == OP_TRANSFER_TO) {
                    this.transfer(fromUsername,
                            destUsername,
                            amount,
                            operationTS);
                }
            }
            else {
                if (OperationType == OP_CREATE_ACCOUNT) {
                    this.createAccount(fromUsername,
                            prevTS,
                            operationTS
                    );
                } else if (OperationType == OP_TRANSFER_TO) {
                    this.transferTo(fromUsername,
                            destUsername,
                            amount,
                            prevTS,
                            operationTS
                    );
                }
            }
        }
        catch (Exception e) {
            System.out.println("Caught exception with description: " + e);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void updateState(List<Operation> updateLog, VectorClock senderReplicaTS) {

        lock.writeLock().lock();

        try {

            for(Operation operation: updateLog){
                if (operation.getOperationTS().lessOrEqual(replicaTS))
                    continue;
                if (operation.getOperationType() == OP_CREATE_ACCOUNT )
                    this.executeOperation(operation.getOperationType(), operation.getAccount(), "", 0, operation.getPrevTS(), operation.getOperationTS(), false);
                else
                    this.executeOperation(operation.getOperationType(), operation.getAccount(), ((TransferOp) operation).getDestAccount(), ((TransferOp) operation).getAmount(), operation.getPrevTS(), operation.getOperationTS(), false);

            }

            this.replicaTS.mergeWith(senderReplicaTS);

            // Check for stable operations
            ledger.forEach(operation -> {
                if (!operation.isStable()) {
                    operation.updateStable(operation.getPrevTS(), this.valueTS);
                    // check if it has become stable
                    if (operation.isStable()) {
                        if (operation.getOperationType() == OP_CREATE_ACCOUNT )
                            this.executeOperation(operation.getOperationType(), operation.getAccount(), "", 0, operation.getPrevTS(), operation.getOperationTS(), true);
                        else
                            this.executeOperation(operation.getOperationType(), operation.getAccount(), ((TransferOp) operation).getDestAccount(), ((TransferOp) operation).getAmount(), operation.getPrevTS(), operation.getOperationTS(), true);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("[UpdateState] Caught exception with description: " + e);
        }
        lock.writeLock().unlock();
    }


    private void incrementReplicaTS() {
        int index = (int) qualifier.charAt(0) - (int) 'A';
        replicaTS.setTimeStamp(index,replicaTS.getTimeStamp(index)+1);
    }

    public void gossip() {

        ArrayList<String> toQualifier = new ArrayList<>();

        switch (qualifier) {
            case "A":
                toQualifier.add("B");
                toQualifier.add("C");
                break;
            case "B":
                toQualifier.add("A");
                toQualifier.add("C");
                break;
            case "C":
                toQualifier.add("A");
                toQualifier.add("B");
                break;
            default:
                break;
        }

        serverService.propagateState(this.getLedgerState(), replicaTS, toQualifier.get(0));
        serverService.propagateState(this.getLedgerState(), replicaTS, toQualifier.get(1));

    }
}
