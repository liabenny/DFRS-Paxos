package rpi.dsa.DFRS.Entity;

import rpi.dsa.DFRS.Service;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = -3334360323606431729L;

    private String senderName;

    private MessageType type;

    private Integer logNum;

    private Integer propNum;

    private Integer num;

    private EventRecord value;

    private Message() {
        this.senderName = Service.hostName;
    }

    public MessageType getType() {
        return type;
    }

    private void setType(MessageType type) {
        this.type = type;
    }

    public Integer getPropNum() {
        return propNum;
    }

    private void setPropNum(Integer propNum) {
        this.propNum = propNum;
    }

    public Integer getLogNum() {
        return logNum;
    }

    private void setLogNum(Integer logNum) {
        this.logNum = logNum;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public EventRecord getValue() {
        return value;
    }

    private void setValue(EventRecord value) {
        this.value = value;
    }

    public String getSenderName() {
        return senderName;
    }

    /**
     * Generate PREPARE(logNum, prepareNum) message
     *
     * @param logNum     log entry that needs to decide for this Synod execution
     * @param prepareNum propose number for prepare message
     * @return message
     */
    public static Message prepare(Integer logNum, Integer prepareNum) {
        Message message = new Message();
        message.setType(MessageType.PREPARE);
        message.setLogNum(logNum);
        message.setPropNum(prepareNum);
        return message;
    }


    /**
     * Generate PROMISE(propNum, accNum, accValue) message
     *
     * @param accNum   latest accept number
     * @param accValue latest accept value
     * @return message
     */
    public static Message promise(Integer propNum, Integer accNum, EventRecord accValue) {
        Message message = new Message();
        message.setType(MessageType.PROMISE);
        message.setPropNum(propNum);
        message.setNum(accNum);
        message.setValue(accValue);
        return message;
    }


    /**
     * Generate PROPOSAL(logNum, propNum, value) message
     *
     * @param logNum       log entry number
     * @param proposeNum   propose number
     * @param proposeValue EventRecord corresponding to log number
     * @return message
     */
    public static Message proposal(Integer logNum, Integer proposeNum, EventRecord proposeValue) {
        Message message = new Message();
        message.setType(MessageType.PROPOSAL);
        message.setLogNum(logNum);
        message.setPropNum(proposeNum);
        message.setValue(proposeValue);
        return message;
    }


    /**
     * Generate ACK(ackNum) message
     *
     * @param ackNum propose number that was accepted
     * @return message
     */
    public static Message ack(Integer ackNum) {
        Message message = new Message();
        message.setType(MessageType.ACK);
        message.setPropNum(ackNum);
        return message;
    }


    /**
     * Generate NACK(nackNum) message
     *
     * @param nackNum max accept number among Synod instances corresponds to log numbers
     * @return message
     */
    public static Message nack(Integer nackNum) {
        Message message = new Message();
        message.setType(MessageType.NACK);
        message.setPropNum(nackNum);
        return message;
    }


    /**
     * Generate COMMIT(logNum, value) message
     *
     * @param logNum      log entry number
     * @param commitValue list of EventRecord corresponding to log numbers
     * @return message
     */
    public static Message commit(Integer logNum, EventRecord commitValue) {
        Message message = new Message();
        message.setLogNum(logNum);
        message.setType(MessageType.COMMIT);
        message.setValue(commitValue);
        return message;
    }

    /**
     * Generate QUERY() message
     *
     * @return message
     */
    public static Message query() {
        Message message = new Message();
        message.setType(MessageType.QUERY);
        return message;
    }

    /**
     * Generate REPLY(maxLogNum)
     *
     * @param maxLogNum maximum log entry number
     * @return message
     */
    public static Message reply(Integer maxLogNum) {
        Message message = new Message();
        message.setType(MessageType.REPLY);
        message.setLogNum(maxLogNum);
        return message;
    }

}
