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

	// TODO: Argument check
	public static Handler storeHandler = (String[] args, Connection connection) -> {
		// Check args number
		if (args.length != 2) {
			TerminalLog.printHandlerErrMes("StoreHandler", connection.getPort(),
					"Invalid number of args, expected: 2, but got: " + args.length);
			return;
		}

		String filename = args[0];
		int filesize;

		// Check second arg is int
		try {
			filesize = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			TerminalLog.printHandlerErrMes("StoreHandler", connection.getPort(),
					"Error, second argument is expected to be an integer");
			return;
		}

		// Check that it is possible to add that index
		if (!Controller.addIndex(filename, filesize, connection)) {
			TerminalLog.printHandlerErrMes("StoreHandler", connection.getPort(),
					"Error! File '" + filename + "' already exists!");
			Handler.sendConrollerMes(connection, Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
			return;
		}

		// TODO: Exceptional case that shouldn't happen
		Integer[] ports = Controller.getDstores(filesize);
		if (ports == null) {
			TerminalLog.printHandlerErrMes("StoreHandler", connection.getPort(),
					" - Server error: client handler failed to get the dstore ports for storing '" + filename + "'");
			Handler.sendConrollerMes(connection, Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
			return;
		}

		// Build up the command
		StringBuilder command = new StringBuilder();
		command.append(Protocol.STORE_TO_TOKEN);

		for (Integer port : ports) {
			command.append(" ");
			command.append(port);
		}

		TerminalLog.printHandlerMes("StoreHandler", connection.getPort(),
				"Found the dstores for '" + filename + "', sending the relevant command");
		Handler.sendConrollerMes(connection, command.toString());

		// Timeout check
		try {
			if (Controller.getIndexTimer(filename).get() != Index.Timeout.SUCCESSFULL) {
				TerminalLog.printHandlerErrMes("StoreHandler", connection.getPort(),
						"Failed to store the file '" + filename + "' before timeout");
				Controller.deleteIndex(filename);
				TerminalLog.printHandlerMes("StoreHandler", connection.getPort(),
						"File '" + filename + "' has been deleted");
				return;
			}

			TerminalLog.printHandlerMes("StoreHandler", connection.getPort(),
					"File '" + filename + "' has been successfully stored!");
			Handler.sendConrollerMes(connection, Protocol.STORE_COMPLETE_TOKEN);

		} catch (InterruptedException | ExecutionException e1) {
			TerminalLog.printHandlerErrMes("StoreHandler", connection.getPort(), "Server error, refer to stack trace");
			e1.printStackTrace();
		}

	};

	public static Handler storeAcknowledgeHandler = (String[] args, Connection connection) -> {
		// Check args number
		if (args.length != 1) {
			TerminalLog.printHandlerErrMes("Store Acknowledgement Handler", connection.getPort(),
					"Invalid number of args, expected: 2, but got: " + args.length);
			return;
		}

		String filename = args[0];

		if (!Controller.indexExists(filename)) {
			TerminalLog.printHandlerErrMes("Store Acknowledgment Handler", connection.getPort(),
					"No longer can acknowledge the file " + args[0]);
			return;
		}

		Controller.ackStorageIndex(filename, connection);
	};

	// TODO: Possibly give a handler a return type of the message to log?
	public static Handler listHandler = (String[] args, Connection connection) -> {
		// Check args number
		if (args.length != 0) {
			TerminalLog.printHandlerErrMes("ListHandler", connection.getPort(),
					"Invalid number of args, expected: 0, but got: " + args.length);
			return;
		}

		TerminalLog.printHandlerMes("ListHandler", connection.getPort(), "Preparing to List the files");
		ArrayList<String> files = Controller.listFiles();

		StringBuilder command = new StringBuilder(Protocol.LIST_TOKEN);
		for (String file : files) {
			command.append(" " + file);
		}

		Handler.sendConrollerMes(connection, command.toString());
		TerminalLog.printHandlerMes("ListHandler", connection.getPort(), "Successfully listed the files");
	};

	public static Handler removeHandler = (String[] args, Connection connection) -> {
		// Check args number
		if (args.length != 1) {
			TerminalLog.printHandlerErrMes("RemoveHandler", connection.getPort(),
					"Invalid number of args, expected: 1, but got: " + args.length);
			return;
		}

		String filename = args[0];

		try {
			ArrayList<Connection> dstores = Controller.startIndexRemoval(filename);

			for (Connection dstore : dstores) {
				TerminalLog.printMes("RemoveHandler", "Index File '" + filename
						+ "' - Attempting to delete the file for dstore: " + dstore.getPort());

				// TODO: do I even need threads here?
				new Thread(() -> {
					Handler.sendConrollerMes(dstore, Protocol.REMOVE_TOKEN + " " + filename);
					TerminalLog.printHandlerMes("RemoveHandler", connection.getPort(), "Index File '" + filename
							+ "' - Sent Request to delete the file for dstore: " + dstore.getPort());

				}).start();

			}

			// Timeout Check
			if (Controller.getIndexTimer(filename).get() != Index.Timeout.SUCCESSFULL) {
				TerminalLog.printHandlerErrMes("RemoveHandler", connection.getPort(),
						"Failed to remove the file '" + filename + "' before timeout");
				return;
			}

			TerminalLog.printHandlerMes("RemoveHandler", connection.getPort(),
					"File '" + filename + "' has been successfully removed!");
			Controller.deleteIndex(filename);
			Handler.sendConrollerMes(connection, Protocol.REMOVE_COMPLETE_TOKEN);

		} catch (InterruptedException | ExecutionException e) {
			TerminalLog.printHandlerErrMes("RemoveHandler", connection.getPort(), "Server error, refer to stack trace");
			e.printStackTrace();
		}
	};

	public static Handler removeAcknowledgeHandler = (String[] args, Connection connection) -> {
		// Check args number
		if (args.length != 1) {
			TerminalLog.printHandlerErrMes("Remove Acknowledgement Handler", connection.getPort(),
					"Invalid number of args, expected: 2, but got: " + args.length);
			return;
		}

		String filename = args[0];

		if (!Controller.indexExists(filename)) {
			TerminalLog.printHandlerErrMes("Remove Acknowledgment Handler", connection.getPort(),
					"No longer can acknowledge the file " + filename);
			return;
		}

		Controller.ackRemovalIndex(filename, connection);
	};

	// Special handler that needs to be instance specific
	public Handler loadHandler = (String[] args, Connection connection) -> {
		// Check args number
		if (args.length != 1) {
			TerminalLog.printHandlerErrMes("LoadHandler", connection.getPort(),
					"Invalid number of args, expected: 1, but got: " + args.length);
			return;
		}

		String filename = args[0];

		if (!Controller.indexExists(filename) || Controller.getIndexState(filename) != Index.State.STORE_COMPLETE) {
			TerminalLog.printHandlerErrMes("LoadHandler", connection.getPort(),
					"File '" + filename + "' doesn't exist");
			Handler.sendConrollerMes(connection, Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
			return;
		}

		int dstorePort = Controller.getDstoreServerPort(dstoresLoad.remove(0));

		TerminalLog.printHandlerMes("LoadHandler", connection.getPort(),
				"Found where to load the file '" + filename + "' from");
		Handler.sendConrollerMes(connection, Protocol.LOAD_FROM_TOKEN + " " + dstorePort);

	};

	/**
	 * Error handlers
	 */
	public static Handler notEnoughDstoresHandler = (String[] args, Connection connection) -> {
		TerminalLog.printHandlerErrMes("Not Enough Dstores Handler", connection.getPort(),
				"Cannot connect! Not enough dstores joined the network");
		Handler.sendConrollerMes(connection, Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
	};

	// TODO: Don't forget to put extra argument here
	public static Handler errorLoadingHandler = (String[] args, Connection connection) -> {
		TerminalLog.printHandlerErrMes("Error Loading File Handler", connection.getPort(),
				"Cannot Load the requested file '" + args[0] + "'");
		Handler.sendConrollerMes(connection, Protocol.ERROR_LOAD_TOKEN);
	};

	public ArrayList<Connection> getDstoresLoad() {
		return dstoresLoad;
	}
}
