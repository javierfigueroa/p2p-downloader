package ftp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * @author E. Javier Figueroa 7/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class Connection implements Runnable {

	private Socket clientSocket = null;
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	private DataHandler dataHandler = null;
	private String username = null;

	public Socket getSocket()
	{
		return clientSocket;
	}

	public String getUsername()
	{
		return username;
	}

	private void setUsername(String username)
	{
		this.username = username;
	}

	public Connection(Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;
		this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		this.writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

		this.dataHandler = new DataHandler(this);
	}

	@Override
	public void run() {
		try
		{
			listenForCommands();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				this.clientSocket.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.clientSocket.close();
	}

	private void listenForCommands() throws Exception 
	{
		sendReply(200, "FTP server is ready.");

		String line = null;
		while ((line = reader.readLine()) != null)
		{
			StringTokenizer st = new StringTokenizer(line);
			String command = st.nextToken().toLowerCase();

			int code = 0;
			if (command.equals("user")) 
			{
				code = saveUser(st);
			}
			else if (command.equals("pass")) 
			{
				code = savePassword(st);
			}
			else if (command.equals("list")) 
			{
				code = listFiles(st);
			}
			else if (command.equals("stor")) 
			{
				code = storeFile(line, st);
			}
			else if (command.equals("retr")) 
			{
				code = retrieveFile(line, st);
			}
			else if (command.equals("port")) 
			{
				code = setPort(line, st);
			}
			else if (command.equals("quit")) 
			{
				code = quit();
			}

			if (code == 221 || code == 0) return;
		}

	}

	private int saveUser(StringTokenizer st) 
	{
		setUsername(st.nextToken());
		return sendReply(331, "Password required for " + getUsername() + ".");
	}

	private int savePassword(StringTokenizer st) throws Exception 
	{
		if (getUsername() == null)
		{
			throw new Exception("503 Login with USER first.");
		}

		Logger.write(getUsername(), "Logged in");
		return sendReply(230, "User " + getUsername() + " logged in.");
	}

	private int quit() throws IOException 
	{
		this.clientSocket.close();
		Logger.write(getUsername(), "Logged out");
		return sendReply(221, "Goodbye.");
	}

	private int retrieveFile(String line, StringTokenizer st) throws IOException 
	{	
		String path = null;
		try
		{
			path = line.substring(5);
		}
		catch (Exception e)
		{
			throw new NoSuchElementException(e.getMessage());
		}

		path = this.getDirectoryPath(path);

		return this.dataHandler.retrieveFile(path);

	}

	private int storeFile(String line, StringTokenizer st) throws IOException {

		String path = null;
		try
		{
			path = line.substring(5);
		}
		catch (Exception e)
		{
			throw new NoSuchElementException(e.getMessage());
		}

		path = this.getDirectoryPath(path);

		return this.dataHandler.storeFile(path);
	}

	private int setPort(String line, StringTokenizer st)
	{
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

		this.dataHandler.setDataPort(dataHost, dataPort);

		return sendReply(200, "PORT command successful.");
	}

	private int listFiles(StringTokenizer st) throws IOException {
		String path = null;
		if (st.hasMoreTokens())
			path = st.nextToken();
		else
			path = "/";

		path = getDirectoryPath(path);

		return this.dataHandler.getFileList(path);
	}

	private String getDirectoryPath(String filename) {
		String path = null;
		if (filename.charAt(0) == '/')
			path = System.getProperty("user.dir") + filename;
		else
			path = System.getProperty("user.dir") + "/" + filename;

		return path;
	}

	int sendReply(int code, String text)
	{
		this.writer.println(code + " " + text);
		return code;
	}
}
