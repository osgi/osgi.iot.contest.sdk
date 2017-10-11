package osgi.enroute.trains.demo.api;

import org.osgi.dto.DTO;

/**
 * Observation class for sending out information about what's happening
 */
public class DemoObservation extends DTO {
	public final static String TOPIC = "osgi/trains/observation/demo";
	
	public enum Type {TRAIN_STARTED, TRAIN_STOPPED, MESSAGE}
	
	public Type type;
	public String train;
	
	public String message;
	public long time;
}
