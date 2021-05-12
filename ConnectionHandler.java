import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

// TODO: when someone is storing 
public class ConnectionHandler implements Runnable {

	public enum ServerType {
		CONTROLLER, DSTORE
	}

	public static final String NAME = ConnectionHandler.class.getName();

	private Connection connection;
	private BufferedReader in;
	private ServerType serverType;
	// TODO: BIG ASSUMPTION!, no new load between reloads
	private ArrayList<Connection> dstores;
	private boolean isDstore;

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
		dstores = new ArrayList<>();
		try {
			if (serverType == ServerType.CONTROLLER) {
				readCommands(controllerParser);
			} else {
				readCommands(dstoreParser);
			}
		} catch (IOException e) {
			TerminalLog.printErr(NAME, connection.getPort() + " - Failed to read input from the connection!");
			e.printStackTrace();
		} finally {
			try {
				TerminalLog.printMes(NAME, connection.getPort() + " - Closing down the connection");
				if (isDstore) {
					TerminalLog.printMes(NAME,
							connection.getPort() + " - Connection was a dstore! Removing the dstore from the list");
					Controller.removeDstore(connection);
				}
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void readCommands(CommandParser parser) throws IOException {
		TerminalLog.printMes(NAME, connection.getPort() + " - Waiting for the command");
		String line;

		while ((line = in.readLine()) != null) {

			TerminalLog.printMes(NAME, connection.getPort() + " - Command successfully read!");

			String[] command = line.split(" ");
			TerminalLog.printMes(NAME, connection.getPort() + " Command: " + Arrays.toString(command));
			Handler handler = parser.parse(command, TerminalLog.stampMes(line));

			String[] args = Arrays.copyOfRange(command, 1, command.length);

			if (handler != null) {
				handler.handle(args, connection);
			} else {
				TerminalLog.printMes(NAME, connection.getPort() + " - Ignoring the command");
			}

			TerminalLog.printMes(NAME, connection.getPort() + " - Waiting for the command");
		}

		TerminalLog.printMes(NAME, connection.getPort() + "- terminated connection");
	}

	public CommandParser dstoreParser = (String[] command, String mes) -> {
		DstoreLogger.getInstance().messageReceived(connection.getSocket(), mes);
		isDstore = false;
		switch (command[0]) {
			case Protocol.STORE_TOKEN:
				return DstoreServerHandler.storeHandler;
			case Protocol.LOAD_DATA_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - New request to load a file!");
				return DstoreServerHandler.loadHandler;
			default:
				// TODO: Logger and Handler
				TerminalLog.printMes(NAME, connection.getPort()
						+ " - this command hasn't been implemented yet or it doesn't exist: " + command[0]);
				return null;
		}
	};

	public CommandParser controllerParser = (String[] command, String mes) -> {
		ControllerLogger.getInstance().messageReceived(connection.getSocket(), mes);
		switch (command[0]) {
			case Protocol.JOIN_TOKEN:
				isDstore = true;
				return DstoreHandler.joinHandler;
			case Protocol.STORE_ACK_TOKEN:
				return ClientHandler.storeAcknowledgeHandler;
			case Protocol.LIST_TOKEN:
				isDstore = false;
				TerminalLog.printMes(NAME, connection.getPort() + " - Client Request!");
				return ClientHandler.listHandler;
			case Protocol.LOAD_TOKEN:
				isDstore = false;
				TerminalLog.printMes(NAME, connection.getPort() + " - Client Request!");

				if (!Controller.isEnoughDstores()) {
					return ClientHandler.notEnoughDstoresHandler;
				} else {
					return (String[] args, Connection connection) -> {
						String filename = args[0];

						if (!Controller.indexExists(filename)
								|| Controller.getIndexState(filename) == Index.State.STORE_IN_PROGRESS
								|| Controller.getIndexState(filename) == Index.State.REMOVE_IN_PROGRESS) {
							TerminalLog.printErr("LoadHandler",
									connection.getPort() + " - File '" + filename + "' doesn't exist");
							ControllerLogger.getInstance().messageSent(connection.getSocket(),
									Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
							connection.getOutWriter().println(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
							return;
						}

						dstores = Controller.getIndexServers(filename);
						String[] argsUpdated = new String[args.length + 1];
						for (int i = 0; i < args.length; i++) {
							argsUpdated[i] = args[i];
						}
						argsUpdated[args.length] = Integer.toString(Controller.getDstoreServerPort(dstores.remove(0)));
						ClientHandler.subLoadHandler.handle(argsUpdated, connection);
					};
				}
			case Protocol.RELOAD_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - Client Request!");
				isDstore = false;

				if (!Controller.isEnoughDstores()) {
					TerminalLog.printErr(NAME,
							connection.getPort() + " - Cannot connect! Not enough dstores joined the network");
					return (String[] args, Connection connection) -> connection.getOutWriter()
							.println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
				} else {
					if (dstores.size() == 0) {
						return (String[] args, Connection connection) -> connection.getOutWriter()
								.println(Protocol.ERROR_LOAD_TOKEN);
					}
					return (String[] args, Connection connection) -> {
						String filename = args[0];
						if (!Controller.indexExists(filename)
								|| Controller.getIndexState(filename) == Index.State.STORE_IN_PROGRESS
								|| Controller.getIndexState(filename) == Index.State.REMOVE_IN_PROGRESS) {
							TerminalLog.printErr("LoadHandler",
									connection.getPort() + " - File '" + filename + "' doesn't exist");
							ControllerLogger.getInstance().messageSent(connection.getSocket(),
									Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
							connection.getOutWriter().println(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
							return;
						}

						String[] argsUpdated = new String[args.length + 1];
						for (int i = 0; i < args.length; i++) {
							argsUpdated[i] = args[i];
						}
						argsUpdated[args.length] = Integer.toString(Controller.getDstoreServerPort(dstores.remove(0)));
						ClientHandler.subLoadHandler.handle(argsUpdated, connection);
					};
				}

			case Protocol.STORE_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - Client Request!");
				isDstore = false;

				if (!Controller.isEnoughDstores()) {
					return ClientHandler.notEnoughDstoresHandler;
				} else {
					return ClientHandler.storeHandler;
				}
			default:
				// TODO: Logger and handler
				isDstore = false;
				TerminalLog.printMes(NAME, connection.getPort() + " - Invalid command sent: " + command[0]);
				return null;
		}

	};
}
