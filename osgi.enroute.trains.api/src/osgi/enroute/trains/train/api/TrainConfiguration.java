package osgi.enroute.trains.train.api;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface TrainConfiguration {

	final static public String TRAIN_CONFIGURATION_PID = "osgi.enroute.trains.train.manager";

	/**
	 * The nice name for the train.
	 */
	String name();
	
	/**
	 * The rfid tag that this train has
	 */
	String rfid();

	/**
	 * Target filter for finding the right Train Controller
	 */
	String target_TrainController();
	
}
