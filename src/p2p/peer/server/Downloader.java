package p2p.peer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import p2p.peer.P2PFile;
import p2p.peer.Peer;
import p2p.peer.Program;

/**
 * @author E. Javier Figueroa 11/27/2010 CNT 5106c Fall 2010 
 * University of Florida
 * 
 */
public class Downloader implements Runnable {
	private String filename;
	private Peer peer;
	private SocketMessenger socketMessenger;

	public Downloader(String filename, Peer peer) throws UnknownHostException, IOException {
		this.filename = filename;
		this.peer = peer;

		Socket socket = new Socket(peer.getHost(), peer.getPort());
		peer.setSocket(socket);

		this.socketMessenger = new SocketMessenger();
	}

	@Override
	public void run() {
		try {
			// connect to peer socket
			String response = peer.getBufferedReader().readLine();
			if (!Common.connected(response, peer)) {
				this.peer.getSocket().close();
				throw new Exception("Failed to connect to peer "
						+ this.peer.getHost() + ":" + this.peer.getPort()
						+ " retrying...");
			}

			// loop to find all the chunks
			 while (!Program.p2pFiles.get(filename).complete()) {
				P2PFile file = Program.p2pFiles.get(filename);

				// compose list of missing chunks
				ArrayList<String> missingChunks = new ArrayList<String>();
				for (Long index : file.getChunks().keySet()) {
					if (file.getChunks().get(index).length == 0) {
						missingChunks.add(Long.toString(index));
					}
				}
				String chunks[] = (String[]) missingChunks.toArray(new String[missingChunks.size()]);
				Thread.sleep(2000);
				
				// let the peer know what chunks are being sent
				Common.sendCommand("query " + filename + " " + Common.combine(chunks, ","), peer);
				response = peer.getBufferedReader().readLine();				
				if (Common.replied(response, peer, "226")) {
					getMissingChunks(response);
				}

				Thread.sleep(4000);
			}

			// finalize
			Common.sendCommand("quit", peer);
		} catch (Exception e) {
			// TODO: log exception
		}
	}

	/**
	 * Downloads the chunks from peer
	 * 
	 * @param response Contains the chunks to be downloaded in CSV string
	 * @throws IOException
	 * @throws Exception
	 */
	private void getMissingChunks(String response) throws IOException, Exception {
		// get found chunks
		StringTokenizer st = new StringTokenizer(response);
		st.nextToken();
		String[] chunks = st.nextToken().split(",");

		// open active connection to receive chunks
		ServerSocket socket = openActiveSocket();
		response = peer.getBufferedReader().readLine();
		if (!Common.openPort(response, peer)) {
			this.peer.getSocket().close();
			throw new Exception("Failed to open active socket with peer "
					+ this.peer.getHost() + ":" + this.peer.getPort()
					+ " retrying...");
		}

		// request chunks
		Common.sendCommand("download " + this.filename + " " + Common.combine(chunks, ","), peer);

		// get the chunks
		this.socketMessenger.collect(filename, chunks, socket);
		response = peer.getBufferedReader().readLine();
		if (!Common.replied(response, peer, "226")) {
			this.peer.getSocket().close();
			throw new Exception("Failed to get the missing chunks from peer "
					+ this.peer.getHost() + ":" + this.peer.getPort()
					+ " retrying...");
		}
	}

	/**
	 * Opens an active socket connection
	 * 
	 * @return server socket
	 * @throws IOException
	 */
	private ServerSocket openActiveSocket() throws IOException {
		ServerSocket socket = new ServerSocket(0);
		InetAddress localhost = this.peer.getSocket().getLocalAddress();
		int s_port = socket.getLocalPort();

		Common.sendPort(s_port, localhost, this.peer);

		return socket;
	}
}
