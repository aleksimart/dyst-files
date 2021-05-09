import java.net.Socket;

public class Index {

	public enum State {
		STORE_IN_PROGRESS, STORE_COMPLETE, REMOVE_IN_PROGRESS, REMOVE_COMPLETE
	}

	private int filesize;
	private State currentState;
	private Connection storer;
	private int dstoresNumber;

	public Index(int filesize, Connection storer) {
		this.filesize = filesize;
		this.storer = storer;
		currentState = State.STORE_IN_PROGRESS;
		dstoresNumber = 0;
	}

	public int getFilesize() {
		return filesize;
	}

	public State getCurrentState() {
		return currentState;
	}

	synchronized public boolean ack() {
		dstoresNumber++;

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

}
