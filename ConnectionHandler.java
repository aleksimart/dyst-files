import java.io.*;
import java.net.*;

public class ConnectionHandler implements Runnable {
	private Socket socket;
	private BufferedReader reader;

	public ConnectionHandler(Socket socket) {
		this.socket = socket;
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			System.err.println(
					"[CONNECTION HANDLER " + socket.getPort() + "]: Failed to get the input steram of the connection");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void run() {
		System.out.println("[CONNECTION HANDLER: " + socket.getPort() + "]: Waiting for the command");

		String firstLine = "";

		try {
			firstLine = reader.readLine();
		} catch (IOException e) {
			System.err.println(
					"[CONNECTION HANDLER: " + socket.getPort() + "]: Failed to read input from the connection");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

		System.out.println("[CONNECTION HANDLER: " + socket.getPort() + "]: Command succsessfully read!");
		int firstSpace = firstLine.indexOf(" ");

		// TODO: big assumption here that the space exists
		switch (firstLine.substring(0, firstSpace)) {
			case Protocol.JOIN_TOKEN:
				System.out.println("[CONNECTION HANDLER: " + socket.getPort()
						+ "]: new dstore just joined the ranks!\n[SERVER]: Declared port: "
						+ firstLine.substring(firstSpace + 1));
				// TODO: This is a dstore, appropriate handler + log
				break;
			case Protocol.LIST_TOKEN:
			case Protocol.STORE_TOKEN:
			case Protocol.LOAD_TOKEN:
			case Protocol.LOAD_DATA_TOKEN:
			case Protocol.RELOAD_TOKEN:
			case Protocol.REMOVE_TOKEN:
				System.out.println("This is a client");
				// TODO: This is client, appropriate handler + log
				break;
			default:
				System.out.println("Invalid command");
				// TODO: This is an invalid command, should log it and ignore
		}
	}

}
