package rpi.dsa.DFRS.Entity;

import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.Serializable;
import java.util.Objects;

public class EventRecord implements Serializable {

    private static final long serialVersionUID = -1327560890542781086L;

    private EventType type;

    private Reservation reservation;

    private boolean checkPoint;

    private String proposerHost;

    public EventRecord(EventType type, Reservation reservation, String host) {
        this.checkPoint = false;
        this.type = type;
        this.reservation = reservation;
        this.proposerHost = host;
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

    public boolean isCheckPoint() {
        return checkPoint;
    }

    public void setCheckPoint(boolean checkPoint) {
        this.checkPoint = checkPoint;
    }

    public String getProposerHost() {
        return proposerHost;
    }

    public void setProposerHost(String proposerHost) {
        this.proposerHost = proposerHost;
    }


    @Override
    public String toString() {
        return String.format("%s %s %s %s", type.getDesc(),
                reservation != null ? reservation.getClientName() : "null",
                reservation != null ? MsgUtil.ListToString(reservation.getFlightNums()) : "null",
                proposerHost);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventRecord record = (EventRecord) o;
        return type == record.type &&
                Objects.equals(reservation, record.reservation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, reservation);
    }
}
