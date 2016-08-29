package osgi.enroute.trains.stations.api;

import java.util.List;

import osgi.enroute.trains.passenger.api.Passenger;

/**
 * StationsManager provides an API for managing the stations and people traveling from 
 * Station A to B. A person can check in at a station with a planned destination 
 * by calling checkIn(). 
 * 
 * TrainOperators that have a train arriving at a station can call unboard/board to 
 * let passengers hop off/on the train.
 * 
 * The StationsManager will publish CheckIn/CheckOut events when a person checks in/out
 * of a station.
 * 
 * The StationsManager should also update the PassengersStatistics as persons visit stations. 
 * 
 * @author tverbele
 *
 */
public interface StationsManager {

	/**
	 * List all known stations
	 * @return
	 */
	List<Station> getStations();
	
	/**
	 * Return the segment for this station
	 * @param station
	 * @return
	 */
	String getStationSegment(String station);

	/**
	 * Return the station for this segment
	 */
	String getStation(String segment);
	
	/**
	 * List the passengers currently waiting in a station
	 * @param station
	 * @return
	 */
	List<Passenger> getPassengersWaiting(String station);
	
	/**
	 * Person checks in at a station with a certain destination
	 * @param person
	 * @param station
	 * @param destination
	 */
	Passenger checkIn(String personId, String station, String destination);

	/**
	 * Train calls unboard to let people hop off the train
	 * @param train
	 * @param station
	 */
	// rename to arrive
	void arrive(String train, String station);
	
	/**
	 * Train boards passengers at a given station
	 * @param train
	 * @param station
	 * @return the list of passengers that boarded the train (includes passengers that are still on the train)
	 */
	List<Passenger> leave(String train, String station);
	
}
