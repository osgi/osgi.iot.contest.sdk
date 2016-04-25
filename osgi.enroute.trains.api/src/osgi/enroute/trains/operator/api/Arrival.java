package osgi.enroute.trains.operator.api;

import org.osgi.dto.DTO;

/**
 * Event class for notifying train arrivals
 */
public class Arrival extends DTO {
	public final static String TOPIC = "osgi/trains/arrival";

	public String train;
	public String station;
	
}
