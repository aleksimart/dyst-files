import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ConnectionHandler implements Runnable {

	public enum ServerType {
		CONTROLLER, DSTORE
	}

	public static final String NAME = ConnectionHandler.class.getName();

	private Connection connection;
	private BufferedReader in;
	private ServerType serverType;
	private ArrayList<Connection> dstores;

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
			Handler handler = parser.parse(command);

			String[] args = Arrays.copyOfRange(command, 1, command.length);
			handler.handle(args, connection);

			TerminalLog.printMes(NAME, connection.getPort() + " - Waiting for the command");
		}

		TerminalLog.printMes(NAME, connection.getPort() + "- terminated connection");
	}

	public CommandParser dstoreParser = (String[] command) -> {
		switch (command[0]) {
			case Protocol.STORE_TOKEN:
				return DstoreServerHandler.storeHandler;
			case Protocol.LOAD_DATA_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - New request to load a file!");
				return DstoreServerHandler.loadHandler;
			default:
				TerminalLog.printMes(NAME, connection.getPort()
						+ " - this command hasn't been implemented yet or it doesn't exist: " + command[0]);
				return null;
		}
	};

	public CommandParser controllerParser = (String[] command) -> {
		// ControllerLogger.getInstance().messageReceived(connection.getSocket(), line);
		switch (command[0]) {
			case Protocol.JOIN_TOKEN:
				return DstoreHandler.joinHandler;
			case Protocol.STORE_ACK_TOKEN:
				return (String[] args, Connection connection) -> {
					if (!Controller.indexExists(command[1])) {
						TerminalLog.printMes(NAME,
								connection.getPort() + " - No longer can acknowledge the file " + command[1]);
						return;
					}

					if (Controller.ackIndex(command[1], connection)) {
						Controller.getStorer(command[1]).getOutWriter().println(Protocol.STORE_COMPLETE_TOKEN);
					}
				};
			case Protocol.LOAD_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - New client attempts to join the network!");

				if (!Controller.isEnoughDstores()) {
					TerminalLog.printErr(NAME,
							connection.getPort() + " - Cannot connect! Not enough dstores joined the network");
					return (String[] args, Connection connection) -> connection.getOutWriter()
							.println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
				} else {
					return (String[] args, Connection connection) -> {
						String filename = args[0];

						if (!Controller.indexExists(filename)) {
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
						ClientHandler.loadHandler.handle(argsUpdated, connection);
					};
				}
			case Protocol.RELOAD_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - New client attempts to join the network!");

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
						String[] argsUpdated = new String[args.length + 1];
						for (int i = 0; i < args.length; i++) {
							argsUpdated[i] = args[i];
						}
						argsUpdated[args.length] = Integer.toString(Controller.getDstoreServerPort(dstores.remove(0)));
						ClientHandler.loadHandler.handle(argsUpdated, connection);
					};
				}

			case Protocol.STORE_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - New client attempts to join the network!");

				if (!Controller.isEnoughDstores()) {
					TerminalLog.printErr(NAME,
							connection.getPort() + " - Cannot connect! Not enough dstores joined the network");
					connection.getOutWriter().println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
					return (String[] args, Connection connection) -> connection.getOutWriter()
							.println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
				} else {
					return ClientHandler.storeHandler;
				}
			default:
				TerminalLog.printMes(NAME, connection.getPort() + " - Invalid command sent: " + command[0]);
				return null;
		}

	};
}
