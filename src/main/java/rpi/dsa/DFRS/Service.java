package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.*;
import rpi.dsa.DFRS.Roles.Acceptor;
import rpi.dsa.DFRS.Roles.Learner;
import rpi.dsa.DFRS.Roles.Proposer;
import rpi.dsa.DFRS.Utils.FileUtils;

import static java.lang.System.exit;
import static rpi.dsa.DFRS.Constants.Constants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Service {

    public static Host myHost;

    public static String hostName;

    public static Map<Integer, EventRecord> lostLogList = new TreeMap<>();

    // Mark the current status of site
    // True  - On recovering
    // False - On working
    private static boolean underRecovery;

    public static boolean isUnderRecovery() {
        return underRecovery;
    }

    public static void setUnderRecovery(boolean underRecovery) {
        Service.underRecovery = underRecovery;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid Parameter.");
            System.out.println("Please specify site ID.");
            return;
        }
        hostName = args[0];
        init();
        start();
    }

    private static void init() {
        /* 1. Resolve local host configuration from knownhost.json */
        for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
            Host host = entry.getValue();
            String site = entry.getKey();
            if (site.equals(Service.hostName)) {
                myHost = host;
                break;
            }
        }

        if (Service.hostName == null) {
            throw new RuntimeException("Unknown local host");
        }

        /* 2. Init dictionary and event record */
        Learner.init();

        /* 3. Start Acceptor and Learner threads */
        listen();

        /* 4. Recover site */
        if (underRecovery) {
            recover();
        }
    }

    private static void listen() {
        BlockingQueue<Map.Entry<Integer, EventRecord>> queue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAP, true);

        Learner learner = new Learner(queue);
        Thread learnerThread = new Thread(learner);
        learnerThread.start();

        Acceptor acceptor = new Acceptor(queue);
        Thread acceptorThread = new Thread(acceptor);
        acceptorThread.start();
    }

    private static void recover() {
        TreeMap<Integer, EventRecord> logList = Learner.getLogList();
        List<Reservation> reservationsBak = Learner.getReservationsBak();

        /* 1. Query the max log entry number */
        Proposer proposer = new Proposer();
        int maxLogNum = proposer.query();

        /* 2. Read the log from end, stop when reach first checkpoint */
        Integer lastCheckPoint = 0;
        for (Map.Entry<Integer, EventRecord> log : logList.descendingMap().entrySet()) {
            EventRecord record = log.getValue();
            if (record.isCheckPoint()) {
                lastCheckPoint = log.getKey();
                break;
            }
        }

        /* 3. Fill in the holes before maxLogNum */
        Learner.fillHoles(maxLogNum);

        /* 4. Replay log from last checkpoint to the end */
        for (int curLogNum = lastCheckPoint + 1; curLogNum <= maxLogNum; curLogNum++) {
            EventRecord record = logList.get(curLogNum);

            /* Replay log and update reservation */
            if (record.getType().equals(EventType.RESERVE)) {
                reservationsBak.add(record.getReservation());
            } else {
                reservationsBak.remove(record.getReservation());
            }

            if (curLogNum % CHECK_POINT_INTERVAL == 0) {

                /* Reach new checkpoint, save reservations to file */
                FileUtils.saveReservationsToFile(reservationsBak);
                record.setCheckPoint(true);
                FileUtils.appendLogToFile(curLogNum, record);

            } else if (Service.lostLogList.containsKey(curLogNum)) {

                /* Current log entry is lost, save the log entry to file */
                FileUtils.appendLogToFile(curLogNum, record);
            }
        }

        /* 5. Set the flag when finish recovery */
        Learner.recoverReservations();
        setUnderRecovery(false);
    }

    private static void start() {
        InputStreamReader inputStreamReader = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        while (true) {
            try {
                /* Waiting input from users */
                String cmd = reader.readLine();
                if (cmd == null || cmd.length() == 0) {
                    continue;
                }

                /* Terminate program */
                if (QUIT.equals(cmd)) {
                    inputStreamReader.close();
                    exit(0);
                }

                /* Handle commands */
                String[] args = cmd.split(" ");
                switch (args[0]) {
                    case RESERVE:
                        CmdHandler.reserve(args);
                        break;
                    case CANCEL:
                        CmdHandler.cancel(args);
                        break;
                    case VIEW:
                        CmdHandler.view(args);
                        break;
                    case LOG:
                        CmdHandler.log(args);
                        break;
                    default:
                        System.out.println("Unknown command");
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException("Service: Read command failed");
            }
        }
    }
}


