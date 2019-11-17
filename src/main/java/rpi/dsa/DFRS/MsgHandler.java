package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.Host;
import rpi.dsa.DFRS.Entity.Message;
import rpi.dsa.DFRS.Entity.Reservation;
import rpi.dsa.DFRS.Utils.Clock;
import rpi.dsa.DFRS.Utils.FileUtils;
import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import static java.lang.System.exit;

public class MsgHandler implements Runnable {

    private String hostName;

    private Host host;

    MsgHandler(String hostName, Host host) {
        this.hostName = hostName;
        this.host = host;
    }

    @Override
    public void run() {
        /* Create socket using the first port in the range*/
        int port = host.getUdpStartPort();
        InetAddress address;
        DatagramSocket socket;
        try {
            address = InetAddress.getByName(host.getIpAddr());
            socket = new DatagramSocket(port, address);
        } catch (IOException e) {
            System.out.println("MsgHandler: Create socket failed");
            exit(0);
            return;
        }
//        System.out.println("[SERVICE] '" + hostName + "' is listening on port " + port);

        while (true) {
            try {
                /* keep waiting for message */
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                socket.receive(datagramPacket);

                /* ================================TEST===================================== */
//                Message message = MsgUtil.deserialize(bytes);
//                System.out.println(message.getRecords());
//                int[][] timetable = message.getTime_table();
//                for (int[] ints : timetable) {
//                    for (int c : ints) {
//                        System.out.print(c + " ");
//                    }
//                    System.out.print("\n");
//                }
                /* ================================TEST===================================== */

                Message message = MsgUtil.deserialize(bytes);
                Integer src_id = message.getSrc();
                List<EventRecord> m_records = message.getRecords();

                /* Handle message */
                switch (message.getType()) {
                    case Constants.NORMAL:
                        int[][] m_timetable = message.getTime_table();
                        handle(src_id, m_records, m_timetable);
                        break;
                    case Constants.SMALL:
                        int[] m_timerow = message.getTime_row();
                        handleSmall(src_id, m_records, m_timerow);
                        break;
                    default:
                        System.out.println("Unknown message");
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException("MsgHandler: Receive message failed");
            }
        }

    }

    private static void handle(Integer src_id, List<EventRecord> m_records, int[][] m_timetable) {
        synchronized (Resource.class) {
            synchronized (Clock.class) {
                /* 1. Get new events for current site */
                int desc_id = Resource.getPid();
                List<EventRecord> newEvents = MsgUtil.getNewEvents(m_records, Clock.getTimeTable(), desc_id);

                /* 2. Add new reservations to current site */
                List<Reservation> newResv = MsgUtil.getNewReservation(newEvents);
                List<Reservation> reservations = Resource.getReservations();
                reservations.addAll(newResv);

                /* 3. Update local time table */
                Clock.updateTimeTable(m_timetable, src_id);

                /* 4. Check reservation conflicts and confirm reservation */
                int[][] timetable = Clock.getTimeTable();
                List<EventRecord> records = Resource.getRecords();
                records.addAll(newEvents);
                MsgUtil.checkConflicts(reservations, records, desc_id);
                MsgUtil.confirm(reservations, records, timetable);

                /* 5. Log truncation */
                MsgUtil.truncate(records, timetable);

                /* 6. Duration */
                FileUtils.saveTimeTableToFile(timetable);
                FileUtils.saveRecordsToFile(records);
                FileUtils.saveResvToFile(reservations);
            }
        }
    }

    private void handleSmall(Integer src_id, List<EventRecord> m_records, int[] m_timerow) {
        synchronized (Resource.class) {
            synchronized (Clock.class) {
                /* 1. Get new events for current site */
                int desc_id = Resource.getPid();
                List<EventRecord> newEvents = MsgUtil.getNewEvents(m_records, Clock.getTimeTable(), desc_id);

                /* 2. Add new reservations to current site */
                List<Reservation> newResv = MsgUtil.getNewReservation(newEvents);
                List<Reservation> reservations = Resource.getReservations();
                reservations.addAll(newResv);

                /* 3. Update local time row */
                Clock.updateTimeRow(m_timerow, src_id);

                /* 4. Check reservation conflicts and confirm reservation */
                int[][] timetable = Clock.getTimeTable();
                List<EventRecord> records = Resource.getRecords();
                records.addAll(newEvents);
                MsgUtil.checkConflicts(reservations, records, desc_id);
                MsgUtil.confirm(reservations, records, timetable);

                /* 5. Log truncation */
                MsgUtil.truncate(records, timetable);

                /* 6. Duration */
                FileUtils.saveTimeTableToFile(timetable);
                FileUtils.saveRecordsToFile(records);
                FileUtils.saveResvToFile(reservations);
            }
        }
    }
}
