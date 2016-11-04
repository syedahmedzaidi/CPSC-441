/**
 * @author Syed Ahmed Zaidi Roll Number = 10150285
 * @version 1.0, Nov 04, 2016
 * WebServer.java
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
public class WebServer extends Thread { 
	
	private Socket connection;
	private ServerSocket serverSocket;
	private int port;
	private volatile boolean shutdown = false;

	public WebServer(int port)
	{
		try{
			serverSocket = new ServerSocket(port);
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}//end contructor

	public void run()
	{
		try
		{
		 	while(!shutdown)
		 	{
		 		try{
					 Socket socket = serverSocket.accept();
					 WorkerThread tcpconn = new WorkerThread(socket);
					 tcpconn.start();
		 		}catch (Exception e){ // related to this particular client -- Ignore anyway
		 			System.out.println(e.getMessage());
				}
		 	} //close while
		}catch (Exception e){ //related to server – Ignore
			e.printStackTrace();
		} 
 	}

 	public void shutdown()
 	{
 		shutdown = true;
 	}

}
