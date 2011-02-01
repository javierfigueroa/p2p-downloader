package p2p.peer.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import p2p.peer.Logger;
import p2p.peer.P2PFile;
import p2p.peer.Program;

/**
 * @author E. Javier Figueroa 11/27/2010 CNT 5106c Fall 2010 
 * University of Florida
 * 
 */
public class SocketMessenger {
	private Connection connection;
	private String dataHost;
	private int dataPort;

	public void setDataPort(String host, int port) {
		dataHost = host;
		dataPort = port;
	}

	public SocketMessenger() {
	}

	public SocketMessenger(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Starts an active socket connection and collects chunks of bytes 
	 * 
	 * @param filename
	 * @param chunkIndexes
	 * @return 226 if successfull, 500 otherwise
	 * @throws IOException
	 */
	public synchronized int collect(String filename, String[] chunkIndexes) throws IOException {
		Logger.write("Saving chunks for file: " + filename + " (" + Program.p2pFiles.get(filename).getSize() + " bytes)");
		ServerSocket dataSocket = new ServerSocket(dataPort);
		this.connection.reply(150);

		int code = collect(filename, chunkIndexes, dataSocket);
		return this.connection.reply(code);
	}

	/**
	 * Collect chunks of bytes via the provided active connection
	 * 
	 * @param filename
	 * @param chunkIndexes
	 * @param dataSocket
	 * @return 226 
	 * @throws IOException
	 */
	public synchronized int collect(String filename, String[] chunkIndexes, ServerSocket dataSocket) throws IOException {
		InputStream inputStream = dataSocket.accept().getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		byte[] buf = new byte[Common.BUFFER_SIZE];
		int nread;
		int chunk = 0;
		while ((nread = inputStream.read(buf, 0, Common.BUFFER_SIZE)) > 0) {
			outputStream.write(buf, 0, nread);
			chunk += nread;
		}

		inputStream.close();
		return map(filename, chunkIndexes, outputStream, chunk);
	}
	

	/**
	 * Stores bytes in TreeSet
	 * 
	 * @param filename
	 * @param chunkIndexes
	 * @param outputStream
	 * @param chunk
	 * @return 226 
	 */
	private synchronized int map(String filename, String[] chunkIndexes, ByteArrayOutputStream outputStream, int chunk) {

		P2PFile file = Program.p2pFiles.get(filename);
		int size = chunk / chunkIndexes.length;
		byte[] data = new byte[size];
		ByteArrayInputStream is = new ByteArrayInputStream(outputStream
				.toByteArray());
		int chunkIndex = 0;

		while ((is.read(data, 0, data.length)) > 0) {
			long key = Long.parseLong(chunkIndexes[chunkIndex++]);

			if (file.getChunks().get(key).length == 0) {
				file.getChunks().put(key, data);
				Logger.write("Received chunk:" + key);
			}
			data = new byte[size];
		}

		return 226;
	}

	/**
	 * Send bytes to peer via socket
	 * 
	 * @param port
	 * @param offset
	 * @param length
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public synchronized void send(String host, int port, long offset, int length,	File file) throws UnknownHostException, IOException {
		Socket dataSocket = new Socket(host, port);
		FileInputStream fileInputStream = new FileInputStream(file);
		OutputStream outputStream = dataSocket.getOutputStream();

		fileInputStream.skip(offset);
		byte buf[] = new byte[Common.BUFFER_SIZE];
		int nread;
		int chunk = 0;
		while ((nread = fileInputStream.read(buf, 0, Common.BUFFER_SIZE)) > 0 && chunk < length) {
			outputStream.write(buf, 0, nread);
			chunk += nread;
		}
		
		outputStream.close();
	}

	/**
	 * Send chunks
	 * 
	 * @param file
	 * @param chunks
	 * @return 226
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public synchronized int send(P2PFile file, String[] chunks) throws UnknownHostException, IOException {
		Socket dataSocket = new Socket(this.dataHost, this.dataPort);
		OutputStream outputStream = dataSocket.getOutputStream();

		for (String index : chunks) {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getChunks().get(Long.parseLong(index)));
			byte buf[] = new byte[Common.BUFFER_SIZE];
			int nread;
			while ((nread = inputStream.read(buf, 0, Common.BUFFER_SIZE)) > 0) {
				outputStream.write(buf, 0, nread);
			}

			Logger.write("Sent chunk:" + index);
		}

		outputStream.close();

		return this.connection.reply(226);
	}


	/**
	 * Stores bytes into file
	 * 
	 * @param filename
	 * @return 226 if successfull, 500 otherwise
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public synchronized int store(String filename) throws FileNotFoundException, IOException {
		P2PFile file = Program.p2pFiles.get(filename);

		if (file.complete()) {
			Logger.write("The file " + filename + " is complete. Storing...");
			FileOutputStream fileOutputStream = new FileOutputStream(file.getFileName());

			for (Long chunkIdx : file.getChunks().keySet()) {
				fileOutputStream.write(file.getChunks().get(chunkIdx), 0, file.getChunks().get(chunkIdx).length);
			}
			Logger.write("The file " + filename + " has been stored");

			return this.connection.reply(226);
		} 
		
		return this.connection.reply(500);
	}
}
