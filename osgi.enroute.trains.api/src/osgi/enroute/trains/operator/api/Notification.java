package osgi.enroute.trains.operator.api;

import org.osgi.dto.DTO;

/**
 * Event class for notifying generic operator messages
 * i.e. notify any delay or cancellations in the schedule
 */
public class Notification extends DTO {
	public final static String TOPIC = "osgi/trains/notification";

	public String train;
	public String message;
	
}
