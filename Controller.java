import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Controller {

    private static int cport;
    private static int R;
    private static int timeout;
    private static int rebalance_period;

    private static ArrayList<DstoreHandler> dstores = new ArrayList<>();

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

        try {
            ControllerLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL);
            System.out.println("[SERVER]: Successfully created log file!");
        } catch (IOException e) {
            System.err.println("[SERVER]: Error:  issue with creating the log file");
            e.printStackTrace();
            System.exit(1);
        }

        try {
            ServerSocket ss = new ServerSocket(cport);
            for (;;) {
                System.out.println("[SERVER]: Ready to accept connections");
                Socket dstore = ss.accept();
                System.out.println("[SERVER]: New connection from port " + dstore.getPort());
                System.out.println("[SERVER]: Transfering control to handler to determine the type of the connection");
                new Thread(new ConnectionHandler(dstore)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String[] list() {
        return new String[] {};
    }

    private static void initArgs(String[] args) {
        if (args.length != 4) {
            System.err.println("Invalid number of args, expected: 4, but got: " + args.length);
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
            System.err.println("Invalid " + name + ", must be an integer");
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static void addDstore(DstoreHandler dstore) {
        dstores.add(dstore);
    }

    private void rebalance() {
    }

    public static boolean isEnoughDstores() {
        return dstores.size() >= R;
    }
}
