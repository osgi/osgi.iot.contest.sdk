package osgi.enroute.trains.location.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface LocationProviderConfig {

	/**
	 * train that we locate
	 * @return
	 */
	String train();
	
	/**
	 * bluetooth device MAC address
	 * @return
	 */
	String mac();
	
	/**
	 * bluetooth rfcomm channel
	 * @return
	 */
	int channel();
	
	/**
	 * rfcomm device to use
	 * @return
	 */
	String rfcomm() default "/dev/rfcomm0";
	
	/**
	 * short code to RFID tag uuid
	 * 
	 * {@code code:tag}
	 */
	String[] code2tag();
	
}