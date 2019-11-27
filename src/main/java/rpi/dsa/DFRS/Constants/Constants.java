package rpi.dsa.DFRS.Constants;

import com.alibaba.fastjson.JSONObject;
import rpi.dsa.DFRS.Entity.Host;
import rpi.dsa.DFRS.Utils.FileUtils;

import java.util.*;

public class Constants {

    /**
     * CONST - Constrains
     */
    public static final int MESSAGE_LENGTH = 1024;

    public static final int MIN_FLIGHT = 1;

    public static final int MAX_FLIGHT = 20;

    public static final long TIMEOUT_MS = 5000;

    public static final int SOCK_TIMEOUT_MS = 100;

    public static final int CHECK_POINT_INTERVAL = 5;

    public static final int BLOCKING_QUEUE_CAP = 8;


    /**
     * CMD - User Input Command
     */
    public static final String RESERVE = "reserve";

    public static final String CANCEL = "cancel";

    public static final String VIEW = "view";

    public static final String LOG = "log";

    public static final String QUIT = "quit";


    /**
     * FILE - file configuration
     */
    public static final String LOG_FILE = "log.txt";

    public static final String ACCEPTOR_FILE = "accept_state.out";

    public static final String RESV_FILE = "reservations.out";

    public static final String IS_CHECKPOINT = "CHECKPOINT";

    public static final String NOT_CHECKPOINT = "-";


    /**
     * HOSTS - Known hosts from configuration file
     */
    private static final String PATH = "knownhosts.json";

    public static final Map<String, Host> HOSTS;

    public static final Map<Integer, String> INDEX_TO_NAME;

    public static final Map<String, Integer> NAME_TO_INDEX;

    static {
        Map<String, Host> map = new HashMap<>();
        String info = FileUtils.readFile(PATH);
        JSONObject jsonObject = JSONObject.parseObject(info);
        JSONObject hosts = jsonObject.getJSONObject("hosts");
        Set<String> keys = hosts.keySet();
        for (String key : keys) {
            Host host = hosts.getObject(key, Host.class);
            map.put(key, host);
        }
        HOSTS = Collections.unmodifiableMap(map);
        Map<Integer, String> iton = new HashMap<>();
        Map<String, Integer> ntoi = new HashMap<>();
        List<String> names = new ArrayList<>(keys);
        Collections.sort(names);
        for (int i = 0; i < names.size(); i++) {
            iton.put(i, names.get(i));
            ntoi.put(names.get(i), i);
        }
        INDEX_TO_NAME = Collections.unmodifiableMap(iton);
        NAME_TO_INDEX = Collections.unmodifiableMap(ntoi);
    }



}
