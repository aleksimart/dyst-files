import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * DstoreConnection
 */
public class DstoreServerHandler {
	// private String command;
	// private String[] args;
	// private Connection connection;

	// public DstoreServerHandler(Connection connection, String[] args) {
	// command = args[0];
	// this.args = new String[args.length - 1];
	// this.connection = connection;

	// for (int i = 1; i < args.length; i++) {
	// this.args[i - 1] = args[i];
	// }
	// }

	// public void handle() {
	// switch (command) {
	// case Protocol.STORE_TOKEN:
	// store();
	// break;
	// default:
	// System.err.println("Err, not implemented yet, or wrong protocol: " +
	// command);
	// }
	// }

	// TODO: possibly don't actually do a while loop when dealing with the dstore
	// client connection but just one command?
	public static Handler storeHandler = (String args[], Connection connection) -> {
		File file = new File(Dstore.getFile_folder().getAbsolutePath() + "/" + args[0]);
		TerminalLog.printMes("StoreHandler from Dstore",
				connection.getPort() + " - Created file '" + args[0] + "', sending acknowledgement");
		DstoreLogger.getInstance().messageSent(connection.getSocket(), Protocol.ACK_TOKEN);
		connection.getOutWriter().println(Protocol.ACK_TOKEN);

		try { // TODO: pretty sure we don't need to log anything related to a file
			FileOutputStream fileStream = new FileOutputStream(file);
			byte[] contents = connection.getIn().readNBytes(Integer.parseInt(args[1]));
			fileStream.write(contents);
			fileStream.close();
			TerminalLog.printMes("StoreHandler from Dstore",
					connection.getPort() + " - Stored file '" + args[0] + "', sending acknowledgement to Controller");
			Dstore.ackStorage(args[0]);

			connection.getInReader().readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	// TODO: Need to close the connection if the file is not here
	public static Handler loadHandler = (String args[], Connection connection) -> {
		// Check args number
		if (args.length != 1) {
			TerminalLog.printHandlerErrMes("LoadHandler from Dstore", connection.getPort(),
					"Invalid number of args, expected: 1, but got: " + args.length);
			return;
		}

		String filename = args[0];

		try {
			FileInputStream inFile = new FileInputStream(new File(Dstore.getFile_folder(), filename));
			TerminalLog.printHandlerMes("LoadHandler from Dstore", connection.getPort(),
					"Found file '" + filename + "', starting the transfer");

			byte[] a = inFile.readAllBytes();
			connection.getOut().write(a);
			connection.getOut().flush();
			inFile.close();

			TerminalLog.printHandlerMes("LoadHandler from Dstore", connection.getPort(),
					"File '" + filename + "', has been transferred successfully");
		} catch (IOException e) {
			TerminalLog.printHandlerErrMes("LoadHandler from Dstore", connection.getPort(),
					"Error when trying to transfer file '" + filename + "', check trace for more information");
			e.printStackTrace();
		}
	};

	public static Handler removeHandler = (String args[], Connection connection) -> {
		TerminalLog.printMes("RemoveHandler from Dstore",
				connection.getPort() + " - Found file '" + args[0] + "', starting the deletion process");
		File fileToDelete = new File(Dstore.getFile_folder(), args[0]);
		fileToDelete.delete();
		TerminalLog.printMes("RemoveHandler from Dstore",
				connection.getPort() + " - File '" + args[0] + "', has been deleted successfully");
		connection.getOutWriter().println(Protocol.REMOVE_ACK_TOKEN + " " + args[0]);
	};
}
