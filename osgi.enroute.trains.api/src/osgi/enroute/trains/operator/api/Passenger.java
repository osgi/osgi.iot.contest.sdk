package osgi.enroute.trains.operator.api;

import org.osgi.dto.DTO;

/**
 * Represents a passenger traveling in a train or waiting in a station
 * 
 * @author tverbele
 *
 */
public class Passenger extends DTO {

	public String firstName;
	public String lastName;
	
	// for "networking" applications?
	public String company;
	public String email;
	
	public PassengerStatistics statistics;
	
	// on which train the passenger is currently on?
	public String onTrain = null;
	// in which station the passenger is currently waiting?
	public String inStation = null;
	
}
