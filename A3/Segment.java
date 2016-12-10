
import java.util.*;
import java.net.*;

/**
 * Segment Class
 * 
 * Segment defines the structure of the packets used for 
 * exchanging data/feedback between FastFtp client and server.
 *
 * Each segment has a sequence number and a payload.
 * If the segment carries data then the payload contains application data.
 * If the segment is an ACK segment from the server, then payload is empty.
 *
 * The max size of the payload is given by MAX_PAYLOAD_SIZE
 * The max segment size is given by MAX_SEGMENT_SIZE 
 * 
 * @author 	Majid Ghaderi
 * @version	2.2
 *
 */
public class Segment {

	private final static int HEADER_SIZE = 4; // bytes
	
	public final static int MAX_PAYLOAD_SIZE = 1000; // bytes
	public final static int MAX_SEGMENT_SIZE = HEADER_SIZE + MAX_PAYLOAD_SIZE; // bytes
	
	// header fields
	private int seqNum;	// segment sequence number
	
	// segment payload, it could be of 0 length
	// Ack segments have no payload
	private byte[] payload;
	
	
    /**
     * No-argument constructor 
     * 
	 * Constructs a segment with seqNum 0 and paylod of 0 length.
     */
	public Segment() {
		this(0, new byte[0]);
	}

	
    /**
     * Constructor 
     * 
	 * Constructs a segment with the given seqNum and paylod of 0 length.
	 *
     * @param seqNum	Sequence number for this segment
     */
	public Segment(int seqNum) {
		this(seqNum, new byte[0]);
	}

	
    /**
     * Constructor 
     * 
	 * Constructs a segment with the given seqNum and paylod.
	 *
     * @param seqNum	Sequence number for this segment
     * @param payload	A byte array to set the payload of the segment
     * 
     * @throws IllegalArgumentException If the seqNum is negative
     */
	public Segment(int seqNum, byte[] payload) {
		setSeqNum(seqNum);
		setPayload(payload);
	}

	
    /**
     * Copy Constructor 
     * 
     * Creates and independent copy of the given argument.
     * 
     * @param seg	The segment to be copied
     */
	public Segment(Segment seg) {
		this(seg.seqNum, seg.payload);
	}
	
	
    /**
     * Constructor 
     * 
     * Creates a segment using the given byte array to reconstruct the segment.
     * It uses the byte array to reconstruct both the header and payload of the segment.
     * 
     * @param data	a byte array to set the header and payload of the segment
     */
	public Segment(byte[] bytes) {
		setBytes(bytes);
	}

	
    /**
     * Constructor 
     * 
     * Creates a segment using the payload of the given DatagramPacket.
     * It uses the data in the packet to constructs both the header and payload of the segment.
     * 
     * 
     * @param packet	The data payload of the packet is used to initialize the segment
     */
	public Segment(DatagramPacket packet) {
		this(Arrays.copyOf(packet.getData(), packet.getLength()));
	}
	
	
    /**
     * Sets the payload of the segment  
     */
	public void setPayload(byte[] data) {
		// cannot be larger than the max size
		if (data.length > MAX_PAYLOAD_SIZE)
			throw new IllegalArgumentException("Payload is too large");
		
		// copy payload
		payload = Arrays.copyOf(data, data.length);
	}
	
	
    /**
     * Returns the payload of the segment in a byte array. 
     */
	public byte[] getPayload() {
		return payload;
	}
	
	
    /**
     * Returns the length of the "payload" of the segment.
     * Note: The return value does not cinclude the length of the header. 
     */
	public int getLength() {
		return payload.length;
	}
	
	
    /**
     * Returns the sequence number 
     */
	public int getSeqNum() {
		return seqNum;
	}
	
	
    /**
     * Sets the sequence number 
     */
	public void setSeqNum(int seqNum) {
		if (seqNum < 0)
			throw new IllegalArgumentException("Negative sequence number");
		
		this.seqNum = seqNum; 
	}

	
    /**
     * Returns a string representation of the segment 
     * 
     * @return The string representation of the segment
     */
	public String toString() {
		return ("Seq#" + seqNum + "\n" + Arrays.toString(payload)); 
	}
	
	
    /**
     * Returns the entire segment as a byte array.
     * The byte array contains both the header and the payload of the segment.
     * Useful when creating a DatagramPacket to encapsulate a segment.  
     * 
     * @return A byte array containing the entire segment
     */
	public byte[] getBytes() {
		byte[] bytes = new byte[HEADER_SIZE + payload.length];
		
		// store sequence number field 
		bytes[0] = (byte) (seqNum);
		bytes[1] = (byte) (seqNum >>> 8);
		bytes[2] = (byte) (seqNum >>> 16);
		bytes[3] = (byte) (seqNum >>> 24);
		
		// store the payload
		System.arraycopy(payload, 0, bytes, HEADER_SIZE, payload.length);
		
		return bytes;
	}

	
    /**
     * Sets the content of a segment using the given byte array.
     * It reconstructs both the header and payload of the segment.
     * Useful when de-encapulating a received DatagramPacket to a segment. 
     * 
     * @param bytes The byte array used to set the header+payload of the segment
     * 
     * @throws IllegalArgumentException If the bytes array is too short to even recover the header 
     */
	public void setBytes(byte[] bytes) {
		// the header is REQUIRED
		if (bytes.length < HEADER_SIZE)
			throw new IllegalArgumentException("Segment header missing");
		
		// cannot be larger than the max size
		if (bytes.length > MAX_SEGMENT_SIZE)
			throw new IllegalArgumentException("Payload is too large");
		
		// construct the header fields
		int b0 = bytes[0] & 0xFF;
		int b1 = bytes[1] & 0xFF;
		int b2 = bytes[2] & 0xFF;
		int b3 = bytes[3] & 0xFF;
		seqNum = (b3 << 24) + (b2 << 16) + (b1 << 8) + (b0);

		
		// copy payload data
		payload = new byte[bytes.length - HEADER_SIZE];
		System.arraycopy(bytes, HEADER_SIZE, payload, 0, payload.length);
	}
	
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		
		byte[] payload = new byte[MAX_PAYLOAD_SIZE];
		
		Arrays.fill(payload, (byte) 0);
		payload[0] = 1;
		payload[MAX_PAYLOAD_SIZE - 1] = 1;
		
		// creating a segment with the payload and seqNum 1
		Segment seg1 = new Segment(1, payload);
		
		// display the segment
		System.out.println("seg1");
		System.out.println(seg1);
		System.out.println();
		
		// create a default segment
		Segment seg2 = new Segment();
		
		// set header+payload of seg2 equal to header+payload of seg1
		// essentially creating a copy of seg1
		seg2.setBytes(seg1.getBytes());
		
		// display the segment
		System.out.println("seg2");
		System.out.println(seg2);
		System.out.println();
		
		// create a DatagramPacket that can be used to send seg2
		byte[] data = seg2.getBytes();
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		
		// create a segment based on the paylaod in a received DatagramPacket
		Segment seg3 = new Segment(pkt);
		
		// display the segment
		System.out.println("seg3");
		System.out.println(seg3);
		System.out.println();
	}
}
