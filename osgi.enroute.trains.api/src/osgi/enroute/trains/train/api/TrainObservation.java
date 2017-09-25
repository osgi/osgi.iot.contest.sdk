package osgi.enroute.trains.train.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out information about what's happening
 */
public class TrainObservation extends DTO {
	public final static String TOPIC = "osgi/trains/observation/train";

	public enum Type {
		/**
		 * Train starts moving
		 */
		MOVING, 
		/**
		 * Train stopped
		 */
		STOPPED,
		/**
		 * Lights switched
		 */
		LIGHT
	}

	public Type type;
	public String train;
	public long time;
	public int directionAndSpeed;
	public boolean on;
}
