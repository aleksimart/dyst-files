import java.net.*;
import java.io.*;

public class Connection {
	public Socket socket;

	private BufferedReader inReader;
	private PrintWriter outWriter;
	private InputStream in;
	private OutputStream out;

	public Connection(Socket socket) throws IOException {
		this.socket = socket;

		in = socket.getInputStream();
		out = socket.getOutputStream();

		inReader = new BufferedReader(new InputStreamReader(in));
		outWriter = new PrintWriter(out, true);
	}

	public BufferedReader getInReader() {
		return inReader;
	}

	public PrintWriter getOutWriter() {
		return outWriter;
	}

	public int getPort() {
		return socket.getPort();
	}

	public void close() throws IOException {
		inReader.close();
		outWriter.close();
		socket.close();
	}

	public InputStream getIn() {
		return in;
	}

	public OutputStream getOut() {
		return out;
	}

	public Socket getSocket() {
		return socket;
	}

}
