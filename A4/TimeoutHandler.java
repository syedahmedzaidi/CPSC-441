import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {

	private Router router;
	public TimeoutHandler(Router router){
		this.router = router;
	}
	@Override
	public void run() {
		try {
			if (router.status){
				router.resend();
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}

}
