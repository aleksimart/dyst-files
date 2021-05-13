public interface Handler {
	public void handle(String args[], Connection connection);

	static void sendConrollerMes(Connection connection, String mes) {
		ControllerLogger.getInstance().messageSent(connection.getSocket(), TerminalLog.stampMes(mes));
		connection.getOutWriter().println(mes);
	}
}
