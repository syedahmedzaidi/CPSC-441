import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
public class ReceiverThread extends Thread {
	
	private DatagramSocket socket = null;
	FastFtp fastFtp;
	boolean status = false;
	// define constructor
	public ReceiverThread(FastFtp fastFtp, DatagramSocket socket, boolean status) {
		
		this.fastFtp = fastFtp;	
		this.socket = socket;
		this.status = status;
	}
	// rhe run method 
	public void run() { 
	// while not terminated : 
	// 1. receive a DatagramPacket pkt from UDP socket 
	// 2. call processAck(new Segment(pkt) ) in the parent process
		while(this.status){
			byte[] pkt_in_bytes = new byte[Segment.MAX_PAYLOAD_SIZE];
			try{
				DatagramPacket receive_pkt = new DatagramPacket(pkt_in_bytes, pkt_in_bytes.length);
				socket.receive(receive_pkt);
				fastFtp.processACK(new Segment(receive_pkt));
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}