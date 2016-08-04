package osgi.enroute.trains.operator.api;

import java.util.List;

import osgi.enroute.trains.passenger.api.Passenger;

/**
 * A Train Operator will manage his fleet of trains to visit a number of stations and hence transport people
 * 
 * Passengers can check in in a station via the StationsManager. If an operator has a train arriving
 * in a station it can pick up passengers by calling the board method of the StationManager. 
 * 
 * The Train Operator will also post Arrival and Departure events when trains arrive/depart in/from a station
 * 
 * The Train Operator is responsible for choosing the schedule in which trains visit stations. 
 * It uses the Command events to interact with the TrackManager 
 *  
 * @author tverbele
 *
 */
public interface TrainOperator {

	/**
	 * List all trains of this operator
	 * @return
	 */
	List<String> getTrains();
	
	/**
	 * List all passengers currently traveling on the train
	 * @param train
	 * @return
	 */
	List<Passenger> getPassengersOnTrain(String train);
	
	/**
	 * Get the schedule planned for a train
	 * @param train
	 * @return
	 */
	Schedule getSchedule(String train);
	
	/**
	 * Get schedules for all trains
	 * @return
	 */
	List<Schedule> getSchedules();

}
