package rpi.dsa.DFRS.Utils;

import rpi.dsa.DFRS.Constants.Constants;

public class Clock {

    private static int[][] timeTable;

    private static int N;

    private static int pid;

    private static int count;

    public static void init(String hostName) {
        pid = Constants.NAME_TO_INDEX.get(hostName);
        int[][] temp = null;
        if (FileUtils.isExist(Constants.TIME_FILE)) {
            temp = FileUtils.readTimeTableFromFile();

        }
        if (temp == null) {
            N = Constants.HOSTS.keySet().size();
            timeTable = new int[N][N];
            count = 0;
        } else {
            timeTable = temp;
            count = timeTable[pid][pid];
        }

        /* ================================TEST===================================== */
//        pid = Constants.NAME_TO_INDEX.get(hostName);
//        N = Constants.HOSTS.keySet().size();
//        timeTable = new int[N][N];
//        count = 0;
        /* ================================TEST===================================== */

    }

    public static int increment() {
        count++;
        timeTable[pid][pid] = count;
        return count;
    }

    public static int[][] getTimeTable() {
        return timeTable;
    }

    public static void updateTimeTable(int[][] newTimeTable, int src_id) {
        /* Direct */
        for (int i = 0; i < N; i++) {
            timeTable[pid][i] = Math.max(timeTable[pid][i], newTimeTable[src_id][i]);
        }

        /* Indirect */
        for (int i = 0; i < N; i++) {
            if (i == pid) {
                continue;
            }
            for (int j = 0; j < N; j++) {
                timeTable[i][j] = Math.max(timeTable[i][j], newTimeTable[i][j]);
            }
        }
    }

    public static void updateTimeRow(int[] newTimeRow, int src_id) {
        for (int i = 0; i < N; i++) {
            timeTable[pid][i] = Math.max(timeTable[pid][i], newTimeRow[i]);
        }

        for (int i = 0; i < N; i++) {
            timeTable[src_id][i] = Math.max(timeTable[src_id][i], newTimeRow[i]);
        }
    }
}
