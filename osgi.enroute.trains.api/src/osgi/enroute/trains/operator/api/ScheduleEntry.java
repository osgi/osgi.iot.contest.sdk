package osgi.enroute.trains.operator.api;

import java.util.Date;

import org.osgi.dto.DTO;

public class ScheduleEntry extends DTO {

	public String start;
	public Date departureTime;
	public String destination;
	public Date arrivalTime;
	
}
