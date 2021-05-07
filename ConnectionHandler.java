import java.io.*;
import java.net.*;

public class ConnectionHandler implements Runnable {
	public static final String NAME = ConnectionHandler.class.getName();

	private Connection connection;
	private Socket socket;
	private BufferedReader in;

	public ConnectionHandler(Socket socket) {
		try {
			connection = new Connection(socket);
			in = connection.getIn();
		} catch (IOException e) {
			TerminalLog.printErr(NAME,
					socket.getPort() + " - Failed to get the input or output stream of the connection");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void run() {
		TerminalLog.printMes(NAME, socket.getPort() + " - Waiting for the command");

		String firstLine;
		try {
			firstLine = in.readLine();
			String[] command = firstLine.split(" ");
			checkArgs(command);
			TerminalLog.printMes(NAME, socket.getPort() + " - Command successfully read!");
			typeOfConnection(command);
		} catch (IOException e) {
			TerminalLog.printErr(NAME, socket.getPort() + " - Failed to read input from the connection!");
			e.printStackTrace();
		} finally {
			try {
				TerminalLog.printMes(NAME, socket.getPort() + " - Closing down the connection");
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// TODO: implement
	public void checkArgs(String[] command) {
		TerminalLog.printMes(NAME, socket.getPort() + " - verifying validity of the command");
	}

	public void typeOfConnection(String[] command) {
		switch (command[0]) {
			case Protocol.JOIN_TOKEN:
				TerminalLog.printMes(NAME, socket.getPort() + " - New dstore just joined the ranks!");
				TerminalLog.printMes(NAME, socket.getPort() + " - Declared port: " + command[1]);
				Controller.addDstore(new DstoreHandler(socket));
				TerminalLog.printMes(NAME, socket.getPort() + " - Dstore successfully added to the list!");
				while (true) {

				}
				// break;
			case Protocol.LIST_TOKEN:
			case Protocol.STORE_TOKEN:
			case Protocol.LOAD_TOKEN:
			case Protocol.LOAD_DATA_TOKEN:
			case Protocol.RELOAD_TOKEN:
			case Protocol.REMOVE_TOKEN:
				TerminalLog.printMes(NAME, socket.getPort() + " - New client attempts to join the network!");

				if (!Controller.isEnoughDstores()) {
					TerminalLog.printErr(NAME,
							socket.getPort() + " - Cannot connect! Not enough dstores joined the network");
				}
				// TODO: This is client, appropriate handler + log
				break;
			default:
				TerminalLog.printMes(NAME, socket.getPort() + " - Invalid command");
				// TODO: This is an invalid command, should log it and ignore
		}

	}
}
