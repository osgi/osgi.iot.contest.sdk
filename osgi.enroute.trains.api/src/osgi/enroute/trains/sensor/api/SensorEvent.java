package osgi.enroute.trains.sensor.api;

import org.osgi.dto.DTO;

public class SensorEvent extends DTO{

	public final static String TOPIC = "osgi/trains/sensor/*";

	public enum Type {
		WATER, 
		LIGHT,
		MOTION,
		DOOR
	}
	
	public Type type;
	public String segment;
	public String train;
	
	public boolean water;
	public boolean dark;
	public boolean motion;
	public boolean open;
}
