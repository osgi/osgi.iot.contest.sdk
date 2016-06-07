package osgi.enroute.trains.passenger.api;

/**
 * Represents a passenger traveling in a train or waiting in a station
 * 
 * @author tverbele
 *
 */
public class Passenger extends Person {

	// on which train the passenger is currently on?
	public String onTrain = null;
	// in which station the passenger is currently waiting?
	public String inStation = null;
	// passenger destination
	public String destination = null;
	
}
