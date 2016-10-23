package osgi.enroute.trains.location.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Defines the location configuration data
 */
@ObjectClassDefinition
public @interface LocationConfig {

	final static public String LOCATION_CONFIG_PID = "osgi.enroute.trains.location.provider";

	String brokerUrl();

	String username();

	String password();
	
	/**
	 * short code to RFID tag uuid
	 * 
	 * {@code code:tag}
	 */
	String[] code2tag();
	
}
