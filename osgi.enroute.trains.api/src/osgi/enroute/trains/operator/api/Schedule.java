package osgi.enroute.trains.operator.api;

import java.util.Date;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * Schedule for a train
 */
public class Schedule extends DTO {
	
	public String train;
	public List<String> stations;
	public List<Date> arrivalTimes;
	public List<Date> departureTimes;
	
}
