package osgi.enroute.trains.cloud.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out information about what's happening
 */
public class Observation extends DTO {
	public final static String TOPIC = "osgi/trains/observation";

	public enum Type {
		CHANGE, //
		/**
		 * Detected an RFID
		 */
		LOCATED, 
		/**
		 * Assignment changed
		 */
		ASSIGNMENT, 
		/**
		 * Assignment reached
		 */
		ASSIGNMENT_REACHED,
		/**
		 * Signal changed color
		 */
		SIGNAL,
		/**
		 * Switched changed alternate state
		 */
		SWITCH,
		/**
		 * A segment is blocked 
		 */
		BLOCKED,
		/**
		 * A segment is dark
		 */
		DARK,
		/**
		 * A train emergency occurs
		 */
		EMERGENCY
		
	}

	public Type type;
	public String segment;
	public String train;
	public Color signal;
	public String assignment;
	public long time;
	public long id;
	public String message;
	public boolean alternate;
	public boolean blocked;
	public boolean dark;
	public boolean emergency;
	public double speed;
}
