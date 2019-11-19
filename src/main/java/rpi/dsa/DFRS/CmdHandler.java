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

    static void reserve(String[] args) {
        /* 1. Check parameter */
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
        for (Integer flight : flights) {
            if (flight < Constants.MIN_FLIGHT || flight > Constants.MAX_FLIGHT) {
                System.out.println("Invalid parameters.");
                System.out.println("<CSV_list_of_flight_numbers> ranges from " + Constants.MIN_FLIGHT + " to " + Constants.MAX_FLIGHT);
                return;
            }
        }
        // TODO handle reserve
    }

    static void cancel(String[] args) {
        /* 1. Check parameters */
        if (args.length != 2) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: cancel <client_name>");
            return;
        }
        // TODO handle cancel
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
