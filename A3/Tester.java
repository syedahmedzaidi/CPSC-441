/**
 * 
 * A simple test driver
 * 
 * @author 	Majid Ghaderi
 * @version	2.1
 *
 */

class Tester {

	public static void main(String[] args) {
		// all srguments should be provided
		// as described in the assignment description 
		if (args.length != 5) {
			System.out.println("wrong number of arguments, try agaon.");
			System.out.println("usage: java Tester server port file window timeout");
			System.exit(0);
		}
		
		// parse the command line arguments
		// assume no errors
		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		int windowSize = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);

		// send the file to server
		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}

}
