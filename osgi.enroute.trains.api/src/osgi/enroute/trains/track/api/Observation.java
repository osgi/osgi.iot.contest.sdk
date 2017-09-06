package osgi.enroute.trains.track.api;

import org.osgi.dto.DTO;

import osgi.enroute.trains.segment.api.Color;

/**
 * Event class for sending out information about what's happening
 */
public class Observation extends DTO {
	public final static String TOPIC = "osgi/trains/observation";

	public enum Type {
		/**
		 * Detected an RFID
		 */
		LOCATED, 
		/**
		 * Signal changed color
		 */
		SIGNAL,
		/**
		 * Switched changed alternate state
		 */
		SWITCH,
		/**
		 * Blocking a segment
		 */
		BLOCKED
	}

	public Type type;
	public String segment;
	public String train;
	public Color signal;
	public long time;
	public boolean alternate;
	public boolean blocked;
}
