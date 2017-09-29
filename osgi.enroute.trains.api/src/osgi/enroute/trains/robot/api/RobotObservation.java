package osgi.enroute.trains.robot.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out information about what's happening
 */
public class RobotObservation extends DTO {
	public final static String TOPIC = "osgi/trains/observation/robot";

	public enum Type {
		/**
		 * Robot loaded container onto a train
		 */
		LOADED, 
		/**
		 * Robot unloaded container from a train
		 */
		UNLOADED
	}

	public Type type;
	public boolean succes;
}
