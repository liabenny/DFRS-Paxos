package rpi.dsa.DFRS.Entity;

import java.io.Serializable;

public class EventRecord implements Serializable {

    private static final long serialVersionUID = -1327560890542781086L;

    private EventType type;

    private Reservation reservation;

    private int seqNum;

    public EventRecord(EventType type, Reservation reservation, int seqNum) {
        this.type = type;
        this.reservation = reservation;
        this.seqNum = seqNum;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
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

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    @Override
    public String toString() {
        return "EventRecord{" +
                "type=" + type +
                ", reservation=" + reservation +
                ", seqNum=" + seqNum +
                '}';
    }
}
