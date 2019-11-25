package rpi.dsa.DFRS.Roles;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.Message;
import rpi.dsa.DFRS.Service;
import rpi.dsa.DFRS.Utils.FileUtils;
import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class Acceptor implements Runnable {

    // Save the Synod variables for multiple log entries.
    // Key   - Log number
    // Value - Synod variables.
    private Map<Integer, AcceptorState> accState;

    private final BlockingQueue<Map.Entry<Integer, EventRecord>> queue;

    private DatagramSocket socket;

    public Acceptor(BlockingQueue<Map.Entry<Integer, EventRecord>> queue) {
        this.queue = queue;

        /* Load previous acceptor state from file */
        if (FileUtils.isExist(Constants.ACCEPTOR_FILE)) {
            accState = FileUtils.readAcceptorStateFromFile();
        } else {
            accState = new HashMap<>();
        }

        /* Initialize UDP socket */
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
                        System.err.printf("[Acceptor] Received prepare(%d, %d) from %s\n",
                                message.getLogNum(),
                                message.getPropNum(),
                                message.getSenderName());

                        handle_prepare(message, propAddr, propPort);
                        break;
                    case PROPOSAL:
                        System.err.printf("[Acceptor] Received proposal(%d, %d, '%s') from %s\n",
                                message.getLogNum(),
                                message.getPropNum(),
                                message.getValue(),
                                message.getSenderName());

                        handle_proposal(message, propAddr, propPort);
                        break;
                    case COMMIT:
                        System.err.printf("[Learner] Received commit(%d,'%s') from %s\n",
                                message.getLogNum(),
                                message.getValue(),
                                message.getSenderName());
                        Integer logNum = message.getLogNum();
                        EventRecord record = message.getValue();
                        Learner.learnProposal(logNum, record, queue);
                        break;
                    case QUERY:
                        System.err.printf("[Learner] Received query() from %s\n",
                                message.getSenderName());

                        handle_query(message, propAddr, propPort);
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
        /* Get acceptor state for the specific log number */
        Integer logNum = prepare.getLogNum();
        if (!accState.containsKey(logNum)) {
            accState.put(logNum, new AcceptorState());
        }
        AcceptorState state = accState.get(logNum);
        int maxAccNum = state.getMaxAccNum();
        Map.Entry<Integer, EventRecord> accEntry = state.getAccEntry();


        /* Generate response based on propose number */
        Integer propNum = prepare.getPropNum();
        Message response;
        if (propNum > maxAccNum) {
            state.setMaxAccNum(propNum);
            if (accEntry == null) {
                response = Message.promise(propNum, null, null);
            } else {
                response = Message.promise(propNum, accEntry.getKey(), accEntry.getValue());
            }

            System.err.printf("[Acceptor] Sending promise(%d, [%d, '%s']) to %s\n",
                    response.getPropNum(),
                    response.getNum(),
                    response.getValue(),
                    prepare.getSenderName());

        } else {
            response = Message.nack(maxAccNum);

            System.err.printf("[Acceptor] Sending nack(%d) to %s\n", maxAccNum, prepare.getSenderName());
        }

        /* Reply to proposer */
        try {
            byte[] buffer = MsgUtil.serialize(response);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, propAddr, propPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* Save acceptor state in the file */
        FileUtils.saveAcceptorStateToFile(accState);
    }

    private void handle_proposal(Message proposal, InetAddress propAddr, Integer propPort) {
        /* Get acceptor state for the specific log number */
        Integer logNum = proposal.getLogNum();
        if (!accState.containsKey(logNum)) {
            accState.put(logNum, new AcceptorState());
        }
        AcceptorState state = accState.get(logNum);
        int maxAccNum = state.getMaxAccNum();

        /* Generate response based on propose number */
        Integer propNum = proposal.getPropNum();
        EventRecord propValue = proposal.getValue();
        Message response;
        if (propNum >= maxAccNum) {
            Map.Entry<Integer, EventRecord> accEntry = new AbstractMap.SimpleEntry<>(propNum, propValue);
            state.setAccEntry(accEntry);
            state.setMaxAccNum(propNum);
            response = Message.ack(propNum);

            System.err.printf("[Acceptor] Sending ack(%d) to %s\n", propNum, proposal.getSenderName());
        } else {
            response = Message.nack(maxAccNum);

            System.err.printf("[Acceptor] Sending nack(%d) to %s\n", maxAccNum, proposal.getSenderName());
        }

        /* Reply to proposer */
        try {
            byte[] buffer = MsgUtil.serialize(response);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, propAddr, propPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* Save acceptor state in the file */
        FileUtils.saveAcceptorStateToFile(accState);
    }

    private void handle_query(Message query, InetAddress propAddr, Integer propPort) {
        /* Get max log number from Learner */
        int maxLogNum = Learner.getMaxLogNum();
        Message reply = Message.reply(maxLogNum);

        System.err.printf("[Learner] Sending reply(%d) to %s\n",
                maxLogNum,
                query.getSenderName());

        /* Reply to proposer */
        try {
            byte[] buffer = MsgUtil.serialize(reply);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, propAddr, propPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class AcceptorState implements Serializable {

        private static final long serialVersionUID = 1758988006920894362L;

        private int maxAccNum;

        private Map.Entry<Integer, EventRecord> accEntry;

        AcceptorState() {
            maxAccNum = -1;
            accEntry = null;
        }

        int getMaxAccNum() {
            return maxAccNum;
        }

        void setMaxAccNum(int maxAccNum) {
            this.maxAccNum = maxAccNum;
        }

        public Map.Entry<Integer, EventRecord> getAccEntry() {
            return accEntry;
        }

        public void setAccEntry(Map.Entry<Integer, EventRecord> accEntry) {
            this.accEntry = accEntry;
        }
    }

}
