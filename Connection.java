import java.net.*;
import java.io.*;

public class Connection {
	public Socket socket;

	public BufferedReader in;
	public PrintWriter out;

	public Connection(Socket socket) throws IOException {
		this.socket = socket;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	public BufferedReader getIn() {
		return in;
	}

	public PrintWriter getOut() {
		return out;
	}

	public void close() throws IOException {
		in.close();
		out.close();
		socket.close();
	}

}
