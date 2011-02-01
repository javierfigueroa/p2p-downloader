package p2p.peer.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import p2p.peer.Logger;
import p2p.peer.Peer;

public class Common {
	public static final int BUFFER_SIZE = 1024;
	public static final int CHUNK_SIZE = 102400;

	public static ArrayList<Peer> read(String fileName) throws IOException {
		Logger.write("Reading peers from file...");
		ArrayList<Peer> peers = new ArrayList<Peer>();
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(new FileInputStream(fileName));
		try {
			while (scanner.hasNextLine()){
				String line = scanner.nextLine();
				StringTokenizer st = new StringTokenizer(line);

				String host = st.nextToken().toLowerCase();
				int port = Integer.parseInt(st.nextToken().toLowerCase());

				Peer peer = new Peer(host, port);
				peers.add(peer);
				text.append(line + NL);
			}
		}
		finally{
			scanner.close();
		}
		Logger.write("Peers found in file: " + fileName + NL + text.toString());
		
		return peers;
	}

	public static String getDirectoryPath(String filename) {
		String path = null;
		if (filename.charAt(0) == '/')
			path = System.getProperty("user.dir") + filename;
		else
			path = System.getProperty("user.dir") + "/" + filename;

		return path;
	}
	
	public static void sendPort(int s_port, InetAddress localhost, Peer peer) throws IOException 
	{
		//Build command
		byte[] ip = localhost.getAddress();
		byte[] port = new byte[2];

		//Build port for command
		port[0] =(byte) (s_port >> 8); 
		port[1] =(byte) (s_port & 0x00FF);

		//Port command csv
		String cmd = "PORT " + Common.convertToUnsignedShort(ip[0]) + "," +
		Common.convertToUnsignedShort(ip[1]) + "," + Common.convertToUnsignedShort(ip[2]) +
		"," +  Common.convertToUnsignedShort(ip[3]) + "," +
		Common.convertToUnsignedShort(port[0]) + "," + Common.convertToUnsignedShort(port[1]);

		//Send port command
		Common.sendCommand(cmd, peer);
	}
	
	public static boolean connected(String response, Peer peer) {
		if(!response.startsWith("200")) {
			Logger.write("Unable to connect to peer " + peer.getHost() + ":" + peer.getPort());
			return false;
		}

		return true;
	}

	public static boolean openPort(String response, Peer peer) {
		if (!response.startsWith("200")) {
			Logger.write("Couldn't open data connection with server at peer " + peer.getHost() + ":" + peer.getPort());
			return false;
		}

		return true;
	}

	public static boolean replied(String response, Peer peer, String code) {
		if (!response.startsWith(code)) {
			Logger.write("Failed to talk to peer " + peer.getHost() + ":" + peer.getPort() + " retrying...");
			return false;
		}

		return true;
	}
	
	public static void sendCommand(String command, Peer peer) throws IOException 
	{
		peer.getBufferedWriter().write(command+"\r\n");
		peer.getBufferedWriter().flush();
	}

	public static short convertToUnsignedShort(byte b)
	{
		return ( b < 0 ) ? (short) (b + 256) : (short) b;
	}

	public static String combine(String[] s, String glue){
		int k=s.length;
		if (k==0)
			return null;
		StringBuilder out=new StringBuilder();
		out.append(s[0]);
		for (int x=1;x<k;++x)
			out.append(glue).append(s[x]);
		return out.toString();
	}
}
