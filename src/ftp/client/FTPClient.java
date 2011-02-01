package ftp.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author E. Javier Figueroa 7/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class FTPClient 
{
	private Socket socket = null;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	private String _host = null;
	private int _port;

	public String getHost()
	{
		return _host;
	}

	private void setHost(String host)
	{
		_host = host;
	}

	public int getPort()
	{
		return _port;
	}

	private void setPort(int port)
	{
		_port = port;
	}

	public FTPClient(String host, int port) 
	{
		setHost(host);
		setPort(port);
	}

	/**
	 * @param host
	 * @param port
	 * @return Message about the status of the connection
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public synchronized String connect() throws UnknownHostException, IOException 
	{
		this.socket = new Socket(getHost(), getPort());
		this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

		String serverResponse = reader.readLine();
		if (!serverResponse.startsWith("200")) 
		{
			throw new IOException("Can't connect to the FTP ftp.server right now, ftp.server said: " + serverResponse);
		}

		return "Connection established";
	}
	
	/**
	 * @throws IOException
	 */
	public void disconnect() throws IOException 
	{
		sendCommand("QUIT");
		this.socket.close();
		this.writer.close();
		this.reader.close();
	}

	/**
	 * @param username
	 * @param password
	 * @return Message about the authentication result for the active connection
	 * @throws IOException
	 */
	public synchronized String authenticate(String username, String password) throws IOException 
	{
		this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));

		sendCommand("USER " + username);
		String serverResponse = this.reader.readLine();
		if (!serverResponse.startsWith("331 ")) 
		{
			throw new IOException("Provided username is invalid, ftp.server said: " + serverResponse);
		}

		sendCommand("PASS " + password);
		serverResponse = this.reader.readLine();
		if (!serverResponse.startsWith("230 ")) 
		{
			throw new IOException("Provided password is invalid, ftp.server said: " + serverResponse);
		}

		return "You're in, start uploading and downloading files";
	}

	/**
	 * @param command
	 * @param filename
	 * @return result message from commands execution
	 * @throws IOException
	 * 
	 * This is a hardcoded solution to only support the commands required for the assignment
	 */
	public synchronized String executeCommand(Commands command, String filename) throws IOException 
	{
		switch(command) 
		{
		case dir : 
		{
			return sendList();
		}
		case get : 
		{
			return getFile(filename);
		}
		case upload : 
		{
			return uploadFile(filename);
		}
		default : 
		{
			return "Invalid command";
		}
		}
	}

	/**
	 * @return Message about the result of the list command
	 * @throws IOException
	 */
	public synchronized String sendList() throws IOException 
	{
		sendCommand("LIST");

		String response = this.reader.readLine();
		if (response.startsWith("425 ") || response.startsWith("550 ")) 
		{
			throw new IOException("No files found, ftp.server said: " + response); 
		} 

		PrintWriter fileStream = new PrintWriter(System.out, true);
		response = this.reader.readLine();
		while (response != null && !response.isEmpty()) 
		{
			fileStream.println(response);
			response = this.reader.readLine();

			if (response.startsWith("226 ")) 
			{
				break;
			}
		}

		return response;
	}

	/**
	 * @param filename
	 * @return Message about the result of the retr command
	 * @throws IOException
	 */
	public synchronized String uploadFile(String filename) throws IOException 
	{
		int port = openPassiveSocket();
		return sendStor(filename, port);
	}

	/**
	 * @param filename
	 * @return Message about the result of the retr command
	 * @throws IOException
	 */
	public synchronized String getFile(String filename) throws IOException 
	{
		ServerSocket socket = openActiveSocket();
		return sendRetr(filename, socket);
	}
	
	private String sendStor(String filename, int port) throws IOException {

		sendCommand("STOR " + filename);

		String response = this.reader.readLine();
		if (response.startsWith("553 ") || response.startsWith("550 ") || response.startsWith("500 ")) 
		{
			throw new IOException("Couldn't upload file: " + filename + ", ftp.server said: " + response); 
		}

		Socket dataSocket = new Socket(getHost(), port);
		File file = new File(filename);
		FileInputStream fileInputStream = new FileInputStream(file);
		OutputStream outputStream = dataSocket.getOutputStream();

		byte buf[] = new byte[1024];
		int nread;
		while ((nread = fileInputStream.read(buf, 0, 1024)) > 0)
		{
			outputStream.write(buf, 0, nread);
		}

		outputStream.close();

		return this.reader.readLine();
	}

	private String sendRetr(String filename, ServerSocket socket) throws IOException 
	{		
		sendCommand("RETR " + filename);

		String response = this.reader.readLine();
		if (response.startsWith("553 ") || response.startsWith("550 ") || response.startsWith("500 ")) 
		{
			throw new IOException("Couldn't download file: " + filename + ", ftp.server said: " + response); 
		}

		File file = new File(filename);
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		InputStream inputStream = socket.accept().getInputStream();

		byte buf[] = new byte[1024];
		int nread;
		while ((nread = inputStream.read(buf, 0, 1024)) > 0)
		{
			fileOutputStream.write(buf, 0, nread);
		}

		fileOutputStream.flush();

		return this.reader.readLine();
	}

	private int openPassiveSocket() throws IOException 
	{	
		ServerSocket dummy = new ServerSocket(0);
		int s_port = dummy.getLocalPort();
		dummy.close();		
		InetAddress localhost =  this.socket.getLocalAddress();

		sendPort(s_port, localhost);

		return s_port;
	}

	private ServerSocket openActiveSocket() throws IOException 
	{	
		ServerSocket socket = new ServerSocket(0);
		InetAddress localhost =  this.socket.getLocalAddress();
		int s_port =  socket.getLocalPort();

		sendPort(s_port, localhost);

		return socket;
	}

	private void sendPort(int s_port, InetAddress localhost) throws IOException 
	{
		//Build command
		byte[] ip = localhost.getAddress();
		byte[] port = new byte[2];

		//Build port for command
		port[0] =(byte) (s_port >> 8); 
		port[1] =(byte) (s_port & 0x00FF);

		//Port command csv
		String cmd = "PORT " + convertToUnsignedShort(ip[0]) + "," +
		convertToUnsignedShort(ip[1]) + "," + convertToUnsignedShort(ip[2]) +
		"," +  convertToUnsignedShort(ip[3]) + "," +
		convertToUnsignedShort(port[0]) + "," + convertToUnsignedShort(port[1]);

		//Send port command
		sendCommand(cmd);

		String response = this.reader.readLine();
		if (!response.startsWith("200 ")) 
		{
			throw new IOException("Couldn't open data connection with server, ftp.server said: " + response); 
		}
	}

	private void sendCommand(String command) throws IOException 
	{
		this.writer.write(command+"\r\n");
		this.writer.flush();
	}

	private short convertToUnsignedShort(byte b)
	{
		return ( b < 0 ) ? (short) (b + 256) : (short) b;
	}
}
