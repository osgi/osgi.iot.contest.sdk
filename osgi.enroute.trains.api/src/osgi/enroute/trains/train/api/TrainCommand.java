package osgi.enroute.trains.train.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out commands to train
 */
public class TrainCommand extends DTO {
	public final static String TOPIC = "osgi/trains/train/command";

	public enum Type {
		MOVE, LIGHT
	}

	public Type type;
	public int directionAndSpeed;
	public boolean on;
	
}
