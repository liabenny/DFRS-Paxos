package rpi.dsa.DFRS;

import rpi.dsa.DFRS.Constants.Constants;
import rpi.dsa.DFRS.Entity.Host;
import rpi.dsa.DFRS.Roles.Acceptor;
import rpi.dsa.DFRS.Roles.Learner;

import static java.lang.System.exit;
import static rpi.dsa.DFRS.Constants.Constants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class Service {

    public static Host myHost;

    public static String hostName;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid Parameter.");
            System.out.println("Please specify site ID.");
            return;
        }
        hostName = args[0];
        init();
        listen();
        start();
    }

    private static void init() {
        /* 1. Resolve local host configuration from knownhost.json */
        for (Map.Entry<String, Host> entry : Constants.HOSTS.entrySet()) {
            Host host = entry.getValue();
            String site = entry.getKey();
            if (site.equals(hostName)) {
                myHost = host;
                break;
            }
        }

        if (hostName == null) {
            throw new RuntimeException("Unknown local host");
        }

        /* 2. Init dictionary and event record */
        Learner.init(hostName);
    }

    private static void listen() {
        Acceptor acceptor = new Acceptor();
        Thread thread = new Thread(acceptor);
        thread.start();
    }

    private static void start() {
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
                    default:
                        System.out.println("Unknown command");
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException("Service: Read command failed");
            }
        }
    }
}


