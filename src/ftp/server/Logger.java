package ftp.server;

/**
 * @author E. Javier Figueroa 7/27/2010
 * CNT 5106c Fall 2010
 * University of Florida
 *
 */
public class Logger 
{
	public static void write(String username, String message)
	{
		System.out.println(String.format(">{username: %s} -> {message: %s}", username, message));
	}
}
