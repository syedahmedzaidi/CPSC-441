
/**
 * @author Syed Ahmed Zaidi Roll Number = 10150285 
 * UrlCache.java
 */
import java.io.*;
import java.util.HashMap;
import java.net.Socket;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.util.Calendar;
public class UrlCache {

	HashMap<String, String> catalog;

    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw exception.
	 * 
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public UrlCache() throws UrlCacheException {
		
		//create file in the working directory
		File dir = new File(System.getProperty("user.dir"));
	
		// if catalog exists then put local catalog objects in it.
		if(new File(dir, "catalog.ser").exists() == true){
		    try{
				FileInputStream fileInput = new FileInputStream("catalog.ser");
				ObjectInputStream catalogInput  = new ObjectInputStream(fileInput);
				catalog = (HashMap<String,String>)catalogInput.readObject();
				fileInput.close();
				catalogInput.close();
		    }catch(IOException e){
				System.out.println(e);
		    }catch(Exception e){
		    	System.out.println(e);
		    }
		}
		// make a new file if catalog doesnt exist
		else{
		    File fileOutput = new File("catalog.ser");
		    catalog = new HashMap<String,String>();
		}
	}	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public void getObject(String url) throws UrlCacheException {

		String host;
		int port;
		String filepath;
		String [] hostandport;
		String divider = "/";
		String portSymbol =":";
		String http_header = null;
		int off = 0;

		// GET HOST AND PORT NO. Cyriac James algorithm
	    String [] parts = url.split(divider, 2);
		if(parts[0].contains(portSymbol)) { //if the url has a port specified

			hostandport = parts[0].split(portSymbol, 2);
			host = hostandport[0];
			port = Integer.parseInt(hostandport[1]);	
			filepath = divider + parts[1];

		} else { //if the port is not specified.

			host = parts[0];
			port = 80;
			filepath = divider + parts[1];

		}
		//create socket to download file
		// create input and output streams
		try{

			Socket socket = new Socket(host, port);	
			String requestLine_1 = "GET " + filepath + " HTTP/1.1\r\n";
 			String requestLine_2 = "Host: " + host + "\r\n";
 			String eoh_line = "\r\n";
 			http_header = requestLine_1 + requestLine_2 + eoh_line;
 			byte [] http_header_in_bytes = http_header.getBytes("US-ASCII");
			System.out.println(http_header); 			
 			socket.getOutputStream().write(http_header_in_bytes);
 			socket.getOutputStream().flush();


	 		File file = new File(host+"/" +filepath);
			file.getParentFile().mkdirs();
			FileOutputStream outStream = new FileOutputStream(file);

			byte[] http_response_header_bytes = new byte[2048];
			byte[] http_object_byte = new byte[1024];
	        int objectLength;
			int num_byte_read = 0;
			String http_response_header_string = null;

			while(num_byte_read!= -1){

				socket.getInputStream().read(http_response_header_bytes, off, 1);
				off++;
				http_response_header_string = new String(http_response_header_bytes, 0, off,"US-ASCII");
				if(http_response_header_string.contains("\r\n\r\n")){
					break;
				}
			}

			long lastmodi = getLastModified(http_response_header_string);

			if(http_response_header_string.contains("304 Not Modified")){
				// add logic to handle not modified files
				System.out.println("File is already in Catalog.");	
			}
			else if(http_response_header_string.contains("200 OK")){
				int counter = 0;
				String[] splitAtContentLength = http_response_header_string.split("Content-Length: ", 2);
				String[] splitAtEoL2 = splitAtContentLength[1].split("\r\n",2);
				objectLength = Integer.parseInt(splitAtEoL2[0]);

				while(num_byte_read!= -1){
					if(counter == objectLength){
						break;
					}
					num_byte_read = socket.getInputStream().read(http_object_byte);
					outStream.write(http_object_byte, 0, num_byte_read);
					counter = counter + num_byte_read;
					outStream.flush();
				}
			}

			File directory = new File(System.getProperty("user.dir"));
		    FileOutputStream fileOutput = new FileOutputStream(directory + "/catalog.ser");
		    ObjectOutputStream catalogOutput = new ObjectOutputStream(fileOutput);
		    catalogOutput.writeObject(catalog);
		    fileOutput.close();
		    catalogOutput.close();
			outStream.close();
			socket.close();
		
		}catch(IOException e){
			System.out.println(e);
		}

			
			/*System.out.println(host);
			System.out.println(port);
			System.out.println(filepath);
			System.out.println(http_header);*/	
	}
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     * @throws UrlCacheException if the specified url is not in the cache, or there are other errors/exceptions
     */
	public long getLastModified(String url) throws UrlCacheException {

		int off = 0;
		String http_header = null;
		long lastmod = 0;
		String [] hostandport;
		String divider = "/";
		String portSymbol =":";
		if(url.contains("200 OK")){
			String[] splitAtLastModified = url.split("Last-Modified: ", 2);
			String[] splitAtEoL1 = splitAtLastModified[1].split("\r\n", 2);
			String lastModDate = splitAtEoL1[0];
			SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss zzz");
			DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss zzz");
		   	Date currentDate = new Date();
		   	//System.out.println(dateFormat.format(date1));
			try{
				Date lastDate = formatter.parse(lastModDate);
				//System.out.println(dateFormat.format(lastDate));
				lastmod = currentDate.getTime() - lastDate.getTime();
				//System.out.println(lastmod);
			}catch(Exception e){
				System.out.println(e);
			}
			return lastmod;
		}
		else{
			String host;
			int port;
			String filepath;
			//break url
			String [] parts = url.split(divider, 2);
			if(parts[0].contains(portSymbol)) { //if the url has a port specified

				hostandport = parts[0].split(portSymbol, 2);
				host = hostandport[0];
				port = Integer.parseInt(hostandport[1]);	
				filepath = divider + parts[1];

			}else { //if the port is not specified.

				host = parts[0];
				port = 80;
				filepath = divider + parts[1];

			}
			try{
				//open socket to get header
				Socket socket = new Socket(host, port);	
				String requestLine_1 = "GET " + filepath + " HTTP/1.1\r\n";
		 		String requestLine_2 = "Host: " + host + "\r\n";
		 		String eoh_line = "\r\n";
		 		http_header = requestLine_1 + requestLine_2 + eoh_line;
		 		byte [] http_header_in_bytes = http_header.getBytes("US-ASCII");
		 			
		 		socket.getOutputStream().write(http_header_in_bytes);
		 		socket.getOutputStream().flush();


				byte[] http_response_header_bytes = new byte[2048];
				byte[] http_object_byte = new byte[1024];
				int num_byte_read = 0;
				String http_response_header_string = null;

				while(num_byte_read!= -1){

					socket.getInputStream().read(http_response_header_bytes, off, 1);
					off++;
					http_response_header_string = new String(http_response_header_bytes, 0, off,"US-ASCII");
					if(http_response_header_string.contains("\r\n\r\n"))
					{
						break;
					}
				}
				//find header for last modified
				String[] splitAtLastModified = http_response_header_string.split("Last-Modified: ", 2);
				String[] splitAtEoL1 = splitAtLastModified[1].split("\r\n", 2);
				String lastModDate = splitAtEoL1[0];
				SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss zzz");
				DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss zzz");
			   	//get current date time with Date()
			   	Date currentDate = new Date();
			   	//System.out.println(dateFormat.format(date1));
				
				Date lastDate = formatter.parse(lastModDate);
				//System.out.println(dateFormat.format(lastDate));
				lastmod = currentDate.getTime() - lastDate.getTime();
				socket.close();
				return lastmod;
			}catch(IOException e){
				System.out.println(e);
			}catch(Exception e){
				System.out.println(e);
			}		
			
		}
		return lastmod;
	}
}
		
