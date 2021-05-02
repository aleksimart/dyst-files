import java.io.*;
import java.net.*;
import java.util.ArrayList;

import jdk.internal.org.jline.utils.InputStreamReader;

public class Controller {

    private static int cport;
    private static int R;
    private static int timeout;
    private static int rebalance_period;

    private static ArrayList<Socket> dstores;

    /**
     * 3 arguments are passes when running the controller:
     * <p>
     * 1: cport : port to listen on <br>
     * 2: R: replication factor <br>
     * 3: timeout: timeout in milliseconds <br>
     * 4: rebalance_period: how long to wait (in ms) to start the next rebalance
     * operation
     * </p>
     */
    public static void main(String[] args) {

        // Initialise the arguments (Like a constructor)
        init(args);

        // Initialise the logger
        try {
            ControllerLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL);
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

                // From that point on call a connection

                InputStream in = dstore.getInputStream();

                BufferedReader inReader = new BufferedReader(new InputStreamReader(in));

                String firstLine = inReader.readLine();
                int firstSpace = firstLine.indexOf(" ");

                switch (firstLine.substring(0, firstSpace)) {
                    case Protocol.JOIN_TOKEN:
                        System.out.println("This is a dstore");
                        // TODO: This is a dstore, appropriate handler + log
                        break;
                    case Protocol.LIST_TOKEN:
                    case Protocol.STORE_TOKEN:
                    case Protocol.LOAD_TOKEN:
                    case Protocol.LOAD_DATA_TOKEN:
                    case Protocol.RELOAD_TOKEN:
                    case Protocol.REMOVE_TOKEN:
                        System.out.println("This is a client");
                        // TODO: This is client, appropriate handler + log
                        break;
                    default:
                        System.out.println("Invalid command");
                        // TODO: This is an invalid command, should log it and ignore
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String[] list() {

    }

    public static void init(String[] args) {
        if (args.length != 4) {
            System.err.println("Invalid number of args, expected: 4, but got: " + args.length);
            System.exit(1);
        }

        parseArgs(args[0], "port");
        parseArgs(args[1], "replicaton factor");
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
            System.err.println("Invalid replication factor, must be an integer");
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

    private void rebalance() {
    }

    /**
     * Ignore this for now
     */
    static class FileThread implements Runnable {

        Socket client;

        public FileThread(Socket client) {
            this.client = client;
        }

        public void run() {
            try {
                // Open Input Stream for receiving a file
                InputStream in = client.getInputStream();
                // 1000 bytes buffer
                byte[] buf = new byte[1000];
                // Length of the buffer or how much of the buffer is actually used
                int buflen;

                // Reads in the bytes into the buffer and returns the number of bytes read in
                buflen = in.read(buf);

                // Read in the butter and convert it to str
                String firstBuffer = new String(buf, 0, buflen);
                System.out.println("First buffer: " + firstBuffer);

                // Pull out the command
                int firstSpace = firstBuffer.indexOf(" ");
                String command = firstBuffer.substring(0, firstSpace);
                System.out.println("command " + command);

                /*
                 * There are two types of commands: 'put' and 'get' Put puts a new file on the
                 * server Get gets the existing file from the server
                 */
                if (command.equals("put")) {
                    // pull out the name of the file
                    int secondSpace = firstBuffer.indexOf(" ", firstSpace + 1);
                    String fileName = firstBuffer.substring(firstSpace + 1, secondSpace);
                    System.out.println("fileName " + fileName);

                    // Create a file with a given name
                    File outputFile = new File(fileName);
                    // Create the output stream for the files
                    FileOutputStream out = new FileOutputStream(outputFile);

                    /*
                     * Write the contents into the file starting from the 1st character after the
                     * second space (buflen secondspace - 1 is the number of bytes to write)
                     */
                    out.write(buf, secondSpace + 1, buflen - secondSpace - 1);

                    /*
                     * If there are still more bytes to read in (the file + commands was more than
                     * the length of the current buffer which is 1000)
                     */
                    while ((buflen = in.read(buf)) != -1) {
                        System.out.print("*");

                        out.write(buf, 0, buflen);
                    }

                    // Closing up everything
                    in.close();
                    client.close();
                    out.close();
                } else if (command.equals("get")) {
                    // pull out the name of the file
                    int secondSpace = firstBuffer.indexOf(" ", firstSpace + 1);
                    String fileName = firstBuffer.substring(firstSpace + 1, secondSpace);
                    System.out.println("fileName " + fileName);

                    // Find a file with a given name
                    File inputFile = new File(fileName);
                    // Create the input stream for the found file
                    FileInputStream inf = new FileInputStream(inputFile);
                    // Prepare the output stream for the client to send the file to
                    OutputStream out = client.getOutputStream();

                    // Keep reading the into the input stream until all of the file is read
                    while ((buflen = inf.read(buf)) != -1) {
                        System.out.print("*");
                        out.write(buf, 0, buflen);
                    }

                    // Close everything up
                    in.close();
                    inf.close();
                    client.close();
                    out.close();
                } else {
                    System.out.println("unrecognised command");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
