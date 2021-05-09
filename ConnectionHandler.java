import java.io.*;
import java.net.*;
import java.util.Arrays;

public class ConnectionHandler implements Runnable {

	public enum ServerType {
		CONTROLLER, DSTORE
	}

	public static final String NAME = ConnectionHandler.class.getName();

	private Connection connection;
	private BufferedReader in;
	private ServerType serverType;

	public ConnectionHandler(Socket socket, ServerType serverType) {
		this.serverType = serverType;
		try {
			connection = new Connection(socket);
			in = connection.getInReader();
		} catch (IOException e) {
			TerminalLog.printErr(NAME,
					socket.getPort() + " - Failed to get the input or output stream of the connection");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void run() {
		TerminalLog.printMes(NAME, connection.getPort() + " - Waiting for the command");
		String line;
		try {
			while ((line = in.readLine()) != null) {
				TerminalLog.printMes(NAME, connection.getPort() + " - Command successfully read!");
				String[] command = line.split(" ");
				TerminalLog.printMes(NAME, connection.getPort() + " Command: " + Arrays.toString(command));
				if (serverType == ServerType.CONTROLLER) {
					controllerHandle(command);
				} else {
					dstoreHandle(command);
				}

				TerminalLog.printMes(NAME, connection.getPort() + " - Waiting for the command");
			}
			TerminalLog.printMes(NAME, connection.getPort() + "- terminated connection");
		} catch (IOException e) {
			TerminalLog.printErr(NAME, connection.getPort() + " - Failed to read input from the connection!");
			e.printStackTrace();
		} finally {
			try {
				TerminalLog.printMes(NAME, connection.getPort() + " - Closing down the connection");
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// TODO: implement (or potentially just do it in handlers)
	public void checkArgs(String[] command) {
		TerminalLog.printMes(NAME, connection.getPort() + " - verifying validity of the command");
	}

	public void dstoreHandle(String[] command) {
		switch (command[0]) {
			case Protocol.STORE_TOKEN:
				DstoreServerHandler server = new DstoreServerHandler(connection, command);
				server.handle();
				break;
			default:
				TerminalLog.printMes(NAME, connection.getPort() + " - this command hasn't been implemented yet");
				break;
		}
	}

	public void controllerHandle(String[] command) {
		switch (command[0]) {
			case Protocol.JOIN_TOKEN:
				DstoreHandler dstore = new DstoreHandler(connection, command);
				dstore.handle();
				break;
			case Protocol.STORE_ACK_TOKEN:
				if (Controller.ackIndex(command[1])) {
					Controller.getStorer(command[1]).getOutWriter().println(Protocol.STORE_COMPLETE_TOKEN);
				}
				break;
			case Protocol.LIST_TOKEN:
			case Protocol.STORE_TOKEN:
			case Protocol.LOAD_TOKEN:
			case Protocol.LOAD_DATA_TOKEN:
			case Protocol.RELOAD_TOKEN:
			case Protocol.REMOVE_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - New client attempts to join the network!");

				if (!Controller.isEnoughDstores()) {
					TerminalLog.printErr(NAME,
							connection.getPort() + " - Cannot connect! Not enough dstores joined the network");
				} else {
					ClientHandler client = new ClientHandler(connection, command);
					client.handle();
				}
				break;
			default:
				TerminalLog.printMes(NAME, connection.getPort() + " - Invalid command");
				// TODO: This is an invalid command, should log it and ignore
		}

	}

	/**
	 * Handlers
	 */
	// private Handler join = (String[] args, Connection connection) -> {
	// TerminalLog.printMes(NAME, connection.getPort() + " - New dstore just joined
	// the ranks!");
	// TerminalLog.printMes(NAME, connection.getPort() + " - Declared port: " +
	// args[0]);
	// Controller.addDstore(Integer.parseInt(args[0]));
	// TerminalLog.printMes(NAME, connection.getPort() + " - Dstore successfully
	// added to the list!");
	// };
}
