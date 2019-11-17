package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.Reservation;
import rpi.dsa.DFRS.Utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class Resource {

    private static int pid;

    private static List<EventRecord> records;

    private static List<Reservation> reservations;

    public static void init(String hostName) {
        pid = Constants.NAME_TO_INDEX.get(hostName);

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

        /* ================================TEST===================================== */
//        pid = Constants.NAME_TO_INDEX.get(hostName);
//        records = new ArrayList<>();
//        reservations = new ArrayList<>();
        /* ================================TEST===================================== */
    }

    public static List<EventRecord> getRecords() {
        return records;
    }

    public static List<Reservation> getReservations() {
        return reservations;
    }

    public static int getPid() {
        return pid;
    }
}
