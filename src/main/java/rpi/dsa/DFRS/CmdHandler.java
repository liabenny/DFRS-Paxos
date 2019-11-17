package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.*;
import rpi.dsa.DFRS.Utils.Clock;
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

        // - one client can only make one reservation
        // - each flight has only 2 seats
        List<EventRecord> records;
        List<Reservation> resv;
        int[][] timeTable;
        synchronized (Resource.class) {
            Map<Integer, Integer> counter = new HashMap<>(flights.size());
            List<Reservation> reservations = Resource.getReservations();

            /* 2. Check flight availability */
            for (Reservation reservation : reservations) {
                if (reservation.getClient_name().equals(cliName)) {
                    System.out.println("Cannot schedule reservation for " + cliName + ".");
                    return;
                }
                for (int flight : reservation.getFlight_nums()) {
                    int num = counter.getOrDefault(flight, 0);
                    counter.put(flight, ++num);
                }
            }
            for (int flight : flights) {
                int count = counter.getOrDefault(flight, 0);
                if (count >= 2) {
                    System.out.println("Cannot schedule reservation for " + cliName + ".");
                    return;
                }
            }

            /* 3. Make reservation and save log record */
            int time;
            synchronized (Clock.class) {
                time = Clock.increment();
                timeTable = Clock.getTimeTable().clone();
            }
            Reservation reservation = new Reservation(cliName, flights, Constants.PENDING);
            EventRecord eventRecord = new EventRecord(EventType.INS, reservation, time, Resource.getPid());
            Resource.getRecords().add(eventRecord);
            Resource.getReservations().add(reservation);
            records = new ArrayList<>(Resource.getRecords());
            resv = new ArrayList<>(Resource.getReservations());
        }
        FileUtils.saveRecordsToFile(records);
        FileUtils.saveResvToFile(resv);
        FileUtils.saveTimeTableToFile(timeTable);
        System.out.println("Reservation submitted for " + cliName + ".");
    }

    static void cancel(String[] args) {
        /* 1. Check parameters */
        if (args.length != 2) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: cancel <client_name>");
            return;
        }

        String cliName = args[1];
        List<Reservation> resv;
        List<EventRecord> records;
        int[][] timeTable;
        synchronized (Resource.class) {
            /* 2. Check whether reservation is exist */
            List<Reservation> reservations = Resource.getReservations();
            Reservation res = null;
            for (Reservation reservation : reservations) {
                if (reservation.getClient_name().equals(cliName)) {
                    res = reservation;
                    break;
                }
            }
            if (res == null) {
                System.out.println("Reservation does not exist.");
                return;
            }

            /* 3. Cancel reservation and save log record */
            int time;
            synchronized (Clock.class) {
                time = Clock.increment();
                timeTable = Clock.getTimeTable().clone();
            }
            reservations.remove(res);
            EventRecord eventRecord = new EventRecord(EventType.DEL, res, time, Resource.getPid());
            Resource.getRecords().add(eventRecord);
            resv = new ArrayList<>(Resource.getReservations());
            records = new ArrayList<>(Resource.getRecords());
        }
        FileUtils.saveResvToFile(resv);
        FileUtils.saveRecordsToFile(records);
        FileUtils.saveTimeTableToFile(timeTable);
        System.out.println("Reservation for " + cliName + " cancelled.");
    }

    static void view(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: view");
            return;
        }

        synchronized (Resource.class) {
            List<Reservation> reservations = new ArrayList<>(Resource.getReservations());
            reservations.sort(Comparator.comparing(Reservation::getClient_name));
            for (Reservation reservation : reservations) {
                String flights = MsgUtil.ListToString(reservation.getFlight_nums());
                String status = reservation.getStatus() == Constants.PENDING ? "pending" : "confirmed";
                System.out.println(reservation.getClient_name() + " " + flights + " " + status);
            }
        }
    }

    static void log(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: log");
            return;
        }
        synchronized (Resource.class) {
            List<EventRecord> records = Resource.getRecords();
            for (EventRecord record : records) {
                Reservation reservation = record.getReservation();
                String cliName = reservation.getClient_name();
                if (record.getType() == EventType.INS) {
                    String flights = MsgUtil.ListToString(reservation.getFlight_nums());
                    System.out.println("insert " + cliName + " " + flights);
                } else {
                    System.out.println("delete " + cliName);
                }
            }
        }
    }

    static void send(String[] args) {
        /* 1. Check parameters */
        if (args.length != 2) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: send <site_id>");
            return;
        }
        String site = args[1];
        Integer dest_id = Constants.NAME_TO_INDEX.get(site);
        if (dest_id == null) {
            System.out.println("Unknown site");
            return;
        }
        if (dest_id == Resource.getPid()) {
            System.out.println("Cannot send to self");
            return;
        }

        /* 2. Read local data */
        Host dest = Constants.HOSTS.get(site);
        String dest_ip = dest.getIpAddr();
        Integer dest_port = dest.getUdpStartPort();
        List<EventRecord> records;
        int[][] time_table;
        int src_id;
        synchronized (Resource.class) {
            records = new ArrayList<>(Resource.getRecords());
            src_id = Resource.getPid();
        }
        synchronized (Clock.class) {
            time_table = Clock.getTimeTable().clone();
        }
        Host src = Constants.HOSTS.get(Constants.INDEX_TO_NAME.get(Resource.getPid()));
        String src_ip = src.getIpAddr();
        Integer src_port = src.getUdpEndPort();

        /* 3. Send message */
        List<EventRecord> np = new ArrayList<>();
        for (EventRecord record : records) {
            if (!MsgUtil.hasRec(time_table, record, dest_id)) {
                np.add(record);
            }
        }
        Message message = new Message(src_id, np, time_table, Constants.NORMAL);
        SendHandler sender = new SendHandler(src_ip, src_port, src_id);
        sender.send(dest_ip, dest_port, message);
    }

    static void sendAll(String[] args) {
        /* 1. Check parameters */
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: sendall");
            return;
        }

        /* 2.Read local data */
        List<EventRecord> records;
        Integer src_id;
        int[][] time_table;
        synchronized (Resource.class) {
            records = new ArrayList<>(Resource.getRecords());
            src_id = Resource.getPid();
        }
        synchronized (Clock.class) {
            time_table = Clock.getTimeTable().clone();
        }

        /* 3.Send messages to all sites */
        Host src = Constants.HOSTS.get(Constants.INDEX_TO_NAME.get(src_id));
        String src_ip = src.getIpAddr();
        Integer src_port = src.getUdpEndPort();
        List<EventRecord> np = new ArrayList<>();
        for (EventRecord record : records) {
            if (!MsgUtil.allHaveRec(time_table, record)) {
                np.add(record);
            }
        }
        Message message = new Message(src_id, np, time_table, Constants.NORMAL);
        SendHandler sender = new SendHandler(src_ip, src_port, src_id);
        sender.sendAll(message);
    }

    static void smallSend(String[] args) {
        /* 1. check parameters */
        if (args.length != 2) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: smallsend <site_id>");
            return;
        }
        String site = args[1];
        Integer dest_id = Constants.NAME_TO_INDEX.get(site);
        if (dest_id == null) {
            System.out.println("Unknown site");
            return;
        }
        if (dest_id == Resource.getPid()) {
            System.out.println("Cannot send to self");
            return;
        }

        /* 2. Read local data */
        Host dest = Constants.HOSTS.get(site);
        String dest_ip = dest.getIpAddr();
        Integer dest_port = dest.getUdpStartPort();
        List<EventRecord> records;
        int[][] time_table;
        int src_id;
        synchronized (Resource.class) {
            records = new ArrayList<>(Resource.getRecords());
            src_id = Resource.getPid();
        }
        synchronized (Clock.class) {
            time_table = Clock.getTimeTable().clone();
        }
        int[] select_row = time_table[src_id];
        Host src = Constants.HOSTS.get(Constants.INDEX_TO_NAME.get(Resource.getPid()));
        String src_ip = src.getIpAddr();
        Integer src_port = src.getUdpEndPort();

        /* 3. Send message */
        List<EventRecord> np = new ArrayList<>();
        for (EventRecord record : records) {
            if (!MsgUtil.hasRec(time_table, record, dest_id)) {
                np.add(record);
            }
        }
        Message message = new Message(src_id, np, select_row, Constants.SMALL);
        SendHandler sender = new SendHandler(src_ip, src_port, src_id);
        sender.send(dest_ip, dest_port, message);
    }

    static void smallSendAll(String[] args) {
        /* 1. Check parameters */
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: sendall");
            return;
        }

        /* 2.Read local data */
        List<EventRecord> records;
        Integer src_id;
        int[][] time_table;
        synchronized (Resource.class) {
            records = new ArrayList<>(Resource.getRecords());
            src_id = Resource.getPid();
        }
        synchronized (Clock.class) {
            time_table = Clock.getTimeTable().clone();
        }

        /* 3.Send messages to all sites */
        Host src = Constants.HOSTS.get(Constants.INDEX_TO_NAME.get(src_id));
        String src_ip = src.getIpAddr();
        Integer src_port = src.getUdpEndPort();
        List<EventRecord> np = new ArrayList<>();
        for (EventRecord record : records) {
            if (!MsgUtil.allHaveRec(time_table, record)) {
                np.add(record);
            }
        }
        int[] select_row = time_table[src_id];
        Message message = new Message(src_id, np, select_row, Constants.SMALL);
        SendHandler sender = new SendHandler(src_ip, src_port, src_id);
        sender.sendAll(message);
    }

    static void clock(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid parameters.");
            System.out.println("usage: clock");
            return;
        }

        synchronized (Clock.class) {
            int[][] timetable = Clock.getTimeTable();
            for (int[] ints : timetable) {
                for (int c : ints) {
                    System.out.print(c + " ");
                }
                System.out.print("\n");
            }
        }
    }

    static class SendHandler {

        private String src_ip;

        private Integer src_port;

        private Integer src_pid;

        SendHandler(String src_ip, Integer src_port, Integer src_pid) {
            this.src_ip = src_ip;
            this.src_port = src_port;
            this.src_pid = src_pid;
        }

        void send(String dest_ip, Integer dest_port, Message message) {
            DatagramSocket socket = null;
            try {
                InetAddress address = InetAddress.getByName(src_ip);
                socket = new DatagramSocket(src_port, address);
                byte[] buffer = MsgUtil.serialize(message);
                InetAddress dest = InetAddress.getByName(dest_ip);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, dest, dest_port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }

        void sendAll(Message message) {
            byte[] buffer = MsgUtil.serialize(message);
            DatagramSocket socket = null;
            try {
                InetAddress address = InetAddress.getByName(src_ip);
                socket = new DatagramSocket(src_port, address);
                for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
                    Integer dest_id = Constants.NAME_TO_INDEX.get(entry.getKey());
                    if (dest_id.equals(src_pid)) {
                        continue;
                    }
                    Host dest_host = entry.getValue();
                    String dest_ip = dest_host.getIpAddr();
                    Integer dest_port = dest_host.getUdpStartPort();
                    InetAddress dest = InetAddress.getByName(dest_ip);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, dest, dest_port);
                    socket.send(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }
}
