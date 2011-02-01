package p2p.peer.client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import p2p.peer.Logger;
import p2p.peer.Peer;
import p2p.peer.Program;
import p2p.peer.server.Common;
import p2p.peer.server.Uploader;


/**
 * @author E. Javier Figueroa 11/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class Client {
	private String host;
	private int port;
	private ArrayList<Peer> peers;
	private File file;

	public String getHost() {
		return this.host;
	}

	private void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	private void setPort(int port) {
		this.port = port;
	}
	
	public File getFile() {
		return this.file;
	}

	public Client(String host, int port) throws IOException {
		//work for the tracker if time allows
		setHost(host);
		setPort(port);

		this.peers = new ArrayList<Peer>();
		String fileWithPath = Common.getDirectoryPath(Program.PEERS_FILE);
		this.peers = Common.read(fileWithPath);
	}

	/**
	 * Registers a file with peers
	 * 
	 * @param filename
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void registerPeer(String filename) throws UnknownHostException, IOException {	
		//find file in system
		String fileWithPath = Common.getDirectoryPath(filename);
		File file = new File(fileWithPath);
		if (!file.exists() || !file.isFile()) {
			Logger.write(filename + " doesn\'t exist");
			return;
		}

		//for each peer
		for (Peer peer : this.peers) {
			String host = peer.getHost();
			int port = peer.getPort();

			//connect to peer
			Socket socket = new Socket(host, port);			
			peer.setSocket(socket);			
			String response = peer.getBufferedReader().readLine();
			if (!Common.connected(response, peer)) continue;

			//register file with peer
			Common.sendCommand("register-peer " + file.getName() + " " + file.length(), peer);
			response = peer.getBufferedReader().readLine();
			if (Common.replied(response, peer, "226")) {
				Logger.write("File was registered with peer " + peer.getHost() + ":" + peer.getPort());
			}

			socket.close();
		}

		this.file = file;
	}

	/**
	 * Uploads chunks to peers previously registered with the registerPeer method
	 * Sequentially sends an even amount of chunks to peers. Chunk size 100KB.
	 * Send remaining bits from oddly sized files to random peer in the list of peers
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void upload() throws UnknownHostException, IOException, InterruptedException {
		long fileSize = this.file.length();
		int filePerPeer = (int) (fileSize / this.peers.size());
		int size = (filePerPeer/Common.CHUNK_SIZE);

		long chunkIndex = 0;
		for(Peer peer : this.peers) {
			String[] chunks = new String[size];
			for ( int i=0; i<chunks.length; i++) {
				chunks[i] = Long.toString(chunkIndex);
				chunkIndex += Common.CHUNK_SIZE;
			}

			peer.setChunks(chunks);
		}

		Logger.write("Uploading...");
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for(Peer peer : this.peers) {
			Uploader uploader = new Uploader(file, peer, getHost(), true);
			Thread thread = new Thread(uploader);
			thread.start();
			
			threads.add(thread);
			Thread.sleep(3000);
		}

		long left;
		if ((left = (size * Common.CHUNK_SIZE) * this.peers.size()) < fileSize) {
			Random random = new Random();
			while ( left <= fileSize ) {
				int peer = random.nextInt(this.peers.size());
				this.peers.get(peer).setChunks(new String[] {Long.toString(left) });
				
				Uploader uploader = new Uploader(file, this.peers.get(peer), getHost(), false);
				Thread thread = new Thread(uploader);
				thread.start();
				
				threads.add(thread);
				left += Common.CHUNK_SIZE;
				Thread.sleep(3000);
			}
		}
		
		for (Thread thread : threads) {
			thread.join();
		}
		
		Logger.write("Upload complete.");
	}
}
