package osgi.enroute.trains.segment.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out commands to control signals / switches
 */
public class SegmentCommand extends DTO {
	public final static String TOPIC = "osgi/trains/command/segment";

	public enum Type {
		SIGNAL, SWITCH
	}

	public Type type;
	public String segment;
	public Color signal;
	public Boolean alternate;
	
}
