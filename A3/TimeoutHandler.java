import java.util.TimerTask;

class TimeoutHandler extends TimerTask {
	private FastFtp fastFtp;
	// define constructor
	public TimeoutHandler(FastFtp fastFtp){
		this.fastFtp = fastFtp;
	}
	// rhe run method 
	public void run() { // call processTimeout () in the main class 
		this.fastFtp.processTimeout();
	}
}