/**
 * ClientHandler
 */
public class ClientHandler implements Handler {

	String command;
	Connection connection;

	public ClientHandler(String command, Connection connection) {
		this.command = command;
		this.connection = connection;
	}

	public void handle() {
		switch (command) {
			case Protocol.STORE_TOKEN:
				storeFile();
			default:
				System.err.println("Err, not implemented yet, or wrong protocol: " + command);
		}
	}

	public void storeFile() {

	}
}
