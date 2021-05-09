import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * DstoreConnection
 */
public class DstoreServerHandler implements Handler {
	private String command;
	private String[] args;
	private Connection connection;

	public DstoreServerHandler(Connection connection, String[] args) {
		command = args[0];
		this.args = new String[args.length - 1];
		this.connection = connection;

		for (int i = 1; i < args.length; i++) {
			this.args[i - 1] = args[i];
		}
	}

	public void handle() {
		switch (command) {
			case Protocol.STORE_TOKEN:
				store();
				break;
			default:
				System.err.println("Err, not implemented yet, or wrong protocol: " + command);
		}
	}

	public void store() {
		// TODO: Possibily fix this
		File file = new File(Dstore.getFile_folder().getAbsolutePath() + "/" + args[0]);
		connection.getOutWriter().println(Protocol.ACK_TOKEN);

		try {
			FileOutputStream fileStream = new FileOutputStream(file);
			byte[] contents = connection.getIn().readNBytes(Integer.parseInt(args[1]));
			fileStream.write(contents);
			fileStream.close();
			Dstore.ackStorage(args[0]);
			connection.getInReader().readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
