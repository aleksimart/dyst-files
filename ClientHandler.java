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
		String name = args[0];
		int filesize = Integer.parseInt(args[1]);

		if (!Controller.addIndex(name, filesize, connection)) {
			connection.getOutWriter().println(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
			return;
		}

		// TODO: check for the value is null
		Integer[] ports = Controller.getDstores(filesize);
		if (ports == null) {
			System.out.println("Server error: client handler failed to get the dstore ports for connection "
					+ connection.getPort());
			connection.getOutWriter().println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
			return;
		}

		StringBuilder command = new StringBuilder();
		command.append(Protocol.STORE_TO_TOKEN);

		for (Integer port : ports) {
			command.append(" ");
			command.append(port);
		}

		connection.getOutWriter().println(command);

		/**
		 * TODO: this isn't the best way. The proper way would be to get all the dstore
		 * sockets in one place and call the readline on them with timeout and throw a
		 * timeout socket exception if they are not done on time which means failed
		 * storage.
		 */
		try {
			Thread.sleep(Controller.getTimeout());
			if (Controller.getIndexState(name) != Index.State.STORE_COMPLETE) {
				Controller.deleteIndex(name);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	};

	public static Handler loadHandler = (String[] args, Connection connection) -> {
		int dstorePort = Controller.getfileServer(args[0]);
		connection.getOutWriter().println(Protocol.LOAD_FROM_TOKEN + " " + dstorePort);
	};
}
