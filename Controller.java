import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

public class Controller {
    public static final String NAME = Controller.class.getName();

    // Passed arguments
    private static int cport;
    private static int R;
    private static int timeout;
    private static int rebalance_period;

    private static ConcurrentHashMap<String, Index> indexMap;
    private static ConcurrentHashMap<Connection, DstoreProps> dstores;
    // private static PriorityBlockingQueue<DstoreProps> dstores;

    /**
     * 3 arguments are passed when running the controller:
     * <p>
     * 1: cport : port to listen on <br>
     * 2: R: replication factor <br>
     * 3: timeout: timeout in milliseconds <br>
     * 4: rebalance_period: how long to wait (in ms) to start the next rebalance
     * operation
     * </p>
     */
    public static void main(String[] args) {

        initArgs(args);
        initLogger();

        indexMap = new ConcurrentHashMap<>();
        dstores = new ConcurrentHashMap<>();

        ServerSocket ss = null;
        try {
            ss = new ServerSocket(cport);
            for (;;) {
                TerminalLog.printMes(NAME, "Ready to accept connections");
                Socket dstore = ss.accept();
                TerminalLog.printMes(NAME, "New connection from port " + dstore.getPort());
                TerminalLog.printMes(NAME, "Transfering control to handler to determine the type of the connection");
                new Thread(new ConnectionHandler(dstore, ConnectionHandler.ServerType.CONTROLLER)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeServer(ss);
        }
    }

    public static void closeServer(ServerSocket ss) {
        try {
            if (ss != null) {
                ss.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getIndexSize(String filename) {
        return indexMap.get(filename).getFilesize();
    }

    /**
     * Adding/Removing/Getting Dstores
     */
    public static void addDstore(Connection dstore, int port) {
        dstores.put(dstore, new DstoreProps(port));
    }

    // TODO: Maybe add a check that doesn't allow dstores with the same listening
    // port to exist
    public static Integer[] getDstorePorts() {
        return null;
    }

    public static void removeDstore(Connection dstore) {
        for (String filename : indexMap.keySet()) {
            Index index = indexMap.get(filename);
            if (index.getDstores().contains(dstore)) {
                // remove it but also remove the index if empty
                if (index.removeDstore(dstore)) {
                    TerminalLog.printMes(NAME, "Removing index for file '" + filename + "'");
                    deleteIndex(filename);
                }
            }
        }

        int port = dstore.getPort();
        dstores.remove(dstore);
        TerminalLog.printMes(NAME, "Successfully removed a dstore '" + port + "'");
    }

    public static Integer[] getDstores(int filesize) {

        if (!isEnoughDstores()) {
            TerminalLog.printMes(NAME, "Not enough Dstores! Can't return the requested dstores!");
            return null;
        }

        Integer[] storingDstores = new Integer[R];

        // TODO: change to floor and ceiling
        List<DstoreProps> ports = dstores.values().stream().sorted(Comparator.comparingInt(DstoreProps::getSize))
                .collect(Collectors.toList());

        // Get them out first
        for (int i = 0; i < R; i++) {
            DstoreProps prop = ports.get(i);
            storingDstores[i] = prop.getPort();
            prop.addFile(filesize);
        }

        return storingDstores;
    }

    public static int getDstoreServerPort(Connection connection) {
        return dstores.get(connection).getPort();
    }

    /**
     * Index related manipulations
     */
    public static boolean addIndex(String filename, int filesize, Connection storer) {
        if (indexExists(filename)) {
            return false;
        }

        Index index = new Index(filesize, storer);
        indexMap.put(filename, index);
        index.startTimer();
        return true;
    }

    public static void deleteIndex(String filename) {
        indexMap.remove(filename);
    }

    public static ArrayList<Connection> startIndexRemoval(String filename) {
        Index index = indexMap.get(filename);
        ArrayList<Connection> dstores = index.getDstores();
        index.startTimer();
        return dstores;
    }

    public static Index.Timeout ackRemovalIndex(String filename, Connection dstore) {
        return indexMap.get(filename).ackRemove(dstore);
    }

    public static ArrayList<String> listFiles() {
        Iterator<String> it = indexMap.keySet().iterator();
        ArrayList<String> files = new ArrayList<>();

        // Only List those that are stored already
        while (it.hasNext()) {
            String filename = it.next();
            if (indexMap.get(filename).getCurrentState() == Index.State.STORE_COMPLETE) {
                files.add(filename);
            }
        }

        return files;
    }

    public static Index.Timeout ackStorageIndex(String filename, Connection dstore) {
        return indexMap.get(filename).ackStore(dstore);
    }

    // public static int getfileServer(String filename) {
    // return dstores.get(indexMap.get(filename).getStore()).getPort();
    // }

    public static CompletableFuture<Index.Timeout> getIndexTimer(String filename) {
        return indexMap.get(filename).getStoreAck();
    }

    public static ArrayList<Connection> getIndexServers(String filename) {
        return indexMap.get(filename).getDstores();
    }

    public static Index.State getIndexState(String filename) {
        return indexMap.get(filename).getCurrentState();
    }

    public static boolean indexExists(String filename) {
        return indexMap.containsKey(filename);
    }

    public static boolean isEnoughDstores() {
        return dstores.size() >= R;
    }

    /**
     * Initialisation Stuff
     */
    private static void initLogger() {
        try {
            ControllerLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL);
            TerminalLog.printMes(NAME, "Successfully created log file!");
        } catch (IOException e) {
            TerminalLog.printErr(NAME, "Error: issue with creating the log file");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void initArgs(String[] args) {
        if (args.length != 4) {
            TerminalLog.printErr(NAME, "Invalid number of args, expected: 4, but got: " + args.length);
            System.exit(1);
        }

        parseArgs(args[0], "port");
        parseArgs(args[1], "replication factor");
        parseArgs(args[2], "timeout period");
        parseArgs(args[3], "rebalance_period");
    }

    private static void parseArgs(String arg, String name) {
        try {
            switch (name) {
                case "port":
                    cport = Integer.parseInt(arg);
                    break;
                case "replication factor":
                    R = Integer.parseInt(arg);
                    break;
                case "timeout period":
                    timeout = Integer.parseInt(arg);
                    break;
                case "rebalance_period":
                    rebalance_period = Integer.parseInt(arg);
                    break;
                default:
                    throw new Exception("Internal error, invalid arg: " + name);
            }

        } catch (NumberFormatException e) {
            TerminalLog.printErr(NAME, "Invalid " + name + ", must be an integer");
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            TerminalLog.printErr(NAME, e.toString());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            TerminalLog.printErr(NAME, e.toString());
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static int getR() {
        return R;
    }

    public static int getTimeout() {
        return timeout;
    }

    public static int getRebalance_period() {
        return rebalance_period;
    }

}
