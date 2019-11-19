package rpi.dsa.DFRS.Entity;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Reservation implements Serializable {

    private static final long serialVersionUID = -8940279946849104966L;

    private String client_name;

    private List<Integer> flight_nums;

    public Reservation(String client_name, List<Integer> flight_nums) {
        this.client_name = client_name;
        this.flight_nums = flight_nums;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public List<Integer> getFlight_nums() {
        return flight_nums;
    }

    public void setFlight_nums(List<Integer> flight_nums) {
        this.flight_nums = flight_nums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(client_name, that.client_name) &&
                Objects.equals(flight_nums, that.flight_nums);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client_name, flight_nums);
    }
}
