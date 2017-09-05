package osgi.enroute.trains.train.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out commands to train
 */
public class TrainCommand extends DTO {
	public final static String TOPIC = "osgi/trains/command/train";

	public enum Type {
		MOVE, LIGHT
	}

	public Type type;
	public String train;
	public int directionAndSpeed;
	public boolean on;
	
}
