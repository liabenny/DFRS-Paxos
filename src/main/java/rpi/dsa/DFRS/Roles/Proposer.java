package rpi.dsa.DFRS.Roles;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.*;
import rpi.dsa.DFRS.Service;
import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Proposer {

    /**
     * ATTRIBUTE - Paxos Variables
     */
    private int maxPropNum;

    private int ackCounter;

    private int nackCounter;

    private int promiseCounter;

    private List<Message> promiseMessages;

    private EventRecord proposedValue;

    /**
     * ATTRIBUTE - Configuration Variables
     */
    private static int pid;

    private static DatagramSocket socket;

    static {
        String ip = Service.myHost.getIpAddr();
        Integer port = Service.myHost.getUdpStartPort();
        pid = Constants.NAME_TO_INDEX.get(Service.hostName);

        try {
            InetAddress address = InetAddress.getByName(ip);
            socket = new DatagramSocket(port, address);
            socket.setSoTimeout(Constants.SOCK_TIMEOUT_MS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Proposer() {
        this.maxPropNum = 0;
        this.ackCounter = 0;
        this.nackCounter = 0;
        this.promiseCounter = 0;
        promiseMessages = new ArrayList<>();
    }

    /**
     * Ask other sites about the maximum log number.
     *
     * @return maximum log number
     */
    public int query() {
        /* 1. Send request to all sites to ask maximum log number */
        int maxLogNum = Learner.getMaxLogNum();
        Message query = Message.query();
        byte[] buffer = MsgUtil.serialize(query);
        System.err.println("[Proposer] Sending query() to all sites");

        try {
            for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
                Host host = entry.getValue();
                String hostIp = host.getIpAddr();
                Integer hostAccPort = host.getUdpEndPort();
                InetAddress hostAddr = InetAddress.getByName(hostIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, hostAddr, hostAccPort);
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* 2. Set a timeout for waiting reply */
        long start = System.currentTimeMillis();
        long end = start + Constants.TIMEOUT_MS;

        try {
            while (System.currentTimeMillis() <= end) {
                /* 3. Keep receiving reply message */
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                try {
                    socket.receive(datagramPacket);
                    Message message = MsgUtil.deserialize(bytes);
                    if (message.getType().equals(MessageType.REPLY)) {

                        System.err.printf("[Proposer] Received reply(%d) from %s\n",
                                message.getLogNum(),
                                message.getSenderName());

                        maxLogNum = Math.max(maxLogNum, message.getLogNum());
                        ackCounter++;
                    }

                    // receive reply from all sites
                    if (ackCounter == Constants.HOSTS.size()) {
                        return maxLogNum;
                    }

                } catch (SocketTimeoutException e) {
                    //pass
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return maxLogNum;
    }

    /**
     * Start an execution of Synod algorithm for one log entry.
     *
     * @param logNum log slot number
     * @param value  recommended value for proposal
     * @return if the recommended value was adopted, then return true. Otherwise, return false.
     */
    public boolean request(Integer logNum, EventRecord value, boolean recovery) {
        /* Prepare our proposal number and phase variables */
        int propNum = 0;
        boolean succeed = false;
        int retry = 0;

        /* Paxos Optimization, if we were the winning site from the previous log number we can skip phase 1.  */
        String previousWinner = Learner.winningSite(logNum - 1);
        if (recovery || !previousWinner.equals(Service.hostName)) {
            /* Choose a propose number */
            propNum = generateNum();

            // PHASE1: send prepare(logNum, propNum) to acceptors
            prepare(logNum, propNum);

            // PHASE1: wait promise(logNum, propNum, accNum, accValue) from majority acceptors
            succeed = waitPromise(logNum, propNum);

            /* retry PHASE1 */
            while (!succeed && retry < 2) {
                retry++;
                reset();
                propNum = generateNum();
                prepare(logNum, propNum);
                succeed = waitPromise(logNum, propNum);
            }
            if (!succeed) {
                return false;
            }
        }

        // PHASE2: send proposal(logNum, propNum, v) to acceptors
        propose(logNum, propNum, value);

        /* retry PHASE1 and PHASE2 */
        succeed = waitAck(logNum, propNum);
        while (!succeed && retry < 2) {
            retry++;
            reset();
            propNum = generateNum();
            prepare(logNum, propNum);
            succeed = waitPromise(logNum, propNum);
            if (succeed) {
                propose(logNum, propNum, value);
                succeed = waitAck(logNum, propNum);
            }
        }
        if (!succeed) {
            return false;
        }

        // PHASE2: send commit(v) to acceptors
        commit(logNum);

        return proposedValue.equals(value);
    }

    private void prepare(Integer logNum, Integer propNum) {
        Message prep = Message.prepare(logNum, propNum);
        byte[] buffer = MsgUtil.serialize(prep);
        System.err.printf("[Proposer] Sending prepare(%d, %d) to all sites\n", logNum, propNum);

        try {
            for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
                Host host = entry.getValue();
                String hostIp = host.getIpAddr();
                Integer hostAccPort = host.getUdpEndPort();
                InetAddress hostAddr = InetAddress.getByName(hostIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, hostAddr, hostAccPort);
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void propose(Integer logNum, Integer propNum, EventRecord value) {
        /* 1. Decide the proposed value */
        int max = Integer.MIN_VALUE;
        EventRecord propValue = null;

        for (Message message : promiseMessages) {
            Integer accNum = message.getNum();
            EventRecord accValue = message.getValue();
            if (accNum != null && accValue != null && accNum > max) {
                propValue = accValue;
            }
        }

        if (propValue == null) {
            propValue = value;
        }
        proposedValue = propValue;

        /* 2. Send proposal to acceptors */
        Message proposal = Message.proposal(logNum, propNum, propValue);
        byte[] buffer = MsgUtil.serialize(proposal);

        System.err.printf("[Proposer] Sending propose(%d, %d, '%s') to all sites\n",
                logNum, propNum,
                proposedValue);

        try {
            for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
                Host host = entry.getValue();
                String hostIp = host.getIpAddr();
                Integer hostAccPort = host.getUdpEndPort();
                InetAddress hostAddr = InetAddress.getByName(hostIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, hostAddr, hostAccPort);
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void commit(Integer logNum) {
        Message commit = Message.commit(logNum, proposedValue);
        byte[] buffer = MsgUtil.serialize(commit);

        System.err.printf("[Proposer] Sending commit(%d, '%s') to all sites\n", logNum,
                proposedValue);

        try {
            for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
                Host host = entry.getValue();
                String hostIp = host.getIpAddr();
                Integer hostAccPort = host.getUdpEndPort();
                InetAddress hostAddr = InetAddress.getByName(hostIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, hostAddr, hostAccPort);
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean waitPromise(Integer logNum, Integer propNum) {
        /* 1. Set a timeout for waiting promise */
        long start = System.currentTimeMillis();
        long end = start + Constants.TIMEOUT_MS;

        try {
            while (System.currentTimeMillis() <= end) {
                /* 2. Keep receiving promise message */
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                try {
                    socket.receive(datagramPacket);
                    Message message = MsgUtil.deserialize(bytes);
                    if (message.getType().equals(MessageType.PROMISE) &&
                            message.getPropNum().equals(propNum) && message.getLogNum().equals(logNum)) {

                        System.err.printf("[Proposer] Received promise(%d, %d, [%d, '%s']) from %s\n",
                                message.getLogNum(),
                                message.getPropNum(),
                                message.getNum(),
                                message.getValue(),
                                message.getSenderName());

                        promiseMessages.add(message);
                        promiseCounter++;
                    } else if (message.getType().equals(MessageType.PROMISE_NACK) &&
                            message.getLogNum().equals(logNum) && message.getPropNum().equals(propNum)) {

                        System.err.printf("[Proposer] Received promise_nack(%d, %d, %d) from %s\n",
                                message.getLogNum(),
                                message.getPropNum(),
                                message.getNum(),
                                message.getSenderName());

                        // update the max propose number
                        maxPropNum = Math.max(maxPropNum, message.getNum());
                        nackCounter++;
                    }

                    /* 3. Receive promise from majority acceptors */
                    if (promiseCounter > Constants.HOSTS.size() / 2) {
                        return true;
                    } else if (nackCounter > Constants.HOSTS.size() / 2) {
                        nackCounter = 0;
                        return false;
                    }
                } catch (SocketTimeoutException e) {
                    //pass
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* 4. Didn't receive enough promise in time */
        nackCounter = 0;
        return false;
    }

    private boolean waitAck(Integer logNum, Integer propNum) {
        /* 1. Set a timeout for waiting ack */
        long start = System.currentTimeMillis();
        long end = start + Constants.TIMEOUT_MS;

        try {
            while (System.currentTimeMillis() <= end) {
                /* 2. Keep receiving ack message */
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                try {
                    socket.receive(datagramPacket);
                    Message message = MsgUtil.deserialize(bytes);
                    if (message.getType().equals(MessageType.ACK) &&
                            message.getLogNum().equals(logNum) && message.getPropNum().equals(propNum)) {

                        System.err.printf("[Proposer] Received ack(%d, %d) from %s\n",
                                message.getLogNum(),
                                message.getPropNum(),
                                message.getSenderName());

                        ackCounter++;
                    } else if (message.getType().equals(MessageType.NACK) &&
                            message.getLogNum().equals(logNum) && message.getPropNum().equals(propNum)) {

                        System.err.printf("[Proposer] Received nack(%d, %d, %d) from %s\n",
                                message.getLogNum(),
                                message.getPropNum(),
                                message.getNum(),
                                message.getSenderName());

                        maxPropNum = Math.max(maxPropNum, message.getNum());
                        nackCounter++;
                    }

                    /* 3. Receive ack from majority acceptors */
                    if (ackCounter > Constants.HOSTS.size() / 2) {
                        return true;
                    } else if (nackCounter > Constants.HOSTS.size() / 2) {
                        nackCounter = 0;
                        return false;
                    }
                } catch (SocketTimeoutException e) {
                    //pass
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* 4. Didn't receive enough ack in time */
        nackCounter = 0;
        return false;
    }


    /**
     * Given a pair of assenting and dissenting messages, increases
     * or decreases ack and nack counters appropriately.
     */
    private void handleResponse(Message message, MessageType ack, MessageType nack) {
        if (message.getType().equals(ack)) {
            ackCounter++;
            if (ack.equals(MessageType.PROMISE)) {
                promiseMessages.add(message);
                // Print the promise information to sys err.
                System.err.printf("[Proposer] Received %s(%d, [%d, '%s']) from %s\n",
                        message.getType().getDesc(),
                        message.getPropNum(),
                        message.getNum(),
                        message.getValue(),
                        message.getSenderName());
            } else {
                // Print the ack information to sys err.
                System.err.printf("[Proposer] Received ack(%d) from %s\n",
                        message.getPropNum(), message.getSenderName());
            }

        } else if (message.getType().equals(nack)) {
            maxPropNum = Math.max(maxPropNum, message.getNum());
            nackCounter++;

            // Print the nack information to sys err.
            System.err.printf("[Proposer] Received %s(%d) from %s\n",
                    message.getType().getDesc(), message.getNum(), message.getSenderName());
        }
    }


    /**
     * Before each retry, reset variable to initial state.
     */
    private void reset() {
        ackCounter = 0;
        nackCounter = 0;
        promiseCounter = 0;
        promiseMessages.clear();
    }

    /**
     * Generate the propose number based on maxPrepNum the site known so far.
     * And update the maxPropNum attribute.
     * <p>
     * Formula: N = (M + 1) * H + id
     * N - propose number
     * M - max number of rounds of executing Synod known so far
     * H - number of distributed sites
     * id - current site ID
     *
     * @return Propose Number
     */
    private int generateNum() {
        int sites = Constants.HOSTS.size();
        int round = maxPropNum / sites;
        maxPropNum = (round + 1) * sites + pid;
        return maxPropNum;
    }
}
