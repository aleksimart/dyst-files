import java.io.*;
import java.net.*;

public class ConnectionHandler implements Runnable {
	public static final String NAME = ConnectionHandler.class.getName();

	private Socket socket;
	private BufferedReader reader;
	private InputStream in;

	public ConnectionHandler(Socket socket) {
		this.socket = socket;
		try {
			in = socket.getInputStream();
			reader = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			TerminalLog.printErr(NAME, socket.getPort() + " - Failed to get the input stream of the connection");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void run() {
		TerminalLog.printMes(NAME, socket.getPort() + " - Waiting for the command");

		String firstLine;
		try {
			firstLine = reader.readLine();
			// TODO: check the arg number
			// ControllerLogger.getInstance().messageReceived(socket, firstLine);
			String[] command = firstLine.split(" ");
			TerminalLog.printMes(NAME, socket.getPort() + " - Command successfully read!");
			typeOfConnection(command);
		} catch (IOException e) {
			TerminalLog.printErr(NAME, socket.getPort() + " - Failed to read input from the connection!");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		} finally {
			closeConnection();
		}

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

	public void closeConnection() {
		try {
			TerminalLog.printMes(NAME, socket.getPort() + " - Closing down the connection");
			socket.shutdownOutput();
			socket.shutdownInput();
			reader.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
