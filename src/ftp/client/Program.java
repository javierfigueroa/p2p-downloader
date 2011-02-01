package ftp.client;

import java.io.Console;
import java.io.IOException;

/**
 * @author E. Javier Figueroa 7/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class Program {

	public static void main(String [] args) throws Exception 
	{
		if (args.length > 2)
		{
			System.err.println("Usage: ftpclient.jar [ ftp server ] [ port ]");
			System.exit(1);
		}

		if (args.length != 2) 
		{
			System.err.println("Usage: ftpclient.jar [ ftp server ] [ port ]");
			System.exit(1);
		}

		try
		{
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			FTPClient client = new FTPClient(host, port);

			String message = client.connect();
			System.out.println(message);
			
			Console console = System.console();
			String username = console.readLine("Enter your username: ");
			String password = console.readLine("Enter your password: ");

			if (!username.isEmpty() && !password.isEmpty())
			{
				message = client.authenticate(username, password);
				System.out.println(message);

				String command = "";
				while(!(command = console.readLine("> ")).equalsIgnoreCase("quit")) 
				{
					try
					{
						if (command.startsWith("dir")) 
						{
							message = client.sendList();
						}
						else if (command.startsWith("get")) 
						{
							message = client.getFile(command.substring(4));
						}
						else if (command.startsWith("upload")) 
						{
							message = client.uploadFile(command.substring(7));
						}

						if (message.isEmpty()) 
						{
							message = "Invalid command, available commands: dir, get <filename>, upload <filename>";
						}

						System.out.println(message);
						message = "";
					}
					catch(IOException e)
					{
						System.out.println(e.getMessage());
					}
				}

				client.disconnect();
			}
			else
			{
				throw new Exception("Please enter a username and password.");
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		System.out.println("Bye.");
		System.exit(0);
	}
}
