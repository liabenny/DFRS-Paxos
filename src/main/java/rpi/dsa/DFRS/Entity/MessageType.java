package rpi.dsa.DFRS.Entity;

public enum MessageType {

    PREPARE("Prepare"),

    PROMISE("Promise"),

    PROMISE_NACK("Promise_NACK"),

    PROPOSAL("Proposal"),

    ACK("ACK"),

    NACK("NACK"),

    COMMIT("Commit"),

    QUERY("Query"),

    REPLY("Reply");

    private String operation;

    MessageType(String operation){
        this.operation = operation;
    }

    public String getDesc() {
        return this.operation;
    }

}
