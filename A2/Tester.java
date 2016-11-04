
/**
 * A simple test driver
 * 
 * @author 	Majid Ghaderi
 * @version	4.0, Oct 19, 2016
 *
 */
 
import java.util.*;

public class Tester {
	
	public static void main(String[] args) {
		int serverPort = 2225;

		// parse command line args
		if (args.length == 1) {
			serverPort = Integer.parseInt(args[0]);
		}

		System.out.println("starting the server on port " + serverPort);
		WebServer server = new WebServer(serverPort);
		server.start();
		System.out.println("server started. Type \"quit\" to stop");
		System.out.println(".....................................");

		Scanner keyboard = new Scanner(System.in);
		while ( !keyboard.next().equals("quit") );

		System.out.println();
		server.shutdown();
		System.out.println("server stopped.");
	}

}
