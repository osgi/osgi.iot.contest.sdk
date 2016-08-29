package osgi.enroute.trains.stations.api;

import org.osgi.dto.DTO;

public class Station extends DTO {

	/**
	 * Name of the station
	 */
	public String name;
	
	/**
	 * Segment the station is located on
	 */
	public String segment;
}
