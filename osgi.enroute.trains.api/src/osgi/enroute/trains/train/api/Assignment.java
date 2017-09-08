package osgi.enroute.trains.train.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out information about train assignments
 */
public class Assignment extends DTO {
	public final static String TOPIC = "osgi/trains/assignment";

	public enum Type {
		/**
		 * Give a new assignment
		 */
		ASSIGN, 
		/**
		 * Notify of assignment reached
		 */
		REACHED,
		/**
		 * Notify of assignment aborted, e.g. could not find route or get access
		 */
		ABORTED
	}

	public Type type;
	public String segment;
	public String train;
	
}
