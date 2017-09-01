package osgi.enroute.trains.track.api;

import java.util.List;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Defines the configuration data for a track
 */
@ObjectClassDefinition
public @interface TrackConfiguration {

	final static public String TRACK_CONFIGURATION_PID = "osgi.enroute.trains.track.manager";

	/**
	 * The segment definitions. The structure is a set of data separated by
	 * colons:
	 * 
	 * {@code name:type:length<mm>:speed<%>:controller:to [, to]} A to that goes
	 * to an alternate of a switch must be appended with a '!'
	 */
	String[]segments();

	/**
	 * A name and RFID code for a train
	 * 
	 * {@code name:rfid}
	 */
	String[]trains();

	/**
	 * The nice name for the configuration.
	 */
	String name();

	class DTO extends org.osgi.dto.DTO {
		/**
		 * The nice name for the configuration.
		 */
		public String name;
		/**
		 * The segment definitions. The structure is a set of data separated by
		 * colons:
		 * 
		 * {@code name:type:length<mm>:speed<%>:controller:to [, to]} A to that
		 * goes to an alternate of a switch must be appended with a '!'
		 */
		public List<String> segments;
		/**
		 * A name and RFID code for a train
		 * 
		 * {@code name:rfid}
		 */
		public List<String> trains;
	}

}
