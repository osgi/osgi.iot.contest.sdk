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
	 * segment name to short code
	 * 
	 * {@code segment:code}
	 */
	String[] segment2code();


	/**
	 * short code to RFID tag uuid
	 * 
	 * {@code code:tag}
	 */
	String[] code2tag();
	
}
