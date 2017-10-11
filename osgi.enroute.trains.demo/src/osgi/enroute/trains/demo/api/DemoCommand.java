package osgi.enroute.trains.demo.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out commands to robot
 */
public class DemoCommand extends DTO {
	public final static String TOPIC = "osgi/trains/command/demo";

	public enum Type {
		START,STOP,EMERGENCY
	}

	public Type type;
	public String train;
	public boolean emergency;
	
}
