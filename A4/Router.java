
import java.util.*;
import java.net.Socket;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import cpsc441.a4.shared.*;

/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 * A separate process broadcasts routing update messages
 * to directly connected neighbors at regular intervals.
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	2.1
 *
 */
public class Router {
	private int routerId;
	private String serverName;
	private int serverPort;
	private int updateInterval;
	RtnTable table;
	int [] linkcost; 	// linkcost [ i ] is the cost of link to router i 
	int [] nexthop; 	// nexthop[ i ] is the next hop node to reach router i 
	int [][] mincost; 	// mincost[ i ] is the mincost vector of router i
	boolean[] neighbors;
	boolean status;
	ObjectInputStream in;
	ObjectOutputStream out;

    /**
     * Constructor to initialize the rouer instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		
		this.routerId = routerId;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.updateInterval = updateInterval;
	}
	

    /**
     * starts the router 
     * 
     * @return The forwarding table of the router
     * @throws Exception 
     */
	@SuppressWarnings("resource")
	public RtnTable start() throws Exception {
		// to be completed
		try{
			//open a TCP connection to the server 
			Socket sock = new Socket(InetAddress.getByName(this.serverName), this.serverPort);
			// Input output streams
			in = new ObjectInputStream(sock.getInputStream());
			out = new ObjectOutputStream(sock.getOutputStream());
			//send recieve and process HELLO
			//send via output stream
			DvrPacket pkt = new DvrPacket(this.routerId, DvrPacket.SERVER, DvrPacket.HELLO);
			out.writeObject(pkt);
			out.flush();
			System.out.println("send " + pkt.toString());
			
			//receive via input stream
			DvrPacket serverResponse_HELLO = (DvrPacket)in.readObject();

			if(serverResponse_HELLO.type != DvrPacket.HELLO ){
				sock.close();
				throw new Exception("Incorrect packet recieved from Server: NOT HELLO");
				
			}
			else if(serverResponse_HELLO.sourceid != DvrPacket.SERVER){
				sock.close();
				throw new Exception("Packet recieved: NOT FROM SERVER");
			}
			
			linkcost = serverResponse_HELLO.getMinCost();

			//initilize neighbors
			neighbors = new boolean[linkcost.length];
			for (int i = 0; i < linkcost.length; i++) {
				if (i != this.routerId && linkcost[i] != DvrPacket.INFINITY){
					neighbors[i] = true;
				}
			}

			mincost = new int[linkcost.length][linkcost.length];
			//mincost of router is the linkcost
			mincost[this.routerId] = linkcost.clone();
			//mincost = INFINITY COST
			for (int i = 0; i < linkcost.length; i++) {
				if (i != this.routerId) {
					for (int j = 0; j < linkcost.length; j++) {
						mincost[i][j] = DvrPacket.INFINITY;
					}
				}
			}
			
			nexthop = new int[linkcost.length];
			for (int i = 0; i < nexthop.length; i++) {
				
				if (neighbors[i]) {
					nexthop[i] = i;
				} else {
					nexthop[i] = this.routerId;
				}
			}
			
			//update table
			table = new RtnTable(linkcost, nexthop);
			
			// start Timer
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimeoutHandler(this), 0, (long) this.updateInterval);
			
			while(status || in.available() > 0){
				try{
					DvrPacket pkt1 = (DvrPacket) in.readObject();
					if (pkt1.type == DvrPacket.HELLO)
						throw new Exception("Received HELLO when not expected");
					//process ROUTE
					else if (pkt1.type == DvrPacket.ROUTE)
						table = processDvr(pkt1);
					//else if QUIT then calncel the timer and we are not active anymore
					else if (pkt1.type == DvrPacket.QUIT) {
						status = false;
						timer.cancel();
					}
				}catch(Exception e){
					e.printStackTrace();
				}	
			}
			sock.close();
			
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return table;
	}

	
	
    private RtnTable processDvr(DvrPacket pkt) {
		//if packet is from then SERVER update the tables
		if (pkt.sourceid == pkt.SERVER){
			
			linkcost = pkt.mincost;
			neighbors = new boolean[linkcost.length];
			
			for (int i = 0; i < linkcost.length; i++){
				if (i != this.routerId && linkcost[i] != DvrPacket.INFINITY){
					neighbors[i] = true;
				}
			}

		}else{
			mincost[pkt.sourceid] = pkt.mincost;
		} ////otherwise from other router and we update mincost

		//update the forward table
		for (int i = 0; i < nexthop.length; i++){
			int distance;
			if (i == this.routerId){
				distance = 0;
			}
			else if(nexthop[i] == this.routerId){
				distance = DvrPacket.INFINITY;
			}
			else{
				distance = mincost[this.routerId][i];
			}
			//Bellman-Ford algorithm
			for (int j = 0; j < mincost.length; j++){
				if ((mincost[j][i] + linkcost[j]) < distance) {
					distance = mincost[j][i] + linkcost[j];
					nexthop[i] = j;
					mincost[this.routerId][i] = distance;
				}
			}
		}
		RtnTable table1 = new RtnTable(mincost[this.routerId], nexthop);
		return table1;
	}
    
public void resend() {
	try {
		for (int i = 0; i < neighbors.length; i++) {
			if (neighbors[i]){
				out.writeObject(new DvrPacket(this.routerId, i, DvrPacket.ROUTE, mincost[this.routerId]));
			}
		}
	}catch(IOException e){
		e.printStackTrace();
	}
}


	/**
     * A simple test driver
	 * @throws Throwable 
     * 
     */
	public static void main(String[] args) throws Throwable {
		// default parameters
		int routerId = 0;
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000; //milli-seconds
		
		// the router can be run with:
		// i. a single argument: router Id
		// ii. all required arquiments
		if (args.length == 1) {
			routerId = Integer.parseInt(args[0]);
		}
		else if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		}
		else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
			
		// print the parameters
		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);
		
		// start the server
		// the start() method blocks until the router receives a QUIT message
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");
		
		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}

}
