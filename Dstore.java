import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class Dstore {

	// Passed arguments to main
	private static int port;
	private static int cport;
	private static int timeout;
	private static File file_folder;

	// Name of the class to use in logging
	public static final String NAME = Dstore.class.getName();

	// Everything needed for connecting with controller
	private static Connection connection;
	private static Socket socket;
	private static PrintWriter out;
	private static BufferedReader in;

	// In case the server is started
	private static DstoreServer server;

	/**
	 * INITIALISATION
	 */
	private static void initLogger() {
		try {
			DstoreLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL, port);
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
					initFileFolder(arg);
					break;
				default:
					throw new Exception("Internal error, invalid arg: " + name);
			}

		} catch (NumberFormatException e) {
			TerminalLog.printErr(NAME, "Failed to parse an argument: " + name + ",it must be an integer");
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			TerminalLog.printErr(NAME, "Failed to parse an argument: " + name);
			e.printStackTrace();
			System.exit(1);
		}

	}

	private static void initFileFolder(String folderName) throws IOException {
		file_folder = new File(folderName);

		if (!file_folder.exists() && !file_folder.mkdir()) {
			throw new IOException("Failed to create folder: " + folderName);
		}

		// Making sure that the passed folder is empty
		File[] files = file_folder.listFiles();
		for (File file : files) {
			file.delete();
		}
	}

	/**
	 * MAIN
	 */
	public static void main(String[] args) {
		initArgs(args);
		initLogger();

		try {
			initConnection();
			joinController();
			startServer();

			String line;
			while ((line = in.readLine()) != null) {
				String[] command = line.split(" ");
				String[] cmdArgs = Arrays.copyOfRange(command, 1, command.length);

				switch (command[0]) {
					case Protocol.REMOVE_TOKEN:
						DstoreServerHandler.removeHandler.handle(cmdArgs, connection);
					default:

				}

			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			// Close the dstore server
			if (serverCreated() && server.isOpen()) {
				server.close();
			}

			// Close the socket connected to the controller
			try {
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * CONTROLLER CONNECTION
	 */
	private static void initConnection() throws IOException {
		TerminalLog.printMes(NAME, "Establishing connection with Controller on port " + cport + ", local address");
		InetAddress address = InetAddress.getLocalHost();

		socket = new Socket(address, cport);
		// socket.setSoTimeout(timeout); // TODO: is this needed

		connection = new Connection(socket);
		out = connection.getOutWriter();
		in = connection.getInReader();

		TerminalLog.printMes(NAME, "Succesfully established connection! Local port: " + port);
	}

	// TODO: yeah make sure to send it from this connection
	public static void ackStorage(String fileName) {
		out.println(Protocol.STORE_ACK_TOKEN + " " + fileName);
	}

	// TODO: Might just ask to send the response as LIST
	private static void joinController() throws IOException {
		TerminalLog.printMes(NAME, "Attempting to join the controller");
		String message = Protocol.JOIN_TOKEN + " " + port;
		out.println(message);
		// Log it
		DstoreLogger.getInstance().messageSent(socket, TerminalLog.stampMes(message));
		TerminalLog.printMes(NAME, "Joined Successfully!");
	}

	/**
	 * SERVER CREATION
	 */
	private static void startServer() {
		server = new DstoreServer(port);
		new Thread(server).start();
	}

	private static boolean serverCreated() {
		return (server != null);
	}

	public static File getFile_folder() {
		return file_folder;
	}

}
