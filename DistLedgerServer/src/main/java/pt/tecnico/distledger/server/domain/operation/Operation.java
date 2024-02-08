package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.vectorclock.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

import java.util.Vector;

public class Operation {
    private String account;
    private OperationType operationType;
    private VectorClock operationTS;
    private VectorClock prevTS;
    private boolean stable;

    public Operation(String fromAccount, OperationType operationType, VectorClock operationTS, VectorClock prevTS) {
        this.account = fromAccount;
        this.operationType = operationType;
        this.operationTS = operationTS;
        this.prevTS = prevTS;
        stable = false;
    }

    public Operation(String fromAccount, OperationType operationType) {
        this.account = fromAccount;
        this.operationType = operationType;
    }


    public OperationType getOperationType() {
        return this.operationType;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public VectorClock getOperationTS() {
        return operationTS;
    }

    public VectorClock getPrevTS() {
        return prevTS;
    }

    public boolean isStable() {
        return stable;
    }

    public void setStable(boolean stable) {
        this.stable = stable;
    }

    public void updateStable(VectorClock prevTS, VectorClock valueTS) {
        stable = valueTS.greaterOrEqual(prevTS);
    }
}
