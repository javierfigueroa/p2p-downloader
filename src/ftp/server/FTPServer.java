package ftp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author E. Javier Figueroa 7/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class FTPServer {

	/**
	 * The port this server connects to.
	 */
	public static final int SERVER_PORT = 21;
	/**
	 * The port this server is listening on.
	 */
	private int port;

	public static void main(String [] args) throws IOException {
		
		if (args.length > 1 || args.length < 1)
		{
			System.err.println("Usage: ftpserver.jar [ port ]");
			System.exit(1);
		}

		int port = SERVER_PORT;
		if (args.length == 1)
			port = Integer.parseInt(args[0]);
		
		FTPServer server = new FTPServer(port);
		server.start();
	}
	
	public FTPServer(int port) {
		this.port = port;
	}

	public void start() throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		while(true) {
			Socket socket = serverSocket.accept();
			Connection connection = new Connection(socket);
			new Thread(connection).start();
		}
	}
}
