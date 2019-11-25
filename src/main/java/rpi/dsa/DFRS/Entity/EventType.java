package rpi.dsa.DFRS.Entity;

public enum EventType {
    RESERVE("reserve"),

    CANCEL("cancel");

    private String operation;

    EventType(String operation){
        this.operation = operation;
    }

    public String getDesc() {
        return this.operation;
    }
}
