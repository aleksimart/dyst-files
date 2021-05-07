import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Dstore {
	private static int port;
	private static int cport;
	private static int timeout;
	public static final String NAME = Dstore.class.getName();

	private static Connection connection;
	private static PrintWriter out;
	private static BufferedReader in;
	private static Socket socket;

	private static File file_folder;

	public static void main(String[] args) {
		initArgs(args);
		initLogger();

		try {
			initConnection();
			joinController();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void initLogger() {
		try {
			DstoreLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL, port);
		} catch (IOException e) {
			TerminalLog.printErr(NAME, "Error: issue with creating the log file");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void initConnection() throws IOException {
		TerminalLog.printMes(NAME, "Establishing connection with Controller on port " + cport + ", local address");
		InetAddress address = InetAddress.getLocalHost();

		socket = new Socket(address, cport, address, port);
		socket.setSoTimeout(timeout); // TODO: is this needed

		connection = new Connection(socket);
		out = connection.getOut();
		in = connection.getIn();

		TerminalLog.printMes(NAME, "Succesfully established connection! Local port: " + port);
	}

	// TODO: Might just ask to send the response as LIST
	private static void joinController() throws IOException {
		TerminalLog.printMes(NAME, "Attempting to join the controller");
		String message = Protocol.JOIN_TOKEN + " " + port;
		out.println(message);
		DstoreLogger.getInstance().messageSent(socket, TerminalLog.stampMes(message));

		String response;

		try {
			response = in.readLine();
			if (response == null) {
				TerminalLog.printErr(NAME, "Connection Lost");
			} else {
				TerminalLog.printMes(NAME, "Server: " + response);
			}
		} catch (SocketTimeoutException e) {
			TerminalLog.printMes(NAME, "Joined Successfully!");
		}

	}

	private static void initArgs(String[] args) {
		if (args.length != 4) {
			TerminalLog.printErr(NAME, "Invalid number of args, expected: 4, but got: " + args.length);
			System.exit(1);
		}

		parseArgs(args[0], "port");
		parseArgs(args[1], "controller port");
		parseArgs(args[2], "timeout period");
		parseArgs(args[3], "file folder");
	}

	private static void parseArgs(String arg, String name) {
		try {
			switch (name) {
				case "port":
					port = Integer.parseInt(arg);
					break;
				case "controller port":
					cport = Integer.parseInt(arg);
					break;
				case "timeout period":
					timeout = Integer.parseInt(arg);
					break;
				case "file folder":
					// TODO: possibly add a check that it the folder is possible to create
					file_folder = new File(arg);
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

}
