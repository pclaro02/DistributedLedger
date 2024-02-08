package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.vectorclock.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

public class DeleteOp extends Operation {

    public DeleteOp(String account, VectorClock operationTS, VectorClock prevTS) {
        super(account, OperationType.OP_DELETE_ACCOUNT, operationTS, prevTS);
    }

    @Override
    public String toString() {
        return "  ledger {\n" +
                "    type: OP_DELETE_ACCOUNT\n" +
                "    userId: " + this.getAccount() +
                "\n    }";
    }

}
