package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.vectorclock.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

public class TransferOp extends Operation {
    private String destAccount;
    private int amount;



    public TransferOp(String fromAccount, String destAccount, int amount, VectorClock operationTS, VectorClock prevTS) {
        super(fromAccount, OperationType.OP_TRANSFER_TO, operationTS, prevTS);
        this.destAccount = destAccount;
        this.amount = amount;
    }

    public String getDestAccount() {
        return destAccount;
    }

    public void setDestAccount(String destAccount) {
        this.destAccount = destAccount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: " + this.getAccount() +
                "    destUserId: " + this.getDestAccount() +
                "    amount: " + this.getAmount() +
                "\n    }";
    }

}
