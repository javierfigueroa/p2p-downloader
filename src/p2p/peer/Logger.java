package p2p.peer;

public class Logger {
	public static void write(String message)
	{
		System.out.println(String.format("peer:%s", message));
	}
}
