package rpi.dsa.DFRS.Roles;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.EventRecord;
import rpi.dsa.DFRS.Entity.Host;
import rpi.dsa.DFRS.Entity.Message;
import rpi.dsa.DFRS.Entity.MessageType;
import rpi.dsa.DFRS.Service;
import rpi.dsa.DFRS.Utils.MsgUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Proposer {

    /**
     * ATTRIBUTE - Paxos Variables
     */
    private int maxPropNum;

    private int ackCounter;

    private int promiseCounter;

    private int nackCounter;

    private List<Message> promiseMessages;

    private EventRecord proposedValue;

    /**
     * ATTRIBUTE - Configuration Variables
     */
    private int pid;

    private DatagramSocket socket;

    public Proposer() {
        this.maxPropNum = 0;
        this.ackCounter = 0;
        this.nackCounter = 0;
        this.promiseCounter = 0;
        promiseMessages = new ArrayList<>();

        String ip = Service.myHost.getIpAddr();
        Integer port = Service.myHost.getUdpStartPort();
        this.pid = Constants.NAME_TO_INDEX.get(Service.hostName);

        try {
            InetAddress address = InetAddress.getByName(ip);
            socket = new DatagramSocket(port, address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean request(EventRecord value) {
        /* Choose a propose number */
        int propNum = generateNum();
        maxPropNum = propNum;

        // PHASE1: send prepare(n) to acceptors
        prepare(propNum);

        // PHASE1: wait promise(accNum, accValue) from majority acceptors
        boolean succeed = waitPromise();

        /* retry PHASE1 */
        int retry = 0;
        while (!succeed && retry < 2) {
            retry++;
            reset();
            propNum = generateNum();
            prepare(propNum);
            succeed = waitPromise();
        }
        if (!succeed) {
            return false;
        }

        // PHASE2: send proposal(n, v) to acceptors
        propose(propNum, value);


        /* retry PHASE1 and PHASE2 */
        succeed = waitAck();
        while (!succeed && retry < 2) {
            retry++;
            reset();
            propNum = generateNum();
            prepare(propNum);
            succeed = waitPromise();
            if (succeed) {
                propose(propNum, value);
            }
        }
        if (!succeed) {
            return false;
        }

        // PHASE2: send commit(v) to acceptors
        commit();

        return proposedValue.equals(value);
    }

    private void prepare(Integer propNum) {
        //TODO send message using multi-thread.
        Message prep = Message.prepare(propNum);
        byte[] buffer = MsgUtil.serialize(prep);

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

    private boolean waitPromise() {
        /* 1. Set a timeout for waiting promise */
        long start = System.currentTimeMillis();
        long end = start + Constants.TIMEOUT;

        try {
            while (System.currentTimeMillis() > end) {
                /* 2. Keep receiving promise message */
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                socket.receive(datagramPacket);
                Message message = MsgUtil.deserialize(bytes);
                if (message.getType().equals(MessageType.PROMISE)) {
                    promiseMessages.add(message);
                    promiseCounter++;
                } else if (message.getType().equals(MessageType.NACK)) {
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* 4. Didn't receive enough promise in time */
        nackCounter = 0;
        return false;
    }

    private void propose(Integer propNum, EventRecord value) {
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
        Message proposal = Message.proposal(propNum, propValue);
        byte[] buffer = MsgUtil.serialize(proposal);

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

    private boolean waitAck() {
        /* 1. Set a timeout for waiting ack */
        long start = System.currentTimeMillis();
        long end = start + Constants.TIMEOUT;

        try {
            while (System.currentTimeMillis() > end) {
                /* 2. Keep receiving ack message */
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                socket.receive(datagramPacket);
                Message message = MsgUtil.deserialize(bytes);
                if (message.getType().equals(MessageType.ACK)) {
                    ackCounter++;
                } else if (message.getType().equals(MessageType.NACK)) {
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* 4. Didn't receive enough ack in time */
        nackCounter = 0;
        return false;
    }

    private void commit() {
        Message commit = Message.commit(proposedValue);
        byte[] buffer = MsgUtil.serialize(commit);

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
     *
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
