package osgi.enroute.trains.operator.api;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface StationConfiguration {

	final static public String STATION_CONFIGURATION_PID = "osgi.enroute.trains.operator.station";

	/**
	 * The nice name for the station.
	 */
	String name();
	
	/**
	 * The segment id this station is located at
	 */
	String segment();


}
