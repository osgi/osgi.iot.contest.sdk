package osgi.enroute.trains.robot.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out commands to robot
 */
public class RobotCommand extends DTO {
	public final static String TOPIC = "osgi/trains/command/robot";

	public enum Type {
		LOAD, UNLOAD, RESET
	}

	public Type type;
	
}
