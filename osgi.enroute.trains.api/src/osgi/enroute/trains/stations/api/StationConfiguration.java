package osgi.enroute.trains.stations.api;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface StationConfiguration {

	final static public String STATION_CONFIGURATION_PID = "osgi.enroute.trains.stations.station";

	/**
	 * The nice name for the station.
	 */
	String name();
	
	/**
	 * The segment id this station is located at
	 */
	String segment();


}
