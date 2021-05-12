import java.io.*;
import java.net.*;

public class myClient {
	private static InetAddress host;
	private static Socket socket;
	private static PrintWriter oos;
	private static BufferedReader ois;

	public static void main(String[] args)
			throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		// get the localhost IP address, if server is running on some other IP, you need
		// to use that
		host = InetAddress.getLocalHost();
		PrintWriter oos = null;
		BufferedReader ois = null;
		checkTimeout();

	}

	public static void checkTimeout()
			throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {

		socket = new Socket(host.getHostName(), 3000);
		oos = new PrintWriter(socket.getOutputStream());
		System.out.println("Sending request to Socket Server");

		oos.println(Protocol.STORE_TOKEN + " fileNames1" + " 13");
		oos.flush();
		// read the server response message
		ois = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String message = ois.readLine();
		System.out.println(message);

		String[] commands = message.split(" ");

		for (int j = 1; j < commands.length - 1; j++) {
			int dStorePort = Integer.parseInt(commands[j]);
			Socket dStoreSocket = new Socket(host.getHostName(), dStorePort);

			PrintWriter dStoreWriter = new PrintWriter(dStoreSocket.getOutputStream());
			BufferedReader dStoreReader = new BufferedReader(new InputStreamReader(dStoreSocket.getInputStream()));

			dStoreWriter.println(Protocol.STORE_TOKEN + " fileNames1" + " 13");
			dStoreWriter.flush();

			String newMessage = dStoreReader.readLine();
			System.out.println(newMessage);

			dStoreWriter.println("file_contents");
			dStoreWriter.flush();
			dStoreSocket.close();
		}

		String finalMessage = ois.readLine();
		System.out.println(finalMessage);

	}

	public static void func1() throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		for (int i = 0; i < 10; i++) {
			// establish socket connection to server
			// write to socket using ObjectOutputStream
			socket = new Socket(host.getHostName(), 3000);
			oos = new PrintWriter(socket.getOutputStream());
			System.out.println("Sending request to Socket Server");

			oos.println(Protocol.STORE_TOKEN + " fileNames" + i + " 13");
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

				dStoreWriter.println(Protocol.STORE_TOKEN + " fileNames" + i + " 13");
				dStoreWriter.flush();

				String newMessage = dStoreReader.readLine();
				System.out.println(newMessage);

				dStoreWriter.println("file_contents");
				dStoreWriter.flush();
				dStoreSocket.close();
			}

			String finalMessage = ois.readLine();
			System.out.println(finalMessage);

			System.out.println("Loading in file: fileNames" + i);
			oos.println(Protocol.LOAD_TOKEN + " fileNames" + i);
			oos.flush();
			message = ois.readLine();
			String[] commands1 = message.split(" ");

			int dstorePort = Integer.parseInt(commands1[1]);
			Socket dStoreSocket = new Socket(host.getHostName(), dstorePort);

			PrintWriter dStoreWriter = new PrintWriter(dStoreSocket.getOutputStream());
			InputStream dStoreReader = dStoreSocket.getInputStream();

			dStoreWriter.println(Protocol.LOAD_DATA_TOKEN + " fileNames" + i);
			dStoreWriter.flush();

			String newMessage = new String(dStoreReader.readNBytes(13));
			System.out.println("Successfull read: " + newMessage);
			dStoreSocket.close();

			System.out.println("Trying again....");
			oos.println(Protocol.RELOAD_TOKEN + " fileNames" + i);
			oos.flush();
			message = ois.readLine();
			commands1 = message.split(" ");

			dstorePort = Integer.parseInt(commands1[1]);
			dStoreSocket = new Socket(host.getHostName(), dstorePort);

			dStoreWriter = new PrintWriter(dStoreSocket.getOutputStream());
			dStoreReader = dStoreSocket.getInputStream();

			dStoreWriter.println(Protocol.LOAD_DATA_TOKEN + " fileNames" + i);
			dStoreWriter.flush();

			newMessage = new String(dStoreReader.readNBytes(13));
			System.out.println("Successfull read: " + newMessage);
			dStoreSocket.close();

			System.out.println("Trying again....");
			oos.println(Protocol.RELOAD_TOKEN + " fileNames" + i);
			oos.flush();
			System.out.println(ois.readLine());

			// close resources
			ois.close();
			oos.close();
			Thread.sleep(100);

		}
	}
}
