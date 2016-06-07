package osgi.enroute.trains.operator.api;

import org.osgi.dto.DTO;

/**
 * Event class for notifying train departures
 */
public class Departure extends DTO {
	public final static String TOPIC = "osgi/trains/departure";

	public String train;
	public String station;
	
}
