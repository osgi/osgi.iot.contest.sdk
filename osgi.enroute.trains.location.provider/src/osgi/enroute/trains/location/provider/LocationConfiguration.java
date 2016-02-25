package osgi.enroute.trains.location.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Defines the location configuration data
 */
@ObjectClassDefinition
public @interface LocationConfiguration {

	final static public String LOCATION_CONFIGURATION_PID = "osgi.enroute.trains.location.provider";

	String accountName();

	String brokerUrl();

	String username();

	String password();
	
	/**
	 * RFID tag short code and segment name
	 * 
	 * {@code code:segment}
	 */
	String[] code2segment();


	/**
	 * RFID tag uuid and short code for tag
	 * 
	 * {@code tag:code}
	 */
	String[] tag2code();
	
}
