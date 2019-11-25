package rpi.dsa.DFRS.Entity;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Reservation implements Serializable {

    private static final long serialVersionUID = -8940279946849104966L;

    private String clientName;

    private List<Integer> flightNums;

    public Reservation(String clientName, List<Integer> flightNums) {
        this.clientName = clientName;
        this.flightNums = flightNums;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public List<Integer> getFlightNums() {
        return flightNums;
    }

    public void setFlightNums(List<Integer> flightNums) {
        this.flightNums = flightNums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(clientName, that.clientName) &&
                Objects.equals(flightNums, that.flightNums);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientName, flightNums);
    }
}
