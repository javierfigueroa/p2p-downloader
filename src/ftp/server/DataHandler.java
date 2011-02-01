package ftp.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author E. Javier Figueroa 7/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class DataHandler 
{

	private Connection connection = null;
	private String dataHost;
	private int dataPort;
	private final int bufferSize = 1024;

	public void setDataPort(String host, int port)
	{
		dataHost = host;
		dataPort = port;
	}

	public DataHandler(Connection connection) 
	{
		this.connection = connection;
	}

	public int getFileList(String path) 
	{
		Logger.write(this.connection.getUsername(), "Displaying files for path: " + path);
		try 
		{
			File dir = new File(path);
			String fileNames[] = dir.list();
			int numFiles = fileNames != null ? fileNames.length : 0;
			Socket dataSocket = this.connection.getSocket();
			PrintWriter writer = new PrintWriter(dataSocket.getOutputStream());

			writer.print("total " + numFiles + "\n");
			for (int i = 0; i < numFiles; i++)
			{
				String fileName = fileNames[i];
				File file = new File(dir, fileName);
				
				writer.println(file.getName());
			}

			writer.flush();

			return this.connection.sendReply(226, "Transfer complete.");
		}
		catch(IOException e) 
		{
			Logger.write(this.connection.getUsername(), e.getMessage());
			return this.connection.sendReply(550, "No such directory.");
		}
		catch(Exception e) 
		{
			Logger.write(this.connection.getUsername(), e.getMessage());
			return this.connection.sendReply(425, "Can't open data connection.");
		}
		
	}

	public int storeFile(String path) throws IOException 
	{
		Logger.write(this.connection.getUsername(), "Storing file: " + path);	
		try 
		{
			File file = new File(path);
			if (file.isFile())
			{
				Logger.write(this.connection.getUsername(), "550, P2PFile exists in that location.");	
				return this.connection.sendReply(550, "P2PFile exists in that location.");
			}
			
			if (dataPort == -1)
			{
				Logger.write(this.connection.getUsername(), "500, Can't establish data connection specified.");	
				return this.connection.sendReply(500, "Can't establish data connection specified.");
			}
			
			ServerSocket dataSocket = new ServerSocket(dataPort);
			this.connection.sendReply(150, "Opening active mode data connection.");
			
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			InputStream inputStream = dataSocket.accept().getInputStream();
	
			byte buf[] = new byte[bufferSize];
			int nread;
			while ((nread = inputStream.read(buf, 0, bufferSize)) > 0)
			{
				fileOutputStream.write(buf, 0, nread);
			}

			inputStream.close();

			return this.connection.sendReply(226, "Transfer complete.");
		}
		catch(IOException e) 
		{
			Logger.write(this.connection.getUsername(), e.getMessage());
			return this.connection.sendReply(553, "Not a regular file.");
		}
	}

	public int retrieveFile(String path) throws IOException 
	{
		Logger.write(this.connection.getUsername(), "Retrieving file: " + path);
		try 
		{
			File file = new File(path);
			if (!file.isFile())
			{
				Logger.write(this.connection.getUsername(), "550, Not a plain file.");	
				return this.connection.sendReply(550, "Not a plain file.");
			}

			if (dataPort == -1)
			{
				Logger.write(this.connection.getUsername(), "500, Can't establish data connection specified.");	
				return this.connection.sendReply(500, "Can't establish data connection specified.");
			}
			
			Socket dataSocket = new Socket(dataHost, dataPort);
			this.connection.sendReply(150, "Opening active mode data connection.");
			
			FileInputStream fileInputStream = new FileInputStream(file);
			OutputStream outputStream = dataSocket.getOutputStream();
			
			byte buf[] = new byte[bufferSize];
			int nread;
			while ((nread = fileInputStream.read(buf, 0, bufferSize)) > 0)
			{
				outputStream.write(buf, 0, nread);
			}

			outputStream.close();

			return this.connection.sendReply(226, "Transfer complete.");
		}
		catch(IOException e) 
		{
			Logger.write(this.connection.getUsername(), e.getMessage());
			return this.connection.sendReply(553, "Not a regular file.");
		}
	}
}
