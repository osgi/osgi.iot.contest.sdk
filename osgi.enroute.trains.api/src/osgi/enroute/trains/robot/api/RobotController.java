package osgi.enroute.trains.robot.api;

public interface RobotController {

	public boolean load();
	
	public boolean unload();
	
	public void reset();
	
	public void stop();
}
