package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.*;
import rpi.dsa.DFRS.Utils.FileUtils;
import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

class CmdHandler {

    static EventRecord reserve(String[] args) {
        /* 1. Create our output event record, initially invalid */
        Reservation invalid_res = new Reservation("", Collections.emptyList());
        EventRecord invalid_event = new EventRecord(EventType.INVALID, invalid_res, -1);

        /* 2. Check parameter */
        if (args.length != 3) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: reserve <client_name> <CSV_list_of_flight_numbers>");
            return invalid_event;
        }
        String client_name = args[1];
        String flight_str = args[2];
        List<Integer> flights = MsgUtil.StringToList(flight_str);
        Reservation res = new Reservation(client_name, flights);
        if (flights.isEmpty()) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: reserve <client_name> <CSV_list_of_flight_numbers>");
            return invalid_event;
        }
        for (Integer flight : flights) {
            if (flight < Constants.MIN_FLIGHT || flight > Constants.MAX_FLIGHT) {
                System.out.println("Invalid parameters.");
                System.out.println("<CSV_list_of_flight_numbers> ranges from " + Constants.MIN_FLIGHT + " to " + Constants.MAX_FLIGHT);
                return invalid_event;
            }
        }
        /* 3. Return our new reservation event. */
        // TODO: Give a proper value to sequence number
        return new EventRecord(EventType.INS, res, 1);
    }

    static EventRecord cancel(String[] args) {
        /* 1. Check parameters */
        if (args.length != 2) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: cancel <client_name>");
            Reservation invalid_res = new Reservation("", Collections.emptyList());
            return new EventRecord(EventType.INVALID, invalid_res, -1);
        }

        /* 2. Return a cancel event record. */
        // TODO proper sequence number
        Reservation res = new Reservation(args[1], Collections.emptyList());
        return new EventRecord(EventType.DEL, res, 1);

    }

    static void view(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: view");
            return;
        }
        // TODO handle view
    }

    static void log(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: log");
            return;
        }
        // TODO handle log
    }
}
