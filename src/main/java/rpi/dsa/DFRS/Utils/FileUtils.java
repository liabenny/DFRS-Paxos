package rpi.dsa.DFRS.Utils;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.Reservation;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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

    public static void saveRecordsToFile(List<EventRecord> records) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Constants.LOG_FILE));
            out.writeObject(records);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<EventRecord> readRecordsFromFlie() {
        List<EventRecord> records = new ArrayList<>();
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(Constants.LOG_FILE));
            //noinspection unchecked
            records = (List<EventRecord>) in.readObject();
            in.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static void saveResvToFile(List<Reservation> reservations) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Constants.RESV_FILE));
            out.writeObject(reservations);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Reservation> readResvFromFile() {
        List<Reservation> reservations = new ArrayList<>();
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(Constants.RESV_FILE));
            //noinspection unchecked
            reservations = (List<Reservation>) in.readObject();
            in.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    public static void saveTimeTableToFile(int[][] timeTable) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Constants.TIME_FILE));
            out.writeObject(timeTable);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int[][] readTimeTableFromFile() {
        int[][] timeTable = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(Constants.TIME_FILE));
            timeTable = (int[][]) in.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return timeTable;
    }
}
