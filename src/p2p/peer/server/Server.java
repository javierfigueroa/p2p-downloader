package p2p.peer.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * @author E. Javier Figueroa 
 * COP 5615 Spring 2011
 * University of Florida
 *
 */
public class Server implements Runnable{
	/**
	 * The port this server is listening on.
	 */
	private int port;

	public Server(int port) {
		this.port = port;
	}

	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while(true) {
				Socket socket = serverSocket.accept();
				Connection connection = new Connection(socket);
				new Thread(connection).start();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
