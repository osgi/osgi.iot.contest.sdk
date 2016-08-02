package osgi.enroute.trains.stations.api;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface StationConfiguration {

	final static public String STATION_CONFIGURATION_PID = "osgi.enroute.trains.station.manager";

	/**
	 * Comma-separated list with station1:segment,station2:segment,...
	 */
	String[] stations();

}
