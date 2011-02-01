package p2p.peer;

import java.io.Console;
import java.io.IOException;
import java.util.HashMap;

import p2p.peer.client.Client;
import p2p.peer.server.Server;


/**
 * @author E. Javier Figueroa 11/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class Program {

	public static String PEERS_FILE;
	public static String COMMANDS = "Supported commands:\n" +
	"register-peer <filename> \t(register a file with the tracker)\n" +
	"upload \t\t\t\t(upload a registered file to peers)\n" +
	"quit \t\t\t\t(exit)\n\n";
	public static HashMap<String, P2PFile> p2pFiles = new HashMap<String, P2PFile>();

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {

		if (args.length > 3 || args.length != 3) {
			Logger.write("Usage: peer.jar [ tracker server ] [ port ] [peers text file]");
			System.exit(1);
		}

		try {
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			int serverPort = port + 1;
			String filename = args[2];
			Console console = System.console();		
			
			Logger.write("Starting peer...");
			Logger.write("Peer client running on " + host +":"+port);
			Logger.write("Peer server running on " + host +":"+serverPort);
			
			PEERS_FILE = filename;
			Client client = new Client(host, port);			
			Server server = new Server(serverPort);			
			new Thread(server).start();

			String message = "Welcome!\n" + COMMANDS;
			Logger.write(message);
			String command = "";
			
			while(!(command = console.readLine("> ")).equalsIgnoreCase("quit")) {
				if (command.startsWith("register-peer") && command.split(" ").length > 0) {
					client.registerPeer(command.split(" ")[1]);
					continue;
				} else if (command.startsWith("upload"))	{
					if (client.getFile() == null) {
						Logger.write("You have no file to upload, use the register-peer command first");
						continue;
					}
					
					client.upload();
					continue;
				} 
				
				message = "Invalid command -> " + COMMANDS;
				Logger.write(message);
			}

		}catch(Exception e){
			Logger.write(e.getMessage());
			Logger.write("***Check your peer's file and verify they are online***");
		}

		Logger.write("Bye.");
		System.exit(0);
	}
}
