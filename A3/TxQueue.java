
import java.util.concurrent.locks.*;

/**
 * TxQueue Class
 * 
 * TxQueue implements a first-in first-out queue of segments using a circular array.
 * Items are added at the 'tail' of the queue, while items are removed from the 'head' of the queue.
 * The next segment to be removed is at the 'head'.
 * 
 * This is a blocking implementation:
 * The queue has a capacity. A call to add() when the queue is full
 * blocks the calling process until space becomes available by calling remove().
 * A call to remove() when the queue is empty will block the calling process until
 * a segment is added to the queue using add(). 
 * 
 * @author 	Majid Ghaderi
 * @version	3.1
 *
 */
public class TxQueue {
	
	// used for mutual exclusion, can be unlocked only by the locking process
	private final ReentrantLock mutex;

	// conditions used for capacity management
	private final Condition notFull;
	private final Condition notEmpty;
	
	// queue ADT variables to implement a circular array
	private Segment[] queue = null;
	private int head = 0;
	private int tail = -1;
	private int count = 0;

	
    /**
     * Constructor 
     * 
     * Creates a queue with the given capacity.
     * 
     * @param capacity	The capacity of the queue
     */
	public TxQueue(int capacity) {
		mutex = new ReentrantLock();
		
		notFull = mutex.newCondition();
		notEmpty = mutex.newCondition();
		
		queue = new Segment[capacity];
	}
	
	
    /**
     * Returns an array of segments in the queue.
     * This array can be used to iterate over the segments in the queue. 
     * 
     * @return An array contaning all queue segments
     */
	public Segment[] toArray() {
		// prevents others from accessing queue
		mutex.lock();
		
		try {
			Segment[] temp = new Segment[count];
			for (int i = 0; i < count; i++) 
				temp[i] = queue[(i + head) % queue.length];
			
			return temp;
		}
		finally {
			// release the lock
			mutex.unlock();
		}
	}

	
    /**
     * Returns the segment at the 'head' of the queue, but does not remove it.  
     * 
     * @return	The segment at the front of the queue.
     * 			If queue is empty, returns null
     */
	public Segment element() {
		// prevents others from accessing queue
		mutex.lock();
		
		try {
			Segment seg = null;
			if (count != 0)
				seg = queue[head];
			
			return seg;
		}
		finally {
			// release the lock
			mutex.unlock();
		}
	}

	
    /**
     * Adds a segment at the 'tail' of the queue if there is any space available,
     * otherwise, the calling process is blokced until space becomes available.
     * 
     * @param seg	The segment to be added to the queue
     * @throws InterruptedException in case the thread excecution is interrupted
     */
	public void add(Segment seg) throws InterruptedException {
		// prevents others from accessing queue
		mutex.lock();
		
		try {
			// wait for space to become available in queue
			if (count == queue.length)
				notFull.await();
			
			// add the segment at the tail of the queue
			tail = (tail + 1) % queue.length;
			queue[tail] = seg;
			count++;
			
			// queue is not empty anymore
			notEmpty.signal();
		}
		finally {
			// release the lock
			mutex.unlock();
		}
	}

	
    /**
     * Removes and returns the segment at the 'head' of the queue if the queue is not empty,
     * otheriwse, will block the calling process until a segment becomes available. 
     * 
     * @return 	The segment at the head of the queue
     * @throws InterruptedException in case the thread excecution is interrupted
     */
	public Segment remove() throws InterruptedException {
		// prevents others from accessing queue
		mutex.lock();

		
		try {
			// wait for items to be added to queue
			if (count == 0)
				notEmpty.await();
			
			// remove the head of the queue and return it
			Segment seg = queue[head];
			head = (head + 1) % queue.length;
			count--;
			
			// queue is not full anymore
			notFull.signal();
			
			return seg;
		}
		finally {
			// release the lock
			mutex.unlock();
		}
	}

	
    /**
     * Returns the number of segments in the queue.
     * 
     * @return 	The number of segments in the queue
     */
	public int size() {
		// prevents others from accessing queue
		mutex.lock();
		
		try {
			// size = capacity - occupied
			return count;
		}
		finally {
			// release the lock
			mutex.unlock();
		}
	}

	
    /**
     * Checks if the queue is empty.
     * 
     * @return 	true if the queue is empty, false otherwise
     */
	public boolean isEmpty() {
		// prevents others from accessing queue
		mutex.lock();
		
		try {
			return (count == 0);
		}
		finally {
			// release the lock
			mutex.unlock();
		}
	}

	
    /**
     * Checks if the queue is full.
     * 
     * @return 	true if the queue is full, false otherwise
     */
	public boolean isFull() {
		// prevents others from accessing queue
		mutex.lock();
		
		try {
			return (count == queue.length);
		}
		finally {
			// release the lock
			mutex.unlock();
		}
	}
	
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		
		// a queue with capacity of 10 segments
		TxQueue q = new TxQueue(10);
		
		try {
			// create the segements with consequitive seq numbers
			// add the segments to the queue
			for (int i = 0; i < 10; i++)
				q.add(new Segment(i));

			// get all segments from the queue
			Segment[] segments = q.toArray();
			
			// iterate over the segments
			for (int i = 0; i < segments.length; i++)
				System.out.println(segments[i].getSeqNum());
			
			// remove half of the segments from the queue
			for (int i = 0; i < segments.length / 2; i++)
				q.remove();
	
			// get the lisyt of the remaing segments
			System.out.print("toArray(): ");
			segments = q.toArray();
			System.out.println("length = " + segments.length);
			
			// iterate over them and remove as many segments from the queue
			for (int i = 0; i < segments.length; i++) {
				System.out.println(segments[i]);
				q.remove();
			}
			
			// queue should be empty now
			if (q.isEmpty())
				System.out.println("queue is empty");
			
			System.out.println("Next call to remove would suspend the program");
			System.out.println("Use Ctrl+C to terminate...");
			q.remove();
		}
		catch (InterruptedException e) {
			// this is OK
			System.out.println("Program is interrupted, exiting.");
		}
	}
	
}
