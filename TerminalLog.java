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

	public static String stampMes(String mes) {
		String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());
		return (timeStamp + ": " + mes);
	}
}
