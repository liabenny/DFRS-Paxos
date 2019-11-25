package rpi.dsa.DFRS.Utils;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.EventType;
import rpi.dsa.DFRS.Entity.Reservation;
import rpi.dsa.DFRS.Roles.Acceptor;

import java.io.*;
import java.util.*;

public class FileUtils {

    public static boolean isExist(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static String readFile(String path) {
        File file = new File(path);
        StringBuilder content = new StringBuilder();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    public static void appendLogToFile(Integer logNum, EventRecord record) {
        if (record == null || record.getReservation() == null) {
            System.out.println("Wrong Record " + record);
            return;
        }
        try {
            FileWriter writer = new FileWriter(Constants.LOG_FILE, true);
            String logEntry = String.format("%d %s %s\n", logNum, record,
                    record.isCheckPoint() ? Constants.IS_CHECKPOINT : Constants.NOT_CHECKPOINT);

            writer.write(logEntry);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TreeMap<Integer, EventRecord> recoverLogFromFile() {
        TreeMap<Integer, EventRecord> logList = new TreeMap<>();
        try {
            FileInputStream inputStream = new FileInputStream(Constants.LOG_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] infos = line.split(" ");
                Integer logNum = Integer.parseInt(infos[0]);
                EventType type = "reserve".equals(infos[1]) ? EventType.RESERVE : EventType.CANCEL;
                String cliName = infos[2];
                List<Integer> flights = MsgUtil.StringToList(infos[3]);
                String hostName = infos[4];
                Reservation reservation = new Reservation(cliName, flights);
                EventRecord record = new EventRecord(type, reservation, hostName);
                if (infos[5].equals(Constants.IS_CHECKPOINT)) {
                    record.setCheckPoint(true);
                }
                logList.put(logNum, record);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logList;
    }

    public static void saveReservationsToFile(List<Reservation> reservations) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Constants.RESV_FILE));
            out.writeObject(reservations);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Reservation> readReservationsFromFile() {
        List<Reservation> reservations = new ArrayList<>();
        File file = new File(Constants.RESV_FILE);

        /* If file not exist, return empty list */
        if (!file.exists()) {
            return reservations;
        }

        /* Otherwise load information from file */
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            //noinspection unchecked
            reservations = (List<Reservation>) in.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    public static void saveAcceptorStateToFile(Map<Integer, Acceptor.AcceptorState> accState) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Constants.ACCEPTOR_FILE));
            out.writeObject(accState);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, Acceptor.AcceptorState> readAcceptorStateFromFile() {
        Map<Integer, Acceptor.AcceptorState> accState = new HashMap<>();
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(Constants.ACCEPTOR_FILE));
            //noinspection unchecked
            accState = (Map<Integer, Acceptor.AcceptorState>) in.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return accState;
    }
}
