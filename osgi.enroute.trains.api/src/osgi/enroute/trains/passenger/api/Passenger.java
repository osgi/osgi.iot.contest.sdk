package osgi.enroute.trains.passenger.api;

import org.osgi.dto.DTO;

/**
 * Represents a passenger traveling in a train or waiting in a station
 * 
 * @author tverbele
 *
 */
public class Passenger extends DTO {

	// which person 
	public Person person;
	
	// on which train the passenger is currently on?
	public String onTrain = null;
	
	// in which station the passenger is currently waiting?
	public String inStation = null;
	
	// passenger destination
	public String destination = null;
	
}
