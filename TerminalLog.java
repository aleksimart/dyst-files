import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * TerminalLog
 */
public class TerminalLog {
	public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

	public static void printMes(String entity, String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		System.out.println("[" + entity + ": " + timeStamp + "]: " + mes);
	}

	public static void printErr(String entity, String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		System.err.println("[" + entity + ": " + timeStamp + "]: " + mes);
	}

	// TODO: make sure to stamp the messages in their logger
	public static String stampMes(String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		return (timeStamp + ": " + mes);
	}

	public static void printHandlerMes(String handlerName, int port, String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		System.out.println("[" + handlerName + ": " + timeStamp + "]: [ " + port + " ] -> " + mes);
	}

	public static void printHandlerErrMes(String handlerName, int port, String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		System.err.println("[" + handlerName + ": " + timeStamp + "]: [ " + port + " ] -> " + mes);
	}

	public static void interactiveCommands(String name, PrintWriter out) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		TerminalLog.printMes(name, "Ready to accept commands");
		String mes;

		System.out.print("> ");
		while ((mes = reader.readLine()) != "quit") {
			out.println(mes);
			System.out.print("> ");

			if (reader.readLine() == Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN) {
				TerminalLog.printErr(name, "Not enough dstores joined yet, try again later!");
				break;
			}
		}

	}
}
