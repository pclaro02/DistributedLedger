# Mini Report - SD_P3
## Gossip Architecture Implementation - Explanation 

### Vector Clock
This Java class implements a vector clock as a data structure with an array of Integers as attribute.
The size of the array is defined as a global variable. It initializes an array of timestamps, full of zeros, or
with timestamps provided as an argument (for example, when creating a new vector, i.e. a copy, is needed).

### Reading Operations
The operation starts by checking if the received vector, `prevTS`, is absolutely greater (i.e., concurrent vectors 
are also excluded) than the replica's one, `valueTS`, which means the server is behind the client and the reading 
operation can't be executed.
If the previous explained verification doesn't succeed, the reading operation is successfully executed and the value 
is returned to the client.

### Write Operations
The two available write operations follow the same logic. 
1. The first step is to calculate the `operationTS` vector (through the `checkIn()`function), which will be sent to 
the client as the `newTS`. 
   - Note that the `checkIn` function increments the `replicaTS`, creates the `operationTS` and merges it with
`prevTS`.
2. The second step is to ensure that the replica's `valueTS` vector is equal or greater than the client's 
received vector, `prevTS`. This guarantees that the client isn't ahead of the server. To accomplish this, the 
function `updateStable` was created. 
If the previous verification succeeds, the operation is added to the ledger. Then, the stability condition is checked 
before executing the operation. If `isStable() -> True`, the operation is executed using `executeOperation` method,
otherwise the operation remains unstable.

#### Update
For each operation that has been received by the other replica, we start by checking if the `operationTS` is less or equal
(to take in consideration the concurrent vectors) than the `replicaTS`. Then, according to the operation type, it will 
be added to the ledger and executed if is stable, or only executed. A flag, `executeOnly`, is passed to the function to
choose either to add to the ledger and execute or execute only, in case that the operation was already added.

### Third Server
We changed the attribute `numServers` on VectorClock class to 3. We also extended `gossip()` in ServerState class so that
ledger's state is propagated to the remaining two servers.

