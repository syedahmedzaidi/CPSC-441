import java.io.*;
import cpsc441.a3.*;
import java.net.Socket;
import java.io.IOException;
import java.io.DataOutputStream;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Timer;
import java.net.InetAddress;

/**
 * FastFtp Class
 *
 * FastFtp implements a basic FTP application based on UDP data transmission.
 * The main mehtod is send() which takes a file name as input argument and send the file
 * to the specified destination host.
 *
 */
public class FastFtp {

    private int rtoTimer;
    private String serverName;
    private int serverPort;
    private Socket socket;
    private DataOutputStream outputstream = null;
    private DataInputStream inputstream = null;
    private FileInputStream file_in_stream  = null;
    private String fileName;
    private InetAddress localAddress;
    private DatagramSocket clientSocket;
    private TxQueue queue;
    private Timer timer;

    /**
     * Constructor to initialize the program
     *
     * @param windowSize	Size of the window for Go-Back_N (in segments)
     * @param rtoTimer		The time-out interval for the retransmission timer (in milli-seconds)
     */
	public FastFtp(int windowSize, int rtoTimer) {
		//
		// to be completed
		//
		queue = new TxQueue(windowSize);
		this.rtoTimer = rtoTimer;
	}


    /**
     * Sends the specified file to the specified destination host:
     * 1. send file name and receiver server confirmation over TCP
     * 2. send file segment by segment over UDP
     * 3. send end of transmission over tcp
     * 3. clean up
     *
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName		Name of the file to be trasferred to the rmeote server
     */
	public void send(String serverName, int serverPort, String fileName) {
		//
		// to be completed
		//
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.fileName = fileName;
		try{
			try{
				socket = new Socket(serverName, serverPort);
				this.localAddress = InetAddress.getByName(this.serverName);
				outputstream = new DataOutputStream(socket.getOutputStream());
				inputstream = new DataInputStream(socket.getInputStream());
				outputstream.writeUTF(this.fileName);
				outputstream.flush();

				if (inputstream.readByte() != 0){
					System.out.println("No File read.");
					socket.close();
					return;
				}//we continue
				else{
					int localPortNum = socket.getLocalPort();
					clientSocket= new DatagramSocket(localPortNum);

					//create file and file input stream
					File file = new File(this.fileName);
					file_in_stream = new FileInputStream(file);

					//file segments
					byte[] file_in_bytes = new byte[Segment.MAX_PAYLOAD_SIZE];

					//sequence number for segement starts at 0 and is incremented per every segment transmitted
					int seqNum = 0;

					ReceiverThread rcvthread = new ReceiverThread(this, clientSocket, true);
					rcvthread.start();
					Segment packet = null;
					int numOfBytes;
					//while((numOfBytes = file_in_stream.read(file_in_bytes)) > 0){
					while((numOfBytes = file_in_stream.read(file_in_bytes)) != -1){
						//http://stackoverflow.com/questions/3329163/is-there-an-equivalent-to-memcpy-in-java
						if(numOfBytes < Segment.MAX_PAYLOAD_SIZE){
							byte[] new_file_in_bytes = new byte[numOfBytes];
							System.arraycopy(file_in_bytes, 0, new_file_in_bytes, 0, numOfBytes);
							packet = new Segment(seqNum, new_file_in_bytes);
						}
						else{
							packet = new Segment(seqNum, file_in_bytes);
						}

						while(queue.isFull()){
							Thread.yield();
						}
						processSend(packet);
						seqNum++;
					}
					while(!queue.isEmpty()){
						Thread.yield();
					}
					outputstream.write(0);
					outputstream.flush();
				}
			}finally{
				outputstream.close();
				inputstream.close();
				file_in_stream.close();
				socket.close();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		catch(NullPointerException e){
			e.printStackTrace();
		}

	}
	public synchronized void processSend(Segment seg) {
		// send seg to the UDP socket
		// add seg to the transmission queue txQueue
		// if txQueue. size () == 1, start the timer
		byte[] segbytes = seg.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(segbytes, segbytes.length, localAddress, this.serverPort);
		try{
			queue.add(seg);

			clientSocket.send(sendPacket);

			if(queue.size() == 1){
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this) , this.rtoTimer);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		catch(IllegalArgumentException e){
			e.printStackTrace();
		}
		catch(InterruptedException e){
			e.printStackTrace();
		}

	}
	public synchronized void processACK(Segment ack) {
		// if ACK not in the current window, do nothing
		// otherwise :
		// cancel the timer
		// while txQueue. element () .getSeqNum() < ack .getSeqNum()
		// txQueue.remove()
		// if not txQueue.isEmpty() , start the timer
		if(queue.element() != null)
		{
		if (ack.getSeqNum() > queue.element().getSeqNum()){

			timer.cancel();
			while(true)
			{
			if(queue.element() == null)
				break;
			if(queue.element().getSeqNum() < ack.getSeqNum()){
				//System.out.println("inside ack");
				try{
					queue.remove();
				}catch (InterruptedException e){
					e.printStackTrace();
				}
			}
			else
				break;
			}

			if (!queue.isEmpty()){
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this), this.rtoTimer);
			}
		}
	}
		else
			System.out.println("No more elements in the queue.");
	}
	public synchronized void processTimeout () {
		// get the list of all pending segments by calling txQueue. toArray ()
		// go through the list and send all segments to the UDP socket
		// if not txQueue.isEmpty() , start the timer
		Segment[] pending_seg = queue.toArray();
		int i = pending_seg.length;
		while(i != 0){

			byte[] segbytes = pending_seg[i-1].getBytes();
			DatagramPacket sendPacket = new DatagramPacket(segbytes, segbytes.length, localAddress, this.serverPort);
			try{
				clientSocket.send(sendPacket);
				if (!queue.isEmpty()){
					timer = new Timer(true);
					timer.schedule(new TimeoutHandler(this), this.rtoTimer);
				}//end if
			}catch(IOException e){
				e.printStackTrace();
			}
			catch(IllegalArgumentException e){
				e.printStackTrace();
			}

			i--; //move to next segment
		}
	}



    /**
     * A simple test driver
     *
     */
	public static void main(String[] args) {
		int windowSize = 10; //segments
		int timeout = 100; // milli-seconds

		String serverName = "localhost";
		String fileName = "";
		int serverPort = 0;

		// check for command line arguments
		if (args.length == 3) {
			// either privide 3 paramaters
			serverName = args[0];
			serverPort = Integer.parseInt(args[1]);
			fileName = args[2];
		}
		else if (args.length == 2) {
			// or just server port and file name
			serverPort = Integer.parseInt(args[0]);
			fileName = args[1];
		}
		else {
			System.out.println("wrong number of arguments, try agaon.");
			System.out.println("usage: java FastFtp server port file");
			System.exit(0);
		}


		FastFtp ftp = new FastFtp(windowSize, timeout);

		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}

}
