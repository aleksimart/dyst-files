public class DstoreProps {
	private int size;
	private int port;

	public DstoreProps(int port) {
		this.port = port;
		this.size = 0;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void addFile(int filesize) {
		size += filesize;
	}
}
