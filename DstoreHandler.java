public class DstoreHandler {

	// private String command;
	// private String[] args;
	// private Connection connection;
	public static String NAME = DstoreHandler.class.getName();

	// public DstoreHandler(Connection connection, String[] args) {
	// command = args[0];
	// this.args = new String[args.length - 1];
	// this.connection = connection;

	// for (int i = 1; i < args.length; i++) {
	// this.args[i - 1] = args[i];
	// }

	// }

	// @Override
	// public void handle() {
	// switch (command) {
	// case Protocol.JOIN_TOKEN:
	// join();
	// break;
	// default:
	// System.err.println("Err, not implemented yet, or wrong protocol: " +
	// command);
	// }
	// }

	public static Handler joinHandler = (String[] args, Connection connection) -> {
		TerminalLog.printMes(NAME, connection.getPort() + " - New dstore just joined the ranks!");
		TerminalLog.printMes(NAME, connection.getPort() + " - Declared port: " + args[0]);
		int port = Integer.parseInt(args[0]);
		Controller.addDstore(connection, port);
		ControllerLogger.getInstance().dstoreJoined(connection.getSocket(), port);
		TerminalLog.printMes(NAME, connection.getPort() + " - Dstore successfully added to the list!");
	};
}
