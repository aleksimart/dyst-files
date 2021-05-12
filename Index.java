import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Index {

	public enum State {
		STORE_IN_PROGRESS, STORE_COMPLETE, REMOVE_IN_PROGRESS, REMOVE_COMPLETE
	}

	public enum Timeout {
		IN_PROGRESS, SUCCESSFULL, TIMED_OUT
	}

	private int filesize;
	private State currentState;
	private Connection storer;
	private int dstoresNumber;
	private ArrayList<Connection> dstores;
	private Timeout timeout;

	private CompletableFuture<Timeout> storeAck;

	public Index(int filesize, Connection storer) {
		this.filesize = filesize;
		this.storer = storer;
		currentState = State.STORE_IN_PROGRESS;
		dstoresNumber = 0;
		dstores = new ArrayList<>();
		storeAck = new CompletableFuture<>();
		timeout = Timeout.IN_PROGRESS;
		storeAck.completeOnTimeout(Timeout.TIMED_OUT, Controller.getTimeout(), TimeUnit.MILLISECONDS);
	}

	public int getFilesize() {
		return filesize;
	}

	public State getCurrentState() {
		return currentState;
	}

	synchronized public Timeout ack(Connection dstore) {
		if (timeout != Timeout.TIMED_OUT) {
			dstoresNumber++;
			dstores.add(dstore);

			if (dstoresNumber == Controller.getR()) {
				storeAck.complete(Timeout.SUCCESSFULL);
				currentState = State.STORE_COMPLETE;
				timeout = Timeout.SUCCESSFULL;
			}
		}

		return timeout;
	}

	public int getDstoresNumber() {
		return dstoresNumber;
	}

	public Connection getStorer() {
		return storer;
	}

	// returns true if the index is no longer stored anywhere
	synchronized public boolean removeDstore(Connection connection) {
		dstores.remove(connection);
		return dstores.size() == 0;
	}

	synchronized public Connection getStore() {
		return dstores.get(0);
	}

	public ArrayList<Connection> getDstores() {
		return new ArrayList<Connection>(dstores);
	}

	public CompletableFuture<Timeout> getStoreAck() {
		return storeAck;
	}

}
