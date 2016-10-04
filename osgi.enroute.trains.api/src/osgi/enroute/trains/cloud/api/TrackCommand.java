package osgi.enroute.trains.cloud.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out commands to control signals / switches
 */
public class TrackCommand extends DTO {
	public final static String TOPIC = "osgi/trains/track/command";

	public enum Type {
		SIGNAL, SWITCH
	}

	public Type type;
	public String segment;
	public Color signal;
	public Boolean alternate;
	
}
