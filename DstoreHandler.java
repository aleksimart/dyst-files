import java.net.Socket;

public class DstoreHandler {
	private Socket socket;

	public DstoreHandler(Socket socket) {
		this.socket = socket;
	}

	public Socket getSocket() {
		return socket;
	}
}
