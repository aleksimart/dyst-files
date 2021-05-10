import java.net.Socket;
import java.util.ArrayList;

public class Index {

	public enum State {
		STORE_IN_PROGRESS, STORE_COMPLETE, REMOVE_IN_PROGRESS, REMOVE_COMPLETE
	}

	private int filesize;
	private State currentState;
	private Connection storer;
	private int dstoresNumber;
	private ArrayList<Connection> dstores;

	public Index(int filesize, Connection storer) {
		this.filesize = filesize;
		this.storer = storer;
		currentState = State.STORE_IN_PROGRESS;
		dstoresNumber = 0;
		dstores = new ArrayList<>();
	}

	public int getFilesize() {
		return filesize;
	}

	public State getCurrentState() {
		return currentState;
	}

	synchronized public boolean ack(Connection dstore) {
		dstoresNumber++;
		dstores.add(dstore);

		if (dstoresNumber == Controller.getR()) {
			currentState = State.STORE_COMPLETE;
			return true;
		}

		return false;
	}

	public int getDstoresNumber() {
		return dstoresNumber;
	}

	public Connection getStorer() {
		return storer;
	}

	synchronized public Connection getStore() {
		return dstores.get(0);
	}

}
