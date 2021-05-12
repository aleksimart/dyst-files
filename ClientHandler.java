import java.util.concurrent.ExecutionException;

/**
 * ClientHandler
 */
public class ClientHandler {

	// public ClientHandler(Connection connection, String[] args) {
	// command = args[0];
	// this.args = new String[args.length - 1];
	// this.connection = connection;

	// for (int i = 1; i < args.length; i++) {
	// this.args[i - 1] = args[i];
	// }
	// }

	// public void handle() {
	// switch (command) {
	// case Protocol.STORE_TOKEN:
	// System.out.println("Arg 1: " + args[0] + " Args 2: " + args[1]);
	// storeFile(args[0], Integer.parseInt(args[1]));
	// break;
	// default:
	// System.err.println("Client: Err, not implemented yet, or wrong protocol: " +
	// command);
	// }
	// }

	public static Handler storeHandler = (String[] args, Connection connection) -> {
		String filename = args[0];
		int filesize = Integer.parseInt(args[1]);

		if (!Controller.addIndex(filename, filesize, connection)) {
			TerminalLog.printErr("StoreHandler",
					connection.getPort() + " - Error! File '" + filename + "' already exists!");
			connection.getOutWriter().println(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
			return;
		}

		// TODO: check for the value is null
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

		// /**
		// * TODO: this isn't the best way. The proper way would be to get all the
		// dstore
		// * sockets in one place and call the readline on them with timeout and throw a
		// * timeout socket exception if they are not done on time which means failed
		// * storage.
		// */
		// new Thread(() -> {
		// try {
		// TerminalLog.printMes("StoreHandler",
		// connection.getPort() + " - Starting a timeout to store a file '" + filename +
		// "'");
		// Thread.sleep(Controller.getTimeout());
		// if (Controller.getIndexState(filename) != Index.State.STORE_COMPLETE) {
		// TerminalLog.printErr("StoreHandler",
		// connection.getPort() + " - Failed to store the file '" + filename + "' before
		// timeout");
		// Controller.deleteIndex(filename);
		// TerminalLog.printMes("StoreHandler",
		// connection.getPort() + " - File '" + filename + "' has been deleted");
		// return;
		// }
		// TerminalLog.printMes("StoreHandler",
		// connection.getPort() + " - File '" + filename + "' has been successfully
		// stored!");
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }

		// }).start();

	};

	// TODO: move all that load mess from connection handler to here
	public static Handler subLoadHandler = (String[] args, Connection connection) -> {
		String filename = args[0];
		int dstorePort = Integer.parseInt(args[1]);

		TerminalLog.printErr("LoadHandler",
				connection.getPort() + " - Found where to load the file '" + filename + "' from");
		ControllerLogger.getInstance().messageSent(connection.getSocket(),
				TerminalLog.stampMes(Protocol.LOAD_FROM_TOKEN + " " + dstorePort));
		connection.getOutWriter().println(Protocol.LOAD_FROM_TOKEN + " " + dstorePort);
	};

	public static Handler storeAcknowledgeHandler = (String[] args, Connection connection) -> {
		if (!Controller.indexExists(args[0])) {
			TerminalLog.printMes("Store Acknowledgment Handler",
					connection.getPort() + " - No longer can acknowledge the file " + args[0]);
			return;
		}

		if (Controller.ackIndex(args[0], connection) == Index.Timeout.SUCCESSFULL) {
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
}
