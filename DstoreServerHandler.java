import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * DstoreConnection
 */
public class DstoreServerHandler {

	public static Handler storeHandler = (String args[], Connection connection) -> {
		// Check args number
		if (args.length != 2) {
			TerminalLog.printHandlerErrMes("StoreHandler from Dstore", connection.getPort(),
					"Invalid number of args, expected: 2, but got: " + args.length);
			return;
		}

		String filename = args[0];

		// Check second arg is int
		int filesize;
		try {
			filesize = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			TerminalLog.printHandlerErrMes("StoreHandler from Dstore", connection.getPort(),
					"Error, second argument is expected to be an integer");
			return;
		}

		File file = new File(Dstore.getFile_folder(), filename);
		TerminalLog.printHandlerMes("StoreHandler from Dstore", connection.getPort(),
				"Created file '" + filename + "', sending acknowledgement");
		Handler.sendDstoreMes(connection, Protocol.ACK_TOKEN);

		try { // TODO: pretty sure we don't need to log anything related to a file
			FileOutputStream fileStream = new FileOutputStream(file);
			byte[] contents = connection.getIn().readNBytes(filesize);

			fileStream.write(contents);
			fileStream.close();

			TerminalLog.printHandlerMes("StoreHandler from Dstore", connection.getPort(),
					"Stored file '" + filename + "', sending acknowledgement to Controller");
			Dstore.ackStorage(filename);

			// Clean up the line
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
		// Check args number
		if (args.length != 1) {
			TerminalLog.printHandlerErrMes("RemoveHandler from Dstore", connection.getPort(),
					"Invalid number of args, expected: 1, but got: " + args.length);
			return;
		}

		String filename = args[0];

		TerminalLog.printHandlerMes("RemoveHandler from Dstore", connection.getPort(),
				"Found file '" + filename + "', starting the deletion process");
		File fileToDelete = new File(Dstore.getFile_folder(), filename);
		fileToDelete.delete();
		TerminalLog.printHandlerMes("RemoveHandler from Dstore", connection.getPort(),
				"File '" + filename + "', has been deleted successfully");
		Handler.sendDstoreMes(connection, Protocol.REMOVE_ACK_TOKEN + " " + args[0]);
	};
}
