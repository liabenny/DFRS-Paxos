package rpi.dsa.DFRS.Entity;

public enum EventType {
    INS("Reserve"),

    DEL("Cancel"),

    INVALID("Invalid");

    private String operation;

    EventType(String operation){
        this.operation = operation;
    }

    public String getDesc() {
        return this.operation;
    }
}
