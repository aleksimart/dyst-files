import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Dstore {

	private static int port;
	private static int cport;
	private static int timeout;
	private static File file_folder;

	public static void main(String[] args) {
		init(args);

		try {
			DstoreLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL, port);
		} catch (IOException e) {
			System.err.println("[DSTORE]: Error:  issue with creating the log file");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			System.out
					.println("[DSTORE]: Establishing connection with Controller on port " + cport + ", local address");
			InetAddress address = InetAddress.getLocalHost();
			Socket connection = new Socket(address, cport, address, port);
			System.out.println("[DSTORE]: Succesfully established connection! Local port: " + port);

			PrintWriter pWriter = new PrintWriter(connection.getOutputStream(), true);
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("[DSTORE]: Ready to accept commands");
			for (;;) {
				System.out.print("> ");
				String mes = stdin.readLine();
				pWriter.println(mes);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void init(String[] args) {
		if (args.length != 4) {
			System.err.println("Invalid number of args, expected: 4, but got: " + args.length);
			System.exit(1);
		}

		parseArgs(args[0], "port");
		parseArgs(args[1], "controller port");
		parseArgs(args[2], "timeout period");
		parseArgs(args[3], "file folder");
	}

	private static void parseArgs(String arg, String name) {
		try {
			switch (name) {
				case "port":
					port = Integer.parseInt(arg);
					break;
				case "controller port":
					cport = Integer.parseInt(arg);
					break;
				case "timeout period":
					timeout = Integer.parseInt(arg);
					break;
				case "file folder":
					// TODO: possibly add a check that it the folder is possible to create
					file_folder = new File(arg);
					break;
				default:
					throw new Exception("Internal error, invalid arg: " + name);
			}

		} catch (NumberFormatException e) {
			System.err.println("Invalid " + name + ", must be an integer");
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalArgumentException e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

}
