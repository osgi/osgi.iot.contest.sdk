package osgi.enroute.trains.demo;

import org.osgi.dto.DTO;

/**
 * Event class for sending out information about what's happening
 */
public class DemoEvent extends DTO {
	public final static String TOPIC = "osgi/trains/demo";

	public String message;
	public long time;
}
