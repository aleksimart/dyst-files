import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * ClientHandler
 */
public class ClientHandler {

	private ArrayList<Connection> dstoresLoad;

	public ClientHandler(String filename) {
		dstoresLoad = Controller.getIndexServers(filename);
	}

	public static Handler storeHandler = (String[] args, Connection connection) -> {
		String filename = args[0];
		int filesize = Integer.parseInt(args[1]);

		if (!Controller.addIndex(filename, filesize, connection)) {
			TerminalLog.printErr("StoreHandler",
					connection.getPort() + " - Error! File '" + filename + "' already exists!");
			connection.getOutWriter().println(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
			return;
		}

		Integer[] ports = Controller.getDstores(filesize);
		if (ports == null) {
			TerminalLog.printErr("StoreHandler", connection.getPort()
					+ " - Server error: client handler failed to get the dstore ports for storing '" + filename + "'");
			System.out.println("" + connection.getPort());
			ControllerLogger.getInstance().messageSent(connection.getSocket(),
					TerminalLog.stampMes(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN));
			connection.getOutWriter().println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
			return;
		}

		StringBuilder command = new StringBuilder();
		command.append(Protocol.STORE_TO_TOKEN);

		for (Integer port : ports) {
			command.append(" ");
			command.append(port);
		}

		TerminalLog.printMes("StoreHandler",
				connection.getPort() + " - found the dstores for '" + filename + "', sending the relevant command");
		ControllerLogger.getInstance().messageSent(connection.getSocket(), TerminalLog.stampMes(command.toString()));
		connection.getOutWriter().println(command);

		try {
			if (Controller.getIndexTimer(filename).get() != Index.Timeout.SUCCESSFULL) {
				TerminalLog.printErr("StoreHandler",
						connection.getPort() + " - Failed to store the file '" + filename + "' before timeout");
				Controller.deleteIndex(filename);
				TerminalLog.printMes("StoreHandler",
						connection.getPort() + " - File '" + filename + "' has been deleted");
				return;
			}

			TerminalLog.printMes("StoreHandler",
					connection.getPort() + " - File '" + filename + "' has been successfully stored!");
		} catch (InterruptedException | ExecutionException e1) {
			e1.printStackTrace();
		}

	};

	// TODO: Possibly give a handler a return type of the message to log?
	public static Handler listHandler = (String[] args, Connection connection) -> {
		ArrayList<String> files = Controller.listFiles();
		StringBuilder command = new StringBuilder(Protocol.LIST_TOKEN);
		for (String file : files) {
			command.append(" " + file);
		}

		connection.getOutWriter().println(command);
	};

	public static Handler removeHandler = (String[] args, Connection connection) -> {
		String filename = args[0];
		try {
			ArrayList<Connection> dstores = Controller.startIndexRemoval(filename);

			for (Connection dstore : dstores) {
				TerminalLog.printMes("Remove Handler", "Index File '" + filename
						+ "' - Attempting to delete the file for dstore: " + dstore.getPort());

				// TODO: do I even need threads here?
				new Thread(() -> {
					dstore.getOutWriter().println(Protocol.REMOVE_TOKEN + " " + filename);
					TerminalLog.printMes("Remove Handler", "Index File '" + filename
							+ "' - Sent Request to delete the file for dstore: " + dstore.getPort());

				}).start();

			}

			if (Controller.getIndexTimer(filename).get() != Index.Timeout.SUCCESSFULL) {
				TerminalLog.printErr("RemoveHandler",
						connection.getPort() + " - Failed to remove the file '" + filename + "' before timeout");
				// Controller.deleteIndex(filename);
				// TerminalLog.printMes("StoreHandler",
				// connection.getPort() + " - File '" + filename + "' has been deleted");
				return;
			}
			TerminalLog.printMes("RemoveHandler",
					connection.getPort() + " - File '" + filename + "' has been successfully removed!");
			Controller.deleteIndex(filename);
			ControllerLogger.getInstance().messageSent(connection.getSocket(),
					TerminalLog.stampMes(Protocol.REMOVE_COMPLETE_TOKEN));
			connection.getOutWriter().println(Protocol.REMOVE_COMPLETE_TOKEN);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	};

	public static Handler removeAcknowledgeHandler = (String[] args, Connection connection) -> {
		if (Controller.ackRemovalIndex(args[0], connection) == Index.Timeout.SUCCESSFULL) {
			// ControllerLogger.getInstance().messageSent(connection.getSocket(),
			// TerminalLog.stampMes(Protocol.REMOVE_COMPLETE_TOKEN));
			// TODO: not sure if i need to do this anymore with the whole future stuff
			// Controller.getStorer(args[0]).getOutWriter().println(Protocol.REMOVE_COMPLETE_TOKEN);
		}

	};

	// // TODO: move all that load mess from connection handler to here
	// public static Handler subLoadHandler = (String[] args, Connection connection)
	// -> {
	// String filename = args[0];
	// int dstorePort = Integer.parseInt(args[1]);

	// TerminalLog.printErr("LoadHandler",
	// connection.getPort() + " - Found where to load the file '" + filename + "'
	// from");
	// ControllerLogger.getInstance().messageSent(connection.getSocket(),
	// TerminalLog.stampMes(Protocol.LOAD_FROM_TOKEN + " " + dstorePort));
	// connection.getOutWriter().println(Protocol.LOAD_FROM_TOKEN + " " +
	// dstorePort);
	// };

	public Handler loadHandler = (String[] args, Connection connection) -> {
		String filename = args[0];

		if (!Controller.indexExists(filename) || Controller.getIndexState(filename) != Index.State.STORE_COMPLETE) {
			TerminalLog.printErr("LoadHandler", connection.getPort() + " - File '" + filename + "' doesn't exist");
			Handler.sendConrollerMes(connection, Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
			return;
		}

		int dstorePort = Controller.getDstoreServerPort(dstoresLoad.remove(0));

		TerminalLog.printErr("LoadHandler",
				connection.getPort() + " - Found where to load the file '" + filename + "' from");
		Handler.sendConrollerMes(connection, Protocol.LOAD_FROM_TOKEN + " " + dstorePort);

	};

	public static Handler storeAcknowledgeHandler = (String[] args, Connection connection) -> {
		if (!Controller.indexExists(args[0])) {
			TerminalLog.printMes("Store Acknowledgment Handler",
					connection.getPort() + " - No longer can acknowledge the file " + args[0]);
			return;
		}

		if (Controller.ackStorageIndex(args[0], connection) == Index.Timeout.SUCCESSFULL) {
			ControllerLogger.getInstance().messageSent(connection.getSocket(),
					TerminalLog.stampMes(Protocol.STORE_COMPLETE_TOKEN));
			Controller.getStorer(args[0]).getOutWriter().println(Protocol.STORE_COMPLETE_TOKEN);
		}
	};

	public static Handler notEnoughDstoresHandler = (String[] args, Connection connection) -> {
		TerminalLog.printErr("Not Enough Dstores Handler",
				connection.getPort() + " - Cannot connect! Not enough dstores joined the network");
		ControllerLogger.getInstance().messageSent(connection.getSocket(),
				TerminalLog.stampMes(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN));
		connection.getOutWriter().println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
	};

	public static Handler errorLoadingHandler = (String[] args, Connection connection) -> connection.getOutWriter()
			.println(Protocol.ERROR_LOAD_TOKEN);

	public ArrayList<Connection> getDstoresLoad() {
		return dstoresLoad;
	}
}
