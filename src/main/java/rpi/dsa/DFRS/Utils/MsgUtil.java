package rpi.dsa.DFRS.Utils;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.EventType;
import rpi.dsa.DFRS.Entity.Message;
import rpi.dsa.DFRS.Entity.Reservation;

import java.io.*;
import java.util.*;

public class MsgUtil {

    public static String ListToString(List<Integer> list) {
        if (list.isEmpty()) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        str.append(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            str.append(",").append(list.get(i));
        }
        return str.toString();
    }

    public static List<Integer> StringToList(String s) {
        List<Integer> list = new ArrayList<>();
        try {
            for (String num : s.split(",")) {
                Integer value = Integer.parseInt(num);
                list.add(value);
            }
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
        return list;
    }

    public static byte[] serialize(Message message) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(buffer);
            out.writeObject(message);
            bytes = buffer.toByteArray();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static Message deserialize(byte[] bytes) {
        Message message = null;
        try {
            ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(buffer);
            message = (Message) in.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return message;
    }

    public static boolean hasRec(int[][] timeTable, EventRecord er, int id) {
        return timeTable[id][er.getProcessId()] >= er.getTime();
    }

    public static boolean allHaveRec(int[][] timeTable, EventRecord er) {
        for (int[] ints : timeTable) {
            if (ints[er.getProcessId()] < er.getTime()) {
                return false;
            }
        }
        return true;
    }

    public static List<EventRecord> getNewEvents(List<EventRecord> records, int[][] timetable, Integer site_id) {
        List<EventRecord> newEvents = new ArrayList<>();
        if (records == null) {
            return newEvents;
        }
        for (EventRecord record : records) {
            if (!hasRec(timetable, record, site_id)) {
                newEvents.add(record);
            }
        }
        return newEvents;
    }

    public static List<Reservation> getNewReservation(List<EventRecord> records) {
        List<Reservation> new_resv = new ArrayList<>();
        for (EventRecord record : records) {
            if (record.getType() == EventType.INS) {
                new_resv.add(record.getReservation());
            } else if (record.getType() == EventType.DEL) {
                new_resv.remove(record.getReservation());
            }
        }
        return new_resv;
    }

    public static void truncate(List<EventRecord> records, int[][] timetable) {
        /* For process i, all the events before(equal to) temp[i] are known by other processes */
        int N = timetable.length;
        int[] temp = new int[N];
        for (int i = 0; i < N; i++) {
            int min = Integer.MAX_VALUE;
            for (int j = 0; j < N; j++) {
                min = Math.min(min, timetable[j][i]);
            }
            temp[i] = min;
        }

        List<EventRecord> delete = new ArrayList<>();
        for (EventRecord record : records) {
            int pid = record.getProcessId();
            int time = record.getTime();
            if (temp[pid] >= time) {
                delete.add(record);
            }
        }
        records.removeAll(delete);
    }

    public static void checkConflicts(List<Reservation> reservations, List<EventRecord> records, int site_id) {
        /* Get the reservation list for every flight */
        Map<Integer, List<Reservation>> flightResv = new HashMap<>(20);
        for (Reservation reservation : reservations) {
            List<Integer> flights = reservation.getFlight_nums();
            for (Integer flight : flights) {
                if (!flightResv.containsKey(flight)) {
                    List<Reservation> resv = new ArrayList<>();
                    flightResv.put(flight, resv);
                }
                flightResv.get(flight).add(reservation);
            }
        }

        /* Check conflict reservations and add them into set */
        Set<Reservation> delete = new HashSet<>();
        for (Map.Entry<Integer, List<Reservation>> entry : flightResv.entrySet()) {
            List<Reservation> resvList = entry.getValue();
            // exclude reservations that have been deleted
            resvList.removeAll(delete);
            if (resvList.size() <= 2) {
                continue;
            }
            PriorityQueue<Reservation> pq = new PriorityQueue<>((r1, r2) -> {
                if (r1.getStatus() != r2.getStatus()) {
                    return r1.getStatus() - r2.getStatus();
                } else {
                    return r1.getClient_name().compareTo(r2.getClient_name());
                }
            });
            pq.addAll(resvList);
            // exclude available seats
            pq.poll();
            pq.poll();
            while (!pq.isEmpty()) {
                Reservation resv = pq.poll();
                delete.add(resv);
            }
        }

        /* Generate DELETE events for conflict reservations */
        List<EventRecord> delRecords = new ArrayList<>();
        for (Reservation resv : delete) {
            int time = Clock.increment();
            EventRecord record = new EventRecord(EventType.DEL, resv, time, site_id);
            delRecords.add(record);
        }
        reservations.removeAll(delete);
        records.addAll(delRecords);
    }

    public static void confirm(List<Reservation> reservations, List<EventRecord> records, int[][] timetable) {
        /* For process i, all the events before(equal to) temp[i] are known by other processes */
        int N = timetable.length;
        int[] temp = new int[N];
        for (int i = 0; i < N; i++) {
            int min = Integer.MAX_VALUE;
            for (int j = 0; j < N; j++) {
                min = Math.min(min, timetable[j][i]);
            }
            temp[i] = min;
        }

        for (EventRecord record : records) {
            Reservation reservation = record.getReservation();
            int pid = record.getProcessId();
            int time = record.getTime();
            if (reservations.contains(reservation)) {
                if (temp[pid] >= time) {
                    reservation.setStatus(Constants.COMFIRMED);
                }
            }
        }
    }


}
