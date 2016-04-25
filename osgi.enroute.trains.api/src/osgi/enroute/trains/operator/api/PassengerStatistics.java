package osgi.enroute.trains.operator.api;

import java.util.HashMap;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Keep statistics for each passenger
 * 
 * @author tverbele
 *
 */
public class PassengerStatistics extends DTO {

	// TODO which statistics do we keep?
	
	// number of train rides
	public int timesTraveled = 0;
	
	// number of "miles" traveled
	public int milesTraveled = 0;
	
	// keep the number of times each station was visited 
	public Map<String, Integer> stationsVisited = new HashMap<String, Integer>();
	
}
