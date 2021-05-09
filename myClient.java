import java.io.*;
import java.net.*;

public class myClient {
	public static void main(String[] args)
			throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		// get the localhost IP address, if server is running on some other IP, you need
		// to use that
		InetAddress host = InetAddress.getLocalHost();
		Socket socket = null;
		PrintWriter oos = null;
		BufferedReader ois = null;

		for (int i = 0; i < 1; i++) {
			// establish socket connection to server
			socket = new Socket(host.getHostName(), 3000);
			// write to socket using ObjectOutputStream
			oos = new PrintWriter(socket.getOutputStream());
			System.out.println("Sending request to Socket Server");

			oos.println(Protocol.STORE_TOKEN + " fileNames 13");
			oos.flush();
			// read the server response message
			ois = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String message = ois.readLine();
			System.out.println(message);

			String[] commands = message.split(" ");

			for (int j = 1; j < commands.length; j++) {
				int dStorePort = Integer.parseInt(commands[j]);
				Socket dStoreSocket = new Socket(host.getHostName(), dStorePort);

				PrintWriter dStoreWriter = new PrintWriter(dStoreSocket.getOutputStream());
				BufferedReader dStoreReader = new BufferedReader(new InputStreamReader(dStoreSocket.getInputStream()));

				dStoreWriter.println(Protocol.STORE_TOKEN + " fileNames 13");
				dStoreWriter.flush();

				String newMessage = dStoreReader.readLine();
				System.out.println(newMessage);

				dStoreWriter.println("file_contents");
				dStoreWriter.flush();

			}
			String finalMessage = ois.readLine();
			System.out.println(finalMessage);

			// close resources
			ois.close();
			oos.close();
			Thread.sleep(100);
		}
	}
}
