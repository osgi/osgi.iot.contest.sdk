package osgi.enroute.trains.demo;

import java.util.concurrent.ScheduledFuture;

public class Train {

	public enum State {RUNNING, STOPPING, STOPPED};
	
	public String name;

	public String assignment;

	public State state;
	
	public ScheduledFuture schedule;
}
