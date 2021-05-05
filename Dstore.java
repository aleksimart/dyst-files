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

	private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
	private static int port;
	private static int cport;
	private static int timeout;

	private static PrintWriter pWriter;
	private static BufferedReader reader;
	private static Socket socket;

	private static File file_folder;

	private static void printMes(String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		System.out.println("[DSTORE: " + timeStamp + "]: " + mes);
	}

	private static void printErr(String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		System.err.println("[DSTORE: " + timeStamp + "]: " + mes);
	}

	private static String stampMes(String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		return (timeStamp + ": " + mes);
	}

	public static void main(String[] args) {
		initArgs(args);
		initLogger();

		try {
			initSocket();
			joinController();
			// socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void initLogger() {
		try {
			DstoreLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL, port);
		} catch (IOException e) {
			printErr("Error: issue with creating the log file");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void initSocket() throws IOException {
		printMes("Establishing connection with Controller on port " + cport + ", local address");
		InetAddress address = InetAddress.getLocalHost();

		pWriter = new PrintWriter(socket.getOutputStream(), true);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		socket = new Socket(address, cport, address, port);
		socket.setSoTimeout(timeout);

		printMes("Succesfully established connection! Local port: " + port);
	}

	// TODO: Might just ask to send the response as LIST
	private static void joinController() throws IOException {
		printMes("Attempting to join the controller");
		String message = Protocol.JOIN_TOKEN + " " + port;
		pWriter.println(message);
		DstoreLogger.getInstance().messageSent(socket, stampMes(message));

		String response;

		try {
			response = reader.readLine();
			if (response == null) {
				printErr("Connection Lost");
			} else {
				printMes("Server: " + response);
			}
		} catch (SocketTimeoutException e) {
			printMes("Joined Successfully!");
		}

	}

	private static void interactiveCommands() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		printMes("Ready to accept commands");
		String mes;

		System.out.print("> ");
		while ((mes = reader.readLine()) != "quit") {
			pWriter.println(mes);
			System.out.print("> ");

			if (reader.readLine() == Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN) {
				printErr("Not enough dstores joined yet, try again later!");
				break;
			}
		}

	}

	private static void initArgs(String[] args) {
		if (args.length != 4) {
			System.err.println("Invalid number of args, expected: 4, but got: " + args.length);
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
