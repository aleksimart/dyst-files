import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Controller {

    public static final String NAME = Controller.class.getName();

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
        initLogger();

        try {
            ServerSocket ss = new ServerSocket(cport);
            for (;;) {
                TerminalLog.printMes(NAME, "Ready to accept connections");
                Socket dstore = ss.accept();
                TerminalLog.printMes(NAME, "New connection from port " + dstore.getPort());
                TerminalLog.printMes(NAME, "Transfering control to handler to determine the type of the connection");
                new Thread(new ConnectionHandler(dstore)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    synchronized public static void addDstore(DstoreHandler dstore) {
        dstores.add(dstore);
        ControllerLogger.getInstance().dstoreJoined(dstore.getSocket(), dstore.getSocket().getPort());
    }

    private void rebalance() {
    }

    public static boolean isEnoughDstores() {
        return dstores.size() >= R;
    }

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

}
