import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

// TODO: when someone is storing 
public class ConnectionHandler implements Runnable {

	public enum ServerType {
		CONTROLLER, DSTORE_SERVER, DSTORE
	}

	public static final String NAME = ConnectionHandler.class.getName();

	private Connection connection;
	private BufferedReader in;
	private ServerType serverType;
	// TODO: BIG ASSUMPTION!, no new load between reloads
	private ClientHandler loader;
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
		try {
			switch (serverType) {
				case CONTROLLER:
					readCommands(controllerParser);
					break;
				case DSTORE_SERVER:
				case DSTORE:
					// TODO: separate
					readCommands(dstoreParser);
					break;
			}
		} catch (IOException e) {
			TerminalLog.printHandlerErrMes(NAME, connection.getPort(), "Failed to read input from the connection!");
			e.printStackTrace();
		} finally {
			try {
				TerminalLog.printHandlerMes(NAME, connection.getPort(), "Closing down the connection");

				if (isDstore) {
					TerminalLog.printHandlerMes(NAME, connection.getPort(),
							"Connection was a dstore! Removing the dstore from the list");
					Controller.removeDstore(connection);
				}

				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void readCommands(CommandParser parser) throws IOException {
		TerminalLog.printHandlerMes(NAME, connection.getPort(), "Waiting for the command");
		String line;

		while ((line = in.readLine()) != null) {

			TerminalLog.printHandlerMes(NAME, connection.getPort(), "Command successfully read!");

			String[] command = line.split(" ");
			TerminalLog.printHandlerMes(NAME, connection.getPort(), "Command: " + Arrays.toString(command));
			Handler handler = parser.parse(command, TerminalLog.stampMes(line));

			String[] args = Arrays.copyOfRange(command, 1, command.length);

			if (handler != null) {
				handler.handle(args, connection);
			} else {
				TerminalLog.printHandlerMes(NAME, connection.getPort(), "No handler, ignoring the command");
			}

			TerminalLog.printHandlerMes(NAME, connection.getPort(), "Waiting for the command");
		}

		TerminalLog.printHandlerMes(NAME, connection.getPort(), "Terminated connection");
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
			case Protocol.REMOVE_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - New request to delete a file!");
				// TODO: this actually comes from controller to dstore and not server ;c
				return DstoreServerHandler.removeHandler;
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
			case Protocol.REMOVE_ACK_TOKEN:
				return ClientHandler.removeAcknowledgeHandler;
			case Protocol.LIST_TOKEN:
				isDstore = false;
				TerminalLog.printMes(NAME, connection.getPort() + " - Client Request!");
				return ClientHandler.listHandler;
			case Protocol.REMOVE_TOKEN:
				TerminalLog.printMes(NAME, connection.getPort() + " - Client Request!");
				isDstore = false;

				if (!Controller.isEnoughDstores()) {
					return ClientHandler.notEnoughDstoresHandler;
				} else {
					return ClientHandler.removeHandler;
				}
			case Protocol.LOAD_TOKEN:
				isDstore = false;
				TerminalLog.printMes(NAME, connection.getPort() + " - Client Request!");

				// TODO: ughh assumption here that args are fine
				loader = new ClientHandler(command[1]);

				if (!Controller.isEnoughDstores()) {
					return ClientHandler.notEnoughDstoresHandler;
				} else {
					return loader.loadHandler;
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
					if (loader.getDstoresLoad().size() == 0) {
						return ClientHandler.errorLoadingHandler;
					}
					return loader.loadHandler;
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
