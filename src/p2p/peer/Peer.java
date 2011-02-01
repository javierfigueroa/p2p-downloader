package p2p.peer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Peer {
	private int port;
	private String hostName;
	private P2PFile p2PFile;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private String[] chunks;
	
	public void setSocket(Socket socket) throws IOException {
		this.socket = socket;
		this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));	
		this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));	
	}
	
	public void setChunks(String[] chunks) {
		this.chunks = chunks;
	}
	
	public void setFile(P2PFile p2PFile) {
		this.p2PFile = p2PFile;
	}
	
	public String[] getChunks() {
		return this.chunks;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getHost() {
		return this.hostName;
	}
	
	public P2PFile getFile() {
		return this.p2PFile;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public BufferedReader getBufferedReader() {
		return this.reader;	
	}
	
	public BufferedWriter getBufferedWriter() {
		return this.writer;
	}
	
	public Peer(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}
}
