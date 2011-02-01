package p2p.peer.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import p2p.peer.Logger;
import p2p.peer.P2PFile;
import p2p.peer.Peer;
import p2p.peer.Program;

/**
 * @author E. Javier Figueroa 11/27/2010 CNT 5106c Fall 2010 
 * University of Florida
 * 
 */
public class Connection implements Runnable {

	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;
	private SocketMessenger socketMessenger;

	public Socket getSocket() {
		return this.socket;
	}

	public Connection(Socket socket) throws IOException {
		this.socket = socket;
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
		this.socketMessenger = new SocketMessenger(this);
	}

	@Override
	public void run() {
		try {
			listen();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				this.socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.socket.close();
	}

	/**
	 * Listens for commands coming in through the connection socket
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void listen() throws IOException, InterruptedException {
		reply(200);

		String line = null;
		while ((line = reader.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line);
			String command = st.nextToken().toLowerCase();

			int code = 0;
			if (command.equals("register-peer")) {
				code = register(st);
			} else if (command.equals("upload")) {
				code = upload(st);
			} else if (command.equals("query")) {
				code = query(st);
			} else if (command.equals("download")) {
				code = download(st);
			} else if (command.equals("port")) {
				code = setPort(st);
			} else if (command.equals("quit")) {
				code = quit();
			}

			if (code == 221 || code == 0) return;
		}
	}

	/**
	 * Queries filename for available chunks
	 * 
	 * @param st command
	 * @return 226 and available chunks in a CSV string, 500 if no chunks found
	 */
	private int query(StringTokenizer st) {
		String filename = st.nextToken();

		if (Program.p2pFiles.containsKey(filename)) {
			P2PFile file = Program.p2pFiles.get(filename);
			String[] chunkIndexes = st.nextToken().split(",");

			if (chunkIndexes.length > 0) {
				ArrayList<String> chunksFound = new ArrayList<String>();
				for (String index : chunkIndexes) {
					if (index != null) { 
						try {
							Long.parseLong(index);
						}catch(Exception e) {
							return reply(500);
						}						
						
						if (file.getChunks().containsKey(Long.parseLong(index)) && file.getChunks().get(Long.parseLong(index)).length > 0) {
							chunksFound.add(index);
						}
					}
				}

				if (chunksFound.size() <= 0) {
					return reply(500);
				}

				String[] chunks = (String[]) chunksFound.toArray(new String[chunksFound.size()]);

				return reply(226, Common.combine(chunks, ","));
			}
		}

		return reply(500);
	}

	/**
	 * Registers a filename in memory, with corresponding chunk map
	 * 
	 * @param st command
	 * @return 226 to acknowledge the file registration
	 */
	private int register(StringTokenizer st) {
		String filename = st.nextToken();
		long fileSize = Long.parseLong(st.nextToken());
		P2PFile p2pFile = new P2PFile(filename, fileSize);

		Integer[] chunks = new Integer[(int) (fileSize / Common.CHUNK_SIZE) + 1];
		for (int i = 0, chunkIdx = 0; i < chunks.length; i++) {
			p2pFile.getChunks().put((long) chunkIdx, new byte[0]);
			chunkIdx += Common.CHUNK_SIZE;
		}

		Logger.write("Registering file " + filename + " (" + fileSize + " bytes)");
		Program.p2pFiles.put(filename, p2pFile);

		return reply(226);
	}

	/**
	 * Sends chunks to peers
	 * 
	 * @param st command
	 * @return 226 for successfull transmission, 500 otherwise
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private int download(StringTokenizer st) throws UnknownHostException, IOException {
		String filename = st.nextToken();
		if (Program.p2pFiles.containsKey(filename)) {
			String chunks = st.nextToken();
			String[] chunkIndexes = chunks.split(",");
			P2PFile file = Program.p2pFiles.get(filename);
			Logger.write("Sending " + chunkIndexes.length + " chunks => " + chunks);

			return this.socketMessenger.send(file, chunkIndexes);
		}

		return reply(500);
	}

	/**
	 * Harvest chunks from peers
	 * 
	 * @param st command
	 * @return 226 for successfull storage of file, 500 otherwise
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int upload(StringTokenizer st) throws IOException, InterruptedException {
		String filename = st.nextToken();
		int code = 500;
		if (Program.p2pFiles.containsKey(filename)) {
			String chunks = st.nextToken();
			String[] chunkIndexes = chunks.split(",");
			Logger.write("About to receive " + chunkIndexes.length + " chunks => " + chunks);
			this.socketMessenger.collect(filename, chunkIndexes);

			boolean harvest = Boolean.parseBoolean(st.nextToken());			 
			if (harvest) {
				code = search(filename);
			}else{
				code = 226;
			}
		}

		return reply(code);
	}

	/**
	 * Searches for chunks in peers
	 * 
	 * @param filename
	 * @return 226 for successfull storage of file, 500 otherwise
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int search(String filename) throws UnknownHostException, IOException, InterruptedException {
		ArrayList<Peer> peers = Common.read(Program.PEERS_FILE);
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for (Peer peer : peers) {
			Downloader downloader = new Downloader(filename, peer);
			Thread thread = new Thread(downloader);
			thread.start();
			threads.add(thread);
		}

		for (Thread thread : threads) {
			thread.join();
		}
		
		return this.socketMessenger.store(filename);
	}

	/**
	 * Sets the port to be used in an opened connection
	 * 
	 * @param st command
	 * @return 200 to acknowledge the setting of the port
	 */
	private int setPort(StringTokenizer st) {
		String portStr = st.nextToken();
		st = new StringTokenizer(portStr, ",");
		String h1 = st.nextToken();
		String h2 = st.nextToken();
		String h3 = st.nextToken();
		String h4 = st.nextToken();
		int p1 = Integer.parseInt(st.nextToken());
		int p2 = Integer.parseInt(st.nextToken());

		String dataHost = h1 + "." + h2 + "." + h3 + "." + h4;
		int dataPort = (p1 << 8) | p2;

		this.socketMessenger.setDataPort(dataHost, dataPort);

		return reply(200);
	}

	private int quit() throws IOException {
		this.socket.close();
		return reply(221);
	}

	public int reply(int code) {
		this.writer.println(code);
		return code;
	}

	public int reply(int code, String message) {
		this.writer.println(code + " " + message);
		return code;
	}

}
