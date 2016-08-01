package osgi.enroute.trains.operator.api;

import org.osgi.dto.DTO;

public class ScheduleEntry extends DTO {

	public String start;
	public long departureTime;
	
	public String destination;
	public long arrivalTime;
	
}
