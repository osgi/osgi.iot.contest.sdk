package osgi.enroute.trains.stations.api;

import org.osgi.dto.DTO;

/**
 * Event class for notifying person check in at a station
 */
public class StationObservation extends DTO {

	public final static String TOPIC = "osgi/trains/station";

	public enum Type {
		CHECK_IN, CHECK_OUT
	}

	public long timestamp = System.currentTimeMillis();

	public Type type;
	public String station;
	public String personId;
	
}
