package osgi.enroute.trains.stations.api;

import org.osgi.dto.DTO;

/**
 * Event class for notifying person check out at a station
 */
public class CheckOut extends DTO {
	public final static String TOPIC = "osgi/trains/checkout";

	public long timestamp = System.currentTimeMillis();
	public String personId;
	public String station;
	
}
