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



}
