package osgi.enroute.trains.operator.api;

import java.util.List;

import org.osgi.util.promise.Promise;

/**
 * A Train Operator will manage his fleet of trains to visit a number of stations and hence transport people
 * 
 * Passengers can check in in a Station. Then the Train Operator will make sure they are picked
 * up by the next train and travel to the next train stop. 
 * 
 * The Train Operator will also post Arrival and Departure events when trains arrive/depart in/from a station
 * 
 * The Train Operator is responsible for choosing the scheme in which trains visit stations. 
 * It uses the Command events to interact with the TrackManager 
 *  
 * @author tverbele
 *
 */
public interface TrainOperator {

	/**
	 * List all passengers of this operator
	 * @return
	 */
	List<Passenger> getPassengers();
	
	/**
	 * List all passengers currently waiting in a station
	 * @param station
	 * @return
	 */
	List<Passenger> getWaitingPassengers(String station);
	
	/**
	 * List all passengers currently traveling on the train
	 * @param train
	 * @return
	 */
	List<Passenger> getPassengersOnTrain(String train);
	
	/**
	 * List all stations this operator is operating
	 * @return
	 */
	List<String> getStations();
	
	/**
	 * A passenger checks in at a station and starts waiting for a train
	 * 
	 * The promise gets resolved when a train picks up the passenger. The promise 
	 * is resolved with the id of the train
	 * @param p
	 * @param station
	 * @return
	 */
	// TODO use a passenger ID instead?
	Promise<String> checkIn(Passenger p, String station);
	
	
}
