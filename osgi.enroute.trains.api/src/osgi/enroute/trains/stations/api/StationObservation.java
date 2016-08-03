package osgi.enroute.trains.stations.api;

import org.osgi.dto.DTO;

/**
 * Event class for events about trains/passengers arriving/leaving in stationss
 */
public class StationObservation extends DTO {

	public final static String TOPIC = "osgi/trains/station";

	public enum Type {
		/**
		 * A passenger checked in at a station
		 */
		CHECK_IN, 
		
		/**
		 * A passenger checked out in a station
		 */
		CHECK_OUT, 
		
		/**
		 * A train arrived at a station
		 */
		ARRIVAL, 
		
		/**
		 * A train departed at a station
		 */
		DEPARTURE, 
		
		/**
		 * Any notification an operator sends out to stations/passengers 
		 * (e.g. a train is delayed)
		 */
		NOTIFICATION
	}

	public Type type;
	public String station;
	public String personId;
	public String train;
	public String message;
	
}
