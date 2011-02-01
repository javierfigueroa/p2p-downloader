package p2p.peer.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import p2p.peer.Peer;

/**
 * @author E. Javier Figueroa 11/27/2010 CNT 5106c Fall 2010 
 * University of Florida
 * 
 */
public class Uploader implements Runnable {
	private File file;	
	private Peer peer;
	private String host;
	private boolean harvest;
	private SocketMessenger socketMessenger;

	public Uploader(File file, Peer peer, String from, boolean harvest) throws UnknownHostException, IOException {
		this.file = file;
		this.peer = peer;
		this.host = from;
		this.harvest = harvest;
		
		Socket socket = new Socket(peer.getHost(), peer.getPort());			
		peer.setSocket(socket);		
		
		this.socketMessenger = new SocketMessenger();
	}

	@Override
	public void run() {
		try {
			//connect to peer socket	
			String response = peer.getBufferedReader().readLine();
			if (!Common.connected(response, peer)) {
				this.peer.getSocket().close();
				throw new Exception("Failed to connect to peer " + this.peer.getHost() + ":" + this.peer.getPort());
			}

			//open passive socket in peer
			int port = openPassiveSocket(peer);			
			response = peer.getBufferedReader().readLine();
			if (!Common.openPort(response, peer)) {
				this.peer.getSocket().close();
				throw new Exception("Failed to open passive socket with peer " + this.peer.getHost() + ":" + this.peer.getPort());				
			}

			//let the peer know what chunks are being sent
			Common.sendCommand("upload " + this.file.getName() + " " + Common.combine(peer.getChunks(), ",") + " " + Boolean.toString(this.harvest), peer);
			response = peer.getBufferedReader().readLine();
			if (!Common.replied(response, peer, "150")) {
				this.peer.getSocket().close();
				throw new Exception("Failed to acknowledge chunks to peer " + this.peer.getHost() + ":" + this.peer.getPort());
			}

			//send the chunks
			this.socketMessenger.send(this.host, port, Long.parseLong(peer.getChunks()[0]), Common.CHUNK_SIZE * peer.getChunks().length, this.file);
			response = peer.getBufferedReader().readLine();
			if (!Common.replied(response, peer, "226")) {
				this.peer.getSocket().close();
				throw new Exception("Failed to send the chunks to peer " + this.peer.getHost() + ":" + this.peer.getPort());			
			}

			//finalize
			Common.sendCommand("quit", peer);
		}catch(Exception e) {
			//TODO: log exception
		}
		
	}

	/**
	 * Finds an open port for a passive socket connection
	 * 
	 * @param peer
	 * @return port number
	 * @throws IOException
	 */
	private int openPassiveSocket(Peer peer) throws IOException 
	{	
		ServerSocket dummy = new ServerSocket(0);
		int s_port = dummy.getLocalPort();
		dummy.close();		
		InetAddress localhost =  peer.getSocket().getLocalAddress();

		Common.sendPort(s_port, localhost, peer);

		return s_port;
	}
}
