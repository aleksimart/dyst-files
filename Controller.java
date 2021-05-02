import java.io.*;
import java.net.*;

public class FTServer {
    public static void main(String[] args) {
        try {
            // Create a server socket with port 4323
            ServerSocket ss = new ServerSocket(4323);
            for (;;) {
                try {
                    // Hangs here until the connection is accepted
                    System.out.println("waiting for connection");
                    Socket client = ss.accept();
                    System.out.println("connected to: " + client.getInetAddress());
                    FileThread thread = new FileThread(client);
                    new Thread(thread).start();

                } catch (Exception e) {
                    System.out.println("error " + e);
                }
            }
        } catch (Exception e) {
            System.out.println("error " + e);
        }
    }

    static class FileThread implements Runnable {

        Socket client;

        public FileThread(Socket client) {
            this.client = client;
        }

        public void run() {
            try {
                // Open Input Stream for receiving a file
                InputStream in = client.getInputStream();
                // 1000 bytes buffer
                byte[] buf = new byte[1000];
                // Length of the buffer or how much of the buffer is actually used
                int buflen;

                // Reads in the bytes into the buffer and returns the number of bytes read in
                buflen = in.read(buf);

                // Read in the butter and convert it to str
                String firstBuffer = new String(buf, 0, buflen);
                System.out.println("First buffer: " + firstBuffer);

                // Pull out the command
                int firstSpace = firstBuffer.indexOf(" ");
                String command = firstBuffer.substring(0, firstSpace);
                System.out.println("command " + command);

                /*
                 * There are two types of commands: 'put' and 'get' Put puts a new file on the
                 * server Get gets the existing file from the server
                 */
                if (command.equals("put")) {
                    // pull out the name of the file
                    int secondSpace = firstBuffer.indexOf(" ", firstSpace + 1);
                    String fileName = firstBuffer.substring(firstSpace + 1, secondSpace);
                    System.out.println("fileName " + fileName);

                    // Create a file with a given name
                    File outputFile = new File(fileName);
                    // Create the output stream for the files
                    FileOutputStream out = new FileOutputStream(outputFile);

                    /*
                     * Write the contents into the file starting from the 1st character after the
                     * second space (buflen secondspace - 1 is the number of bytes to write)
                     */
                    out.write(buf, secondSpace + 1, buflen - secondSpace - 1);

                    /*
                     * If there are still more bytes to read in (the file + commands was more than
                     * the length of the current buffer which is 1000)
                     */
                    while ((buflen = in.read(buf)) != -1) {
                        System.out.print("*");

                        out.write(buf, 0, buflen);
                    }

                    // Closing up everything
                    in.close();
                    client.close();
                    out.close();
                } else if (command.equals("get")) {
                    // pull out the name of the file
                    int secondSpace = firstBuffer.indexOf(" ", firstSpace + 1);
                    String fileName = firstBuffer.substring(firstSpace + 1, secondSpace);
                    System.out.println("fileName " + fileName);

                    // Find a file with a given name
                    File inputFile = new File(fileName);
                    // Create the input stream for the found file
                    FileInputStream inf = new FileInputStream(inputFile);
                    // Prepare the output stream for the client to send the file to
                    OutputStream out = client.getOutputStream();

                    // Keep reading the into the input stream until all of the file is read
                    while ((buflen = inf.read(buf)) != -1) {
                        System.out.print("*");
                        out.write(buf, 0, buflen);
                    }

                    // Close everything up
                    in.close();
                    inf.close();
                    client.close();
                    out.close();
                } else {
                    System.out.println("unrecognised command");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
