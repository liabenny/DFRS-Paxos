package rpi.dsa.DFRS.Roles;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.Reservation;
import rpi.dsa.DFRS.Utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class Learner {

    private static List<EventRecord> records;

    private static List<Reservation> reservations;

    public static void init(String hostName) {

        /* Read Log file or initialize */
        if (FileUtils.isExist(Constants.LOG_FILE)) {
            records = FileUtils.readRecordsFromFlie();
        } else {
            records = new ArrayList<>();
        }

        /* Read reservations file or initialize */
        if (FileUtils.isExist(Constants.RESV_FILE)) {
            reservations = FileUtils.readResvFromFile();
        } else {
            reservations = new ArrayList<>();
        }
    }

    public static void checkHoles() {
        //TODO finish holes checking
    }

    public static void learnProposal(EventRecord proposal) {
        Reservation reservation = proposal.getReservation();
        reservations.add(reservation);
        records.add(proposal);
        //TODO save new log entry in stable memory.
    }

    public static List<EventRecord> getRecords() {
        return records;
    }

    public static List<Reservation> getReservations() {
        return reservations;
    }

}
