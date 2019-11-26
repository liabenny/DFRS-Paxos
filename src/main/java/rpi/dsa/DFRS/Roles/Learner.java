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

    private static List<Reservation> reservationsBak;

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

                int nextCheckPoint = getNextCheckPoint();
                if (logNum == nextCheckPoint) {
                    /* Detest holes and save checkpoint */
                    saveCheckPoint(logNum, record);
                } else if (logNum > nextCheckPoint) {
                    /* Detect holes and recover checkpoint */
                    recoverCheckPoint(logNum, record, nextCheckPoint);
                } else {
                    /* The checkpoint has already been saved.
                     * Only update the reservationsBak, since reservations has already updated before */
                    updateReservations(logNum, record, reservationsBak);

                    /* Save new log entry in stable storage */
                    FileUtils.appendLogToFile(logNum, record);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveCheckPoint(Integer logNum, EventRecord record) {
        /* 1. Fill holes before the checkpoint */
        fillHoles(logNum);

        /* 2. Save reservations and log entry in stable storage */
        record.setCheckPoint(true);
        FileUtils.saveReservationsToFile(reservationsBak);
        FileUtils.appendLogToFile(logNum, record);
    }

    private void recoverCheckPoint(Integer logNum, EventRecord record, Integer checkPoint) {
        /* 1. Fill holes before the checkpoint */
        fillHoles(checkPoint);

        /* 2. Save reservations and checkpoint in stable storage */
        EventRecord checkPointRecord = logList.get(checkPoint);
        checkPointRecord.setCheckPoint(true);
        FileUtils.saveReservationsToFile(reservationsBak);
        FileUtils.appendLogToFile(checkPoint, checkPointRecord);

        /* 3. Check whether there are other missed checkpoints */
        Map.Entry<Integer, EventRecord> entry = new AbstractMap.SimpleEntry<>(logNum, record);
        try {
            queue.put(entry);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            int nextCheckPoint = getNextCheckPoint();
            updateReservations(logNum, record, reservations);

            if (logNum == nextCheckPoint) {
                /* Update reservationBak when logNum <= next checkpoint*/
                updateReservations(logNum, record, reservationsBak);

                int maxLogNum = getMaxLogNum();
                if (logNum == maxLogNum) {
                    /* There is no larger log number handling checkpoint,
                     * then notify the Learner to start handle checkpoint */
                    Map.Entry<Integer, EventRecord> entry = new AbstractMap.SimpleEntry<>(logNum, record);
                    try {
                        queue.put(entry);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } else if (logNum > nextCheckPoint) {
                /* When exceed checkpoint, notify the Learner to start handle checkpoint */
                Map.Entry<Integer, EventRecord> entry = new AbstractMap.SimpleEntry<>(logNum, record);
                try {
                    queue.put(entry);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                /* Update reservationBak when logNum <= next checkpoint*/
                updateReservations(logNum, record, reservationsBak);

                /* Save new log entry in stable storage */
                FileUtils.appendLogToFile(logNum, record);
            }
        }
    }

    public static void init() {
        /* Read Log file or initialize */
        if (FileUtils.isExist(Constants.LOG_FILE)) {
            // Recover data structure from file
            logList = FileUtils.recoverLogFromFile();
            reservationsBak = FileUtils.readReservationsFromFile();
            Service.setUnderRecovery(true);
        } else {
            // use TreeMap to keep the order of log entry based on keys
            logList = new TreeMap<>();
            reservations = new ArrayList<>();
            reservationsBak = new ArrayList<>();
            Service.setUnderRecovery(false);
        }
    }

    public static void recoverReservations() {
        reservations = new ArrayList<>(reservationsBak);
    }

    public static void updateReservations(Integer logNum, EventRecord record, List<Reservation> reservationList) {
        if (record.getType().equals(EventType.RESERVE)) {
            /* Handle "reserve" commit */
            Reservation reservation = record.getReservation();
            int maxLogNum = getMaxLogNum();
            boolean hasCancel = false;

            for (int curLogNum = logNum + 1; curLogNum <= maxLogNum; curLogNum++) {
                EventRecord curRecord = logList.get(curLogNum);
                if (curRecord == null) {
                    continue;
                }

                // If there is "cancel" log after current log number, then do not update reservations.
                if (curRecord.getType().equals(EventType.CANCEL) && curRecord.getReservation().equals(reservation)) {
                    hasCancel = true;
                    break;
                }
            }

            if (!hasCancel) {
                reservationList.add(reservation);
            }

        } else {
            /* Handle "cancel" commit */
            Reservation reservation = record.getReservation();
            int maxLogNum = getMaxLogNum();
            boolean hasReserve = false;

            for (int curLogNum = logNum + 1; curLogNum <= maxLogNum; curLogNum++) {
                EventRecord curRecord = logList.get(curLogNum);
                if (curRecord == null) {
                    continue;
                }

                // If there is "cancel" log after current log number, then do not update reservations.
                if (curRecord.getType().equals(EventType.RESERVE) && curRecord.getReservation().equals(reservation)) {
                    hasReserve = true;
                    break;
                }
            }

            if (!hasReserve) {
                reservationList.remove(reservation);
            }
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
                proposer.request(logNum, null, true);
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
     * Return the winning site for a given log position.
     */
    public static String winningSite(int logNumber) {
        String winning_site = "";
        if (logList.containsKey(logNumber)) {
            EventRecord record = logList.get(logNumber);
            winning_site = record.getProposerHost();
        }
        return winning_site;
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

    /**
     * Get the log number of checkpoint after the most recent checkpoint
     *
     * @return log number
     */
    public static int getNextCheckPoint() {
        Integer maxCheckPointLogNum = 0;
        for (Map.Entry<Integer, EventRecord> entry : logList.descendingMap().entrySet()) {
            EventRecord curRecord = entry.getValue();
            if (curRecord.isCheckPoint()) {
                maxCheckPointLogNum = entry.getKey();
                break;
            }
        }
        return maxCheckPointLogNum + Constants.CHECK_POINT_INTERVAL;
    }

    public static TreeMap<Integer, EventRecord> getLogList() {
        return logList;
    }

    public static List<Reservation> getReservations() {
        return reservations;
    }

    public static List<Reservation> getReservationsBak() {
        return reservationsBak;
    }
}
