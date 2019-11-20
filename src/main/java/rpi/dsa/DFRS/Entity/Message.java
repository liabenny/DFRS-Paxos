package rpi.dsa.DFRS.Entity;

import java.io.Serializable;
import java.util.List;


public enum MessageType{
    PREPARE, PROMISE, PROMISE_NACK, ACCEPT, ACCEPT_NACK, ACCEPTED
}


public class Message implements Serializable {

    private MessageType type;

    private Integer num;

    private EventRecord value;

    private Message() {
    }

    public MessageType getType() {
        return type;
    }

    private void setType(MessageType type) {
        this.type = type;
    }

    public Integer getNum() {
        return num;
    }

    private void setNum(Integer num) {
        this.num = num;
    }

    public EventRecord getValue() {
        return value;
    }

    private void setValue(EventRecord value) {
        this.value = value;
    }

    public static Message prepare(Integer prepareNum) {
        Message message = new Message();
        message.setType(MessageType.PREPARE);
        message.setNum(prepareNum);
        return message;
    }

    public static Message promise(Integer accNum, EventRecord accValue) {
        Message message = new Message();
        message.setType(MessageType.PROMISE);
        message.setNum(accNum);
        message.setValue(accValue);
        return message;
    }

    public static Message promise_nack(Integer nackNum) {
        Message message = new Message();
        message.setType(MessageType.PROMISE_NACK);
        message.setNum(nackNum);
        return message;
    }

    public static Message proposal(Integer proposeNum, EventRecord proposeValue) {
        Message message = new Message();
        message.setType(MessageType.PROPOSAL);
        message.setNum(proposeNum);
        message.setValue(proposeValue);
        return message;
    }

    public static Message ack(Integer ackNum) {
        Message message = new Message();
        message.setType(MessageType.ACK);
        message.setNum(ackNum);
        return message;
    }

    public static Message nack(Integer nackNum) {
        Message message = new Message();
        message.setType(MessageType.NACK);
        message.setNum(nackNum);
        return message;
    }

    public static Message query() {
        Message message = new Message();
        message.setType(MessageType.QUERY);
        return message;
    }

    public static Message reply(Integer maxLogSlot) {
        Message message = new Message();
        message.setType(MessageType.REPLY);
        message.setNum(maxLogSlot);
        return message;
    }

    public static Message commit(EventRecord commitValue) {
        Message message = new Message();
        message.setType(MessageType.COMMIT);
        message.setValue(commitValue);
        return message;
    }

}
