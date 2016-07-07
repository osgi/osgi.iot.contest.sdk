package osgi.enroute.trains.stations.api;

import org.osgi.dto.DTO;

/**
 * Event class for notifying person check in at a station
 */
public class CheckIn extends DTO {
	public final static String TOPIC = "osgi/trains/checkin";

	public long timestamp = System.currentTimeMillis();
	public String personId;
	public String station;
	
}
