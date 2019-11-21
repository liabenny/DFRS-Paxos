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
        int currentPhase = 1;
        // PHASE1: send prepare(n) to acceptors
        prepare(propNum);

        // PHASE1: wait promise(accNum, accValue) from majority acceptors
        boolean succeed = waitForMajority(currentPhase);

        //TODO: May want to functionize the retry logic
        /* retry PHASE1 */
        int retry = 0;
        while (!succeed && retry < 2) {
            retry++;
            reset();
            propNum = generateNum();
            prepare(propNum);
            succeed = waitForMajority(currentPhase);
        }
        if (!succeed) {
            return false;
        }

        // PHASE2: send proposal(n, v) to acceptors
        propose(propNum, value);
        currentPhase = 2;

        /* retry PHASE1 and PHASE2 */
        succeed = waitForMajority(currentPhase);
        while (!succeed && retry < 2) {
            retry++;
            reset();
            currentPhase = 1;
            propNum = generateNum();
            prepare(propNum);
            succeed = waitForMajority(currentPhase);
            if (succeed) {
                propose(propNum, value);
                currentPhase = 2;
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

    private void handleResponse(Message message, MessageType ack, MessageType nack){
        if (message.getType().equals(ack)){
            ackCounter++;
        } else if (message.getType().equals(nack)){
            maxPropNum = Math.max(maxPropNum, message.getNum());
            nackCounter++;
        }
    }

    private boolean waitForMajority(int currPhase){
        try {
            /* 1. Set a timeout for waiting ack */
            long start = System.currentTimeMillis();
            socket.setSoTimeout(Math.toIntExact(Constants.TIMEOUT_ms));

            while (true) {
                /* 2. Keep receiving ack message */
                byte[] bytes = new byte[Constants.MESSAGE_LENGTH];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, Constants.MESSAGE_LENGTH);
                socket.receive(datagramPacket);
                Message message = MsgUtil.deserialize(bytes);

                /* 3. Check phase and handle messages accordingly. If we are in phase 2, phase one acks
                *     and nacks should be ignored, since a majority was already found. */
                if (currPhase == 1){
                    handleResponse(message, MessageType.PROMISE, MessageType.PROMISE_NACK);
                } else if (currPhase == 2){
                    handleResponse(message, MessageType.ACK, MessageType.NACK);
                }

                /* 4. Receive ack from majority acceptors */
                if (ackCounter > Constants.HOSTS.size() / 2) {
                    return true;
                } else if (nackCounter > Constants.HOSTS.size() / 2) {
                    nackCounter = 0;
                    return false;
                } else{
                    /* 5. Update our socket timeout with the remaining time */
                    long elapsed_ms = System.currentTimeMillis() - start;
                    int remaining_ms = Math.toIntExact(Constants.TIMEOUT_ms - elapsed_ms);
                    if(remaining_ms > 0){
                        socket.setSoTimeout(remaining_ms);
                    } else{
                        return false;
                    }
                }
            }
        } catch (java.net.SocketTimeoutException e){
            /* 6. Didn't receive enough ack in time */
            nackCounter = 0;
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // We should not be reaching this line. If we do somehow return false.
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
