package rpi.dsa.DFRS.Entity;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {

    private static final long serialVersionUID = -913063426662718136L;

    private Integer src;

    private List<EventRecord> records;

    private int[][] time_table;

    private int[] time_row;

    private Integer type;

    public Message(Integer src, List<EventRecord> records, int[][] time_table, Integer type) {
        this.src = src;
        this.records = records;
        this.time_table = time_table;
        this.type = type;
    }

    public Message(Integer src, List<EventRecord> records, int[] time_row, Integer type) {
        this.src = src;
        this.records = records;
        this.time_row = time_row;
        this.type = type;
    }

    public List<EventRecord> getRecords() {
        return records;
    }

    public int[][] getTime_table() {
        return time_table;
    }

    public Integer getSrc() {
        return src;
    }

    public int[] getTime_row() {
        return time_row;
    }

    public Integer getType() {
        return type;
    }
}
