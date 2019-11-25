package rpi.dsa.DFRS.Entity;

import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.Serializable;

public class EventRecord implements Serializable {

    private static final long serialVersionUID = -1327560890542781086L;

    private EventType type;

    private Reservation reservation;

    private boolean checkPoint;

    public EventRecord(EventType type, Reservation reservation) {
        this.checkPoint = false;
        this.type = type;
        this.reservation = reservation;
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

    @Override
    public String toString() {
        return String.format("%s %s %s", type.getDesc(),
                reservation != null ? reservation.getClientName() : "null",
                reservation != null ? MsgUtil.ListToString(reservation.getFlightNums()) : "null");
    }
}
