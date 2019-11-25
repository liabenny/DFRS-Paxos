package rpi.dsa.DFRS.Roles;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.EventType;
import rpi.dsa.DFRS.Entity.Reservation;
import rpi.dsa.DFRS.Service;
import rpi.dsa.DFRS.Utils.FileUtils;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Learner implements Runnable {

    private static TreeMap<Integer, EventRecord> logList;

    private static List<Reservation> reservations;

    private final BlockingQueue<Map.Entry<Integer, EventRecord>> queue;

    public Learner(BlockingQueue<Map.Entry<Integer, EventRecord>> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Map.Entry<Integer, EventRecord> entry = queue.take();
                Integer logNum = entry.getKey();
                EventRecord record = entry.getValue();
                saveCheckPoint(logNum, record);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveCheckPoint(Integer logNum, EventRecord record) {
        /* 1. Add all log numbers relate with the checkpoint into list */
        List<Integer> checkList = new ArrayList<>();
        for (int i = 1; i < Constants.CHECK_POINT_INTERVAL; i++) {
            checkList.add(logNum - i);
        }

        /* 2. Keeping filling the holes until there is no hole */
        while (!checkList.isEmpty()) {
            List<Integer> tmp = new ArrayList<>();
            for (Integer curLogNum : checkList) {
                if (logList.containsKey(curLogNum)) {
                    tmp.add(curLogNum);
                    continue;
                }
                synchronized (Proposer.class) {
                    Proposer proposer = new Proposer();
                    proposer.request(curLogNum, null);
                }
            }
            checkList.removeAll(tmp);

            // Wait time for learner to save handle new commit message.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /* 3. Save reservations and log entry to file */
        record.setCheckPoint(true);
        FileUtils.saveReservationsToFile(reservations);
        FileUtils.appendLogToFile(logNum, record);
    }

    public static void learnProposal(Integer logNum, EventRecord record,
                                     BlockingQueue<Map.Entry<Integer, EventRecord>> queue) {
        /* Already know about the entry, ignore it */
        if (logList.containsKey(logNum)) {
            return;
        }

        /* Learn new log entry */
        logList.put(logNum, record);
        System.out.println("<DEBUG> current log list: " + logList);

        if (Service.isUnderRecovery()) {
            /* When site is recovering, do not update reservations and checkpoint */
            Service.lostLogList.put(logNum, record);

        } else {
            if (record.getType().equals(EventType.RESERVE)) {
                Reservation reservation = record.getReservation();
                reservations.add(reservation);
            } else {
                Reservation reservation = record.getReservation();
                reservations.remove(reservation);
            }

            if (logNum % Constants.CHECK_POINT_INTERVAL == 0) {
                /* When reach check point, use another thread to fill the holes and save check point */
                Map.Entry<Integer, EventRecord> entry = new AbstractMap.SimpleEntry<>(logNum, record);
                try {
                    queue.put(entry);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                /* If not check point, then save new log entry in file */
                FileUtils.appendLogToFile(logNum, record);
            }
        }
    }

    public static void init() {
        /* Read Log file or initialize */
        if (FileUtils.isExist(Constants.LOG_FILE)) {
            // Recover data structure from file
            logList = FileUtils.recoverLogFromFile();
            reservations = FileUtils.readReservationsFromFile();
            Service.setUnderRecovery(true);
        } else {
            // use TreeMap to keep the order of log entry based on keys
            logList = new TreeMap<>();
            reservations = new ArrayList<>();
            Service.setUnderRecovery(false);
        }
    }

    /**
     * Update log information until there is no hole in the log.
     * Fill in the holes based on ascending order on log number
     *
     * @param maxLogNum max log entry number
     */
    public static void fillHoles(Integer maxLogNum) {
        int logNum;
        while ((logNum = getLostLogNum(maxLogNum)) > 0) {
            System.out.println("<DEBUG> fill hole in logNum: " + logNum);
            synchronized (Proposer.class) {
                Proposer proposer = new Proposer();
                proposer.request(logNum, null);
            }

            // Wait time for learner to save handle new commit message.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Find the next log number for new event (starts from 1).
     * Log number increment by 1 every time.
     *
     * @return log number
     */
    public static int getNextLogNum() {
        /* log number start from 1 */
        if (logList.isEmpty()) {
            return 1;
        }

        /* Fill in holes in the log */
        int maxLogNum = getMaxLogNum();
        System.out.println("<DEBUG> maxLogNum: " + maxLogNum);
        fillHoles(maxLogNum);

        return logList.keySet().size() + 1;
    }

    /**
     * Find the log number for any hole in the log.
     * If cannot find any hole in the log, then return -1.
     *
     * @param maxLogNum max log entry number
     * @return log number of hole
     */
    private static int getLostLogNum(Integer maxLogNum) {
        System.out.println("<DEBUG> current log list (Learner): " + logList);
        System.out.printf("<DEBUG> maxLogNum(Learner): %d\n", maxLogNum);
        for (int logNum = 1; logNum <= maxLogNum; logNum++) {
            if (!logList.containsKey(logNum)) {
                return logNum;
            }
        }
        return -1;
    }

    /**
     * Get the max log number in the log list.
     *
     * @return max log number
     */
    public static int getMaxLogNum() {
        Integer maxLogNum = logList.descendingMap().firstKey();
        if (maxLogNum == null) {
            return 0;
        }
        return maxLogNum;
    }

    public static TreeMap<Integer, EventRecord> getLogList() {
        return logList;
    }

    public static List<Reservation> getReservations() {
        return reservations;
    }

}
