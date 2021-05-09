import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * DstoreServer
 */
public class DstoreServer implements Runnable {
	private int port;
	private ServerSocket ss;

	private static final String NAME = DstoreServer.class.getName();

	public DstoreServer(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		try {
			ss = new ServerSocket(port);
			for (;;) {
				TerminalLog.printMes(NAME, "Ready to accept connections");
				Socket client = ss.accept();
				TerminalLog.printMes(NAME, "New connection from port " + client.getPort());
				TerminalLog.printMes(NAME, "Transfering control to handler to determine the type of the connection");
				new Thread(new ConnectionHandler(client, ConnectionHandler.ServerType.DSTORE)).start();
			}
		} catch (IOException e) {
			TerminalLog.printErr(NAME, "Error: " + e + ", closing the server");
		} finally {
			close();
		}
	}

	public void close() {
		try {
			ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isOpen() {
		return (ss != null && !ss.isClosed());
	}

}
