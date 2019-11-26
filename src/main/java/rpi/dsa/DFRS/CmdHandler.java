package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.*;
import rpi.dsa.DFRS.Roles.Learner;
import rpi.dsa.DFRS.Roles.Proposer;
import rpi.dsa.DFRS.Utils.MsgUtil;


import java.util.*;

class CmdHandler {

    static void reserve(String[] args) {
        /* 1. Check format of parameters */
        if (args.length != 3) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: reserve <client_name> <CSV_list_of_flight_numbers>");
            return;
        }
        String cliName = args[1];
        String flight_str = args[2];
        List<Integer> flights = MsgUtil.StringToList(flight_str);
        if (flights.isEmpty()) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: reserve <client_name> <CSV_list_of_flight_numbers>");
            return;
        }

        /* 2. Check validity of flight number */
        for (Integer flight : flights) {
            if (flight < Constants.MIN_FLIGHT || flight > Constants.MAX_FLIGHT) {
                System.out.println("Invalid parameters.");
                System.out.println("<CSV_list_of_flight_numbers> ranges from " + Constants.MIN_FLIGHT + " to " + Constants.MAX_FLIGHT);
                return;
            }
        }

        /* 3. Update the log information and get next log number */
        int logNum = Learner.getNextLogNum();
//        System.out.println("<DEBUG> logNum: " + logNum);

        /* 4. Check flight space and client name */
        List<Reservation> reservations = Learner.getReservations();
        int[] curResvNum = new int[Constants.MAX_FLIGHT + 1];
        for (Reservation reservation : reservations) {
            List<Integer> flightNums = reservation.getFlightNums();
            for (Integer flight : flightNums) {
                curResvNum[flight]++;
            }
            if (reservation.getClientName().equals(cliName)) {
                System.out.println("Cannot schedule reservation for " + cliName + ".");
                return;
            }
        }
        for (Integer flight : flights) {
            if (curResvNum[flight] >= 2) {
                System.out.println("Cannot schedule reservation for " + cliName + ".");
                return;
            }
        }

        /* 5. Start new Synod instance */
        Reservation reservation = new Reservation(cliName, flights);
        EventRecord record = new EventRecord(EventType.RESERVE, reservation, Service.hostName);

        boolean succeed;
        synchronized (Proposer.class) {
            Proposer proposer = new Proposer();
            succeed = proposer.request(logNum, record, false);
        }

        if (succeed) {
            System.out.println("Reservation submitted for " + cliName + ".");
        } else {
            System.out.println("Cannot schedule reservation for " + cliName + ".");
        }
    }

    static void cancel(String[] args) {
        /* 1. Check format of parameters */
        if (args.length != 2) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: cancel <client_name>");
            return;
        }
        String cliName = args[1];

        /* 2. Update the log information and get next log number */
        int logNum = Learner.getNextLogNum();

        /* 3. Check the reservation is exist */
        List<Reservation> reservations = Learner.getReservations();
        Reservation target = null;
        for (Reservation reservation : reservations) {
            if (reservation.getClientName().equals(cliName)) {
                target = reservation;
                break;
            }
        }
        if (target == null) {
            System.out.println("Cannot cancel reservation for " + cliName + ".");
            return;
        }

        /* 4. Start new Synod instance */
        EventRecord record = new EventRecord(EventType.CANCEL, target, Service.hostName);
        boolean succeed;
        synchronized (Proposer.class) {
            Proposer proposer = new Proposer();
            succeed = proposer.request(logNum, record, false);
        }
        if (succeed) {
            System.out.println("Reservation for " + cliName + " cancelled.");
        } else {
            System.out.println("Cannot cancel reservation for " + cliName + ".");
        }
    }

    static void view(String[] args) {
        /* 1. Check format of parameters */
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: view");
            return;
        }

        /* 2. Sort the reservation list */
        List<Reservation> reservations = Learner.getReservations();
        reservations.sort(Comparator.comparing(Reservation::getClientName));

        /* 3. Print reservations to stdout */
        for (Reservation reservation : reservations) {
            String flights = MsgUtil.ListToString(reservation.getFlightNums());
            System.out.println(reservation.getClientName() + " " + flights);
        }
    }

    static void log(String[] args) {
        /* 1. Check format of parameters */
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: log");
            return;
        }

        /* 2. Print log to stdout */
        Map<Integer, EventRecord> logList = Learner.getLogList();
        for (Map.Entry<Integer, EventRecord> log : logList.entrySet()) {
            EventRecord record = log.getValue();
            Reservation reservation = record.getReservation();
            if (record.getType() == EventType.RESERVE) {
                String flights = MsgUtil.ListToString(reservation.getFlightNums());
                System.out.println("reserve " + reservation.getClientName() + " " + flights);
            } else {
                System.out.println("cancel " + reservation.getClientName());
            }
        }
    }
}
