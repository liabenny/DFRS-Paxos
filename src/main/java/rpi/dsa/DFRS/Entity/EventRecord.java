package rpi.dsa.DFRS.Entity;

import java.io.Serializable;

public class EventRecord implements Serializable {

    private static final long serialVersionUID = -1327560890542781086L;

    private EventType type;

    private Reservation reservation;

    private int time;

    private int processId;

    public EventRecord(EventType type, Reservation reservation, int time, int processId) {
        this.type = type;
        this.reservation = reservation;
        this.time = time;
        this.processId = processId;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return "EventRecord{" +
                "type=" + type +
                ", reservation=" + reservation +
                ", time=" + time +
                ", processId=" + processId +
                '}';
    }
}
