package rpi.dsa.DFRS.Roles;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.Message;
import rpi.dsa.DFRS.Service;
import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Acceptor implements Runnable {

    private int maxAccNum;

    private Integer accNum;

    private EventRecord accValue;

    private DatagramSocket socket;

    public Acceptor() {
        this.maxAccNum = -1;
        this.accNum = null;
        this.accValue = null;
        String ip = Service.myHost.getIpAddr();
        Integer port = Service.myHost.getUdpEndPort();

        try {
            InetAddress address = InetAddress.getByName(ip);
            socket = new DatagramSocket(port, address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                socket.receive(datagramPacket);
                Message message = MsgUtil.deserialize(bytes);
                InetAddress propAddr = datagramPacket.getAddress();
                Integer propPort = datagramPacket.getPort();

                switch (message.getType()) {
                    case PREPARE:
                        handle_prepare(message, propAddr, propPort);
                        break;
                    case PROPOSAL:
                        handle_proposal(message, propAddr, propPort);
                        break;
                    case COMMIT:
                        Learner.learnProposal(message.getValue());
                        reset();
                        break;
                    case QUERY:
                        break;
                    default:
                        System.out.println("Acceptor: Invalid Message. " + message.toString());
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handle_prepare(Message prepare, InetAddress propAddr, Integer propPort) {
        /* Generate response based on propose number */
        Integer propNum = prepare.getNum();
        Message response;
        if (propNum > maxAccNum) {
            maxAccNum = propNum;
            response = Message.promise(accNum, accValue);
        } else {
            response = Message.nack(maxAccNum);
        }

        /* Reply to proposer */
        try {
            byte[] buffer = MsgUtil.serialize(response);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, propAddr, propPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handle_proposal(Message proposal, InetAddress propAddr, Integer propPort) {
        /* Generate response based on propose number */
        Integer propNum = proposal.getNum();
        EventRecord propValue = proposal.getValue();
        Message response;
        if (propNum > maxAccNum) {
            accNum = propNum;
            accValue = propValue;
            maxAccNum = propNum;
            response = Message.ack(propNum);
        } else {
            response = Message.nack(maxAccNum);
        }

        /* Reply to proposer */
        try {
            byte[] buffer = MsgUtil.serialize(response);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, propAddr, propPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * After receiving committed proposal, reset acceptor's state.
     * (Problem: what if the commit message get lost? Then the state will not reset.)
     */
    private void reset() {
        maxAccNum = 0;
        accNum = null;
        accValue = null;
    }

}
