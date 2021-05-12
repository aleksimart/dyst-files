import java.io.IOException;
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

		startTimer();

	}

	public int getFilesize() {
		return filesize;
	}

	public State getCurrentState() {
		return currentState;
	}

	// TODO: why do I have to say the name of the file to the index lol, fix that
	// pls
	// synchronized public void startRemoval(String filename) {
	// currentState = State.REMOVE_IN_PROGRESS;
	// ArrayList<CompletableFuture<Timeout>> requests = new ArrayList<>();

	// for (Connection dstore : dstores) {
	// TerminalLog.printMes("Index File '" + filename + "'",
	// "Attempting to delete the file for dstore: " + dstore.getPort());
	// CompletableFuture<Timeout> future = CompletableFuture.supplyAsync(() -> {
	// // TODO: fix that
	// ControllerLogger.getInstance().messageSent(dstore.getSocket(),
	// Protocol.REMOVE_TOKEN);
	// dstore.getOutWriter().println(Protocol.REMOVE_TOKEN + " " + filename);
	// try {
	// dstore.getInReader().readLine();
	// TerminalLog.printMes("Index File '" + filename + "'",
	// "has been deleted successfully for dstore: " + dstore.getPort());
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// TerminalLog.printMes("Index File '" + filename + "'",
	// "Successfully removed the file in dstore: " + dstore.getPort());
	// return Timeout.SUCCESSFULL;
	// });

	// requests.add(future);
	// }
	// storeAck = CompletableFuture.allOf(requests.toArray(new
	// CompletableFuture[requests.size()]))
	// .thenApply(ignore -> Timeout.SUCCESSFULL);
	// startTimer();
	// }

	public void startTimer() {
		storeAck = new CompletableFuture<>();
		timeout = Timeout.IN_PROGRESS;
		storeAck.completeOnTimeout(Timeout.TIMED_OUT, Controller.getTimeout(), TimeUnit.MILLISECONDS);
	}

	synchronized public Timeout ackRemove(Connection dstore) {
		if (timeout != Timeout.TIMED_OUT) {
			dstoresNumber--;
			dstores.remove(dstore);

			if (dstoresNumber == 0) {
				storeAck.complete(Timeout.SUCCESSFULL);
				currentState = State.REMOVE_COMPLETE;
				timeout = Timeout.SUCCESSFULL;
			}
		}

		return timeout;
	}

	synchronized public Timeout ackStore(Connection dstore) {
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

	// TODO: not sure that I need to synch here
	synchronized public Connection getStore() {
		return dstores.get(0);
	}

	public ArrayList<Connection> getDstores() {
		// Making a copy here for safety
		return new ArrayList<Connection>(dstores);
	}

	public CompletableFuture<Timeout> getStoreAck() {
		return storeAck;
	}

}
