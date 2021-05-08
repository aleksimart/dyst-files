public class Index {

	public enum State {
		STORE_IN_PROGRESS, STORE_COMPLETE, REMOVE_IN_PROGRESS, REMOVE_COMPLETE
	}

	private int filesize;
	private State currentState;
	private int dstoresNumber;

	public Index(int filesize) {
		this.filesize = filesize;
		currentState = State.STORE_IN_PROGRESS;
		dstoresNumber = 0;
	}

	public int getFilesize() {
		return filesize;
	}

	public State getCurrentState() {
		return currentState;
	}

	public boolean ack() {
		dstoresNumber++;
		return dstoresNumber++ == Controller.getR();
	}

	public int getDstoresNumber() {
		return dstoresNumber;
	}

}
