package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.Host;
import rpi.dsa.DFRS.Utils.Clock;

import static java.lang.System.exit;
import static rpi.dsa.DFRS.Constants.Constants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class app {

    private static Host myHost;

    private static String hostName;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid Parameter.");
            System.out.println("Please specify site ID.");
            return;
        }
        String siteId = args[0];
        init(siteId);
        listen();
        start();
    }

    private static void init(String siteId) {
        /* 1. Get local host information */
        String ipAddr = null;
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            ipAddr = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        /* 2. Resolve local host configuration from knownhost.json */
        for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
            Host host = entry.getValue();
            String site = entry.getKey();
            if (host.getIpAddr().equals(ipAddr) && site.equals(siteId)) {
                hostName = site;
                myHost = host;
                break;
            }
        }
//        /* ================================TEST===================================== */
//        hostName = "test";
//        myHost = HOSTS.get(hostName);
//        /* ================================TEST===================================== */

        if (hostName == null) {
            throw new RuntimeException("Unknown local host");
        }
//        System.out.println("[SERVICE] resolve local ip address success!");


        /* 3. Init time table and local clock */
        Clock.init(hostName);

        /* 4. Init dictionary and event record */
        Resource.init(hostName);

    }

    private static void listen() {
        MsgHandler handler = new MsgHandler(hostName, myHost);
        Thread thread = new Thread(handler);
        thread.start();
    }

    private static void start() {
//        System.out.println("[SERVICE] start service, process ID is " + NAME_TO_INDEX.get(hostName));
        InputStreamReader inputStreamReader = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        while (true) {
            try {
                /* Waiting input from users */
                String cmd = reader.readLine();
                if (cmd == null || cmd.length() == 0) {
                    continue;
                }

                /* Terminate program */
                if (QUIT.equals(cmd)) {
                    inputStreamReader.close();
                    exit(0);
                }

                /* Handle commands */
                String[] args = cmd.split(" ");
                switch (args[0]) {
                    case RESERVE:
                        CmdHandler.reserve(args);
                        break;
                    case CANCEL:
                        CmdHandler.cancel(args);
                        break;
                    case VIEW:
                        CmdHandler.view(args);
                        break;
                    case LOG:
                        CmdHandler.log(args);
                        break;
                    case SEND:
                        CmdHandler.send(args);
                        break;
                    case SENDALL:
                        CmdHandler.sendAll(args);
                        break;
                    case SMALLSEND:
                        CmdHandler.smallSend(args);
                        break;
                    case SMALLSENDALL:
                        CmdHandler.smallSendAll(args);
                        break;
                    case CLOCK:
                        CmdHandler.clock(args);
                        break;
                    default:
                        System.out.println("Unknown command");
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException("App: Read command failed");
            }
        }
    }
}


