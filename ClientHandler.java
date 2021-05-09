import java.util.concurrent.CompletableFuture;

/**
 * ClientHandler
 */
public class ClientHandler implements Handler {

	private String command;
	private String[] args;
	private Connection connection;

	public ClientHandler(Connection connection, String[] args) {
		command = args[0];
		this.args = new String[args.length - 1];
		this.connection = connection;

		for (int i = 1; i < args.length; i++) {
			this.args[i - 1] = args[i];
		}
	}

	public void handle() {
		switch (command) {
			case Protocol.STORE_TOKEN:
				System.out.println("Arg 1: " + args[0] + " Args 2: " + args[1]);
				storeFile(args[0], Integer.parseInt(args[1]));
				break;
			default:
				System.err.println("Client: Err, not implemented yet, or wrong protocol: " + command);
		}
	}

	/**
	 * <p>
	 * 1. Create index <br>
	 * 2. Pull out the relevant dstores (what type should be stored?)
	 * </p>
	 */
	public void storeFile(String name, int filesize) {
		Controller.addIndex(name, filesize, connection);

		// TODO: check for the value is null
		Integer[] ports = Controller.getDstores(filesize);

		StringBuilder command = new StringBuilder();
		command.append(Protocol.STORE_TO_TOKEN);

		for (Integer port : ports) {
			command.append(" ");
			command.append(port);
		}

		connection.getOutWriter().println(command);
	}
}
