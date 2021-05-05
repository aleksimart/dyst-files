import java.io.*;
import java.net.*;

public class ConnectionHandler implements Runnable {
	private Socket socket;
	private BufferedReader reader;
	private InputStream in;

	public ConnectionHandler(Socket socket) {
		this.socket = socket;
		try {
			in = socket.getInputStream();
			reader = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			printErr("Failed to get the input stream of the connection");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	public void printMes(String mes) {
		System.out.println("[CONNECTION HANDLER: " + socket.getPort() + "]: " + mes);
	}

	public void printErr(String mes) {
		System.err.println("[CONNECTION HANDLER: " + socket.getPort() + "]: " + mes);
	}

	@Override
	public void run() {
		printMes("Waiting for the command");

		String firstLine = "";
		try {
			firstLine = reader.readLine();
		} catch (IOException e) {
			printErr("Failed to read input from the connection");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

		printMes("Command successfully read!");

		// TODO: check the arg number
		String[] command = firstLine.split(" ");
		typeOfConnection(command);

		closeConnection();
	}

	public void typeOfConnection(String[] command) {
		switch (command[0]) {
			case Protocol.JOIN_TOKEN:
				printMes("New dstore just joined the ranks!");
				printMes("Declared port: " + command[1]);
				Controller.addDstore(new DstoreHandler());
				printMes("Dstore successfully added to the list!");
				break;

			case Protocol.LIST_TOKEN:
			case Protocol.STORE_TOKEN:
			case Protocol.LOAD_TOKEN:
			case Protocol.LOAD_DATA_TOKEN:
			case Protocol.RELOAD_TOKEN:
			case Protocol.REMOVE_TOKEN:
				printMes("New client attempts to join the network!");
				if (!Controller.isEnoughDstores()) {
					printErr("Cannot connect! Not enough dstores joined the network");
				}
				// TODO: This is client, appropriate handler + log
				break;
			default:
				printErr("Invalid command");
				// TODO: This is an invalid command, should log it and ignore
		}

	}

	public void closeConnection() {
		try {
			printMes("Closing down the connection");
			socket.shutdownOutput();
			socket.shutdownInput();
			reader.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
