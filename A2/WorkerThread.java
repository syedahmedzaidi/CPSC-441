/**
 * @author Syed Ahmed Zaidi Roll Number = 10150285 
 * @version 1.0, Nov 04, 2016
 * WorkerThread.java
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
public class WorkerThread extends Thread{

	private Socket sock;//The socket it communicates with the client on.
	
	public WorkerThread(Socket s)
	{
		sock = s;
	}

	public void run ()
    {
    	String string;
    	int num_byte_read = 0;
    	String http_response_header_string = null;
    	String divider = "/";
    	FileInputStream fileInput;
    	
		try{
 			Scanner inputStream = new Scanner(sock.getInputStream(), "UTF-8"); 
 			string = inputStream.nextLine();
 			System.out.println(string);
 			//GET_/pathname.something_HTTP/1.1\r\n
 			//GET_/pathname.something_HTTP/1.0\r\n
 			//We split the string where ever there is a space as seen above where space is _
 			//Same algorithm implemented in Assignment 1 for splitting response string.
 			String[] splitAtSpace = string.split(" ");

 			//check if request is 400 Bad Request using following predefined facts
 			//1) Doesnt begin with GET
 			//2) or If there isn't HTTP/1.1\r\n or HTTP/1.0\r\n
 			//3) or any of the fields is missing.
 			if(!splitAtSpace[0].equals("GET") || (!splitAtSpace[2].equals("HTTP/1.1") && !splitAtSpace[2].equals("HTTP/1.0")) || splitAtSpace.length < 3){
 				String badRequest = new String("400 Bad Request\r\n");
 				String connection_close = "connection: close\r\n";
 				String eoh_line = "\r\n";
 				String badRequest_response = badRequest + connection_close + eoh_line;
 				byte[] badRequest_in_bytes = badRequest_response.getBytes("US-ASCII");
 				sock.getOutputStream().write(badRequest_in_bytes);
 				sock.getOutputStream().flush();
 				sock.close();
 				return;
 			}else{
 				//Check if file exist
 				//First remove the divider that was appended in request
 				//Check if file exist, if it doesnt, send 404 not found
 				String[] fileName_without_path = splitAtSpace[1].split(divider, 2);
 				File file = new File(fileName_without_path[1]);
 				
 				if(!file.exists()){
 					String notFound = new String("404 Not Found\r\n");
 					String connection_close = "connection: close\r\n";
 					String eoh_line = "\r\n";
 					String notFound_response = notFound + connection_close + eoh_line;
 					byte[] notFound_in_bytes = notFound_response.getBytes("US-ASCII");
 					sock.getOutputStream().write(notFound_in_bytes);
 					sock.getOutputStream().flush();
 					sock.close();
 					return;
 				}

 				//If all cases pass (request header is correct)
 				//File parsing into byte method logic from stackoverflow
 				//http://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
 				fileInput = new FileInputStream(file);
 				long length = file.length();
 				byte[] file_in_bytes = new byte[(int) length];
		   	 	fileInput.read(file_in_bytes);
		    	fileInput.close();

		    	//send 200 OK Response
		    	String ok_response = new String("HTTP/1.1 200 OK\r\n");
 				String connection_close = "connection: close\r\n";
 				String eoh_line = "\r\n";
 				String date ="Date: 2016/11/04\r\n";
 				String server ="Server: Local Host\r\n";
 				String last_modified ="Last Modified: 12:16 AM\r\n";
 				String content_length = "Content Length: "+file.length()+"\r\n";
 				String end_of_response = date + server + last_modified + content_length + connection_close + eoh_line;
 				byte[] ok_response_in_bytes = ok_response.getBytes("US-ASCII");
 				byte[] end_of_response_in_bytes = end_of_response.getBytes("US-ASCII");
 				sock.getOutputStream().write(ok_response_in_bytes);
 				sock.getOutputStream().write(end_of_response_in_bytes);
 				sock.getOutputStream().write(file_in_bytes);
 				sock.getOutputStream().flush();
 				sock.close();
 				return;
 			}
 			
		}catch(IOException e){
			 e.printStackTrace();
		}
    }
}




				