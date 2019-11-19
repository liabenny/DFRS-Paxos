package rpi.dsa.DFRS.Entity;

public enum MessageType {

    PREPARE("Prepare"),

    PROMISE("Promise"),

    PROPOSAL("Proposal"),

    ACK("ACK"),

    NACK("NACK"),

    QUERY("Query"),

    REPLY("Reply"),

    COMMIT("Commit");

    private String operation;

    MessageType(String operation){
        this.operation = operation;
    }

    public String getDesc() {
        return this.operation;
    }

}
