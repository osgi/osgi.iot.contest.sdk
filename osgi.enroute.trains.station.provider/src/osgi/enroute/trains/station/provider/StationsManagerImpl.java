package osgi.enroute.trains.station.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.trains.passenger.api.Passenger;
import osgi.enroute.trains.passenger.api.Person;
import osgi.enroute.trains.passenger.api.PersonDatabase;
import osgi.enroute.trains.station.provider.StationsManagerImpl.Config;
import osgi.enroute.trains.stations.api.Station;
import osgi.enroute.trains.stations.api.StationObservation;
import osgi.enroute.trains.stations.api.StationObservation.Type;
import osgi.enroute.trains.stations.api.StationsManager;

/**
 * The StationsManager handles the checking in/out of passengers 
 * and the arriving/leaving of trains in stations.
 * 
 * 
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = Config.STATION_CONFIGURATION_PID)
public class StationsManagerImpl implements StationsManager{

    @ObjectClassDefinition
    @interface Config {
    	final static public String STATION_CONFIGURATION_PID = "osgi.enroute.trains.station.manager";

    	/**
    	 * Comma-separated list with station1:segment,station2:segment,...
    	 */
    	String[] stations();
    }
	
	@Reference
	private PersonDatabase personDB;
	
	@Reference
	private EventAdmin ea;
	
	@Reference
	private DTOs dtos;
	
	private Map<String, String> stations = new HashMap<>();
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private Map<String, List<Passenger>> passengersInStation = new HashMap<>();
	private Map<String, List<Passenger>> passengersOnTrain = new HashMap<>();
	
	@Activate
	public void activate(Config c){
		for(String s : c.stations()){
			String[] split = s.split(":");
			stations.put(split[0], split[1]);
			passengersInStation.put(split[0], new ArrayList<>());
		}
	}
	
	@Override
	public List<Station> getStations() {
		return stations.entrySet().stream().map(e -> {
			Station s = new Station();
			s.name = e.getKey();
			s.segment = e.getValue();
			return s;
		}).collect(Collectors.toList());
	}
	
	@Override
	public String getStationSegment(String station) {
		return stations.get(station);
	}
	
	@Override
	public String getStation(String segment){
		return stations.entrySet().stream().filter(e -> e.getValue().equals(segment)).map(e -> e.getKey()).findFirst().get();
	}

	@Override
	public List<Passenger> getPassengersWaiting(String station) {
		try {
			lock.readLock().lock();
			return new ArrayList<Passenger>(passengersInStation.get(station));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<Passenger> getPassengersOnTrain(String train) {
		try {
			lock.readLock().lock();
			if(!passengersOnTrain.containsKey(train)){
				passengersOnTrain.put(train,  new ArrayList<>());
			}
			return new ArrayList<Passenger>(passengersOnTrain.get(train));
		} finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public Passenger checkIn(String personId, String station, String destination) {
		// TODO throw exceptions instead of null return?

		Person person = personDB.getPerson(personId);
		if(person == null){
			System.err.println("Non-existent person tried to check in");
			return null;
		}

		if(!passengersInStation.containsKey(station)){
			System.err.println("Station "+station+" is not managed by this StationsManager");
			return null;
		}
		
		if(!passengersInStation.containsKey(destination)){
			System.err.println("Station "+station+" is not managed by this StationsManager");
			return null;
		}
		
		if(!checkValidPersonLocation(personId, station)){
//			System.err.println("Person "+personId+" cannot be at "+station);
			return null;
		}
		
		
		Passenger p = new Passenger();
		p.person = person;
		p.inStation = station;
		p.destination = destination;
		
		System.out.println(p.person.firstName+" "+p.person.lastName+" checked in at "+station+" to travel to "+destination);
		
		try {
			lock.writeLock().lock();
			List<Passenger> waiting = passengersInStation.get(station);
			waiting.add(p);
			System.out.println("Now "+waiting.size()+" passengers waiting in "+station);
		} finally {
			lock.writeLock().unlock();
		}
		checkIn(personId, station);
		
		return p;
	}
	
	private void checkIn(String personId, String station){
		StationObservation checkIn = new StationObservation();
		checkIn.type = Type.CHECK_IN;
		checkIn.personId = personId;
		checkIn.station = station;
		
		try {
			Event event = new Event(StationObservation.TOPIC, dtos.asMap(checkIn));
			ea.postEvent(event);
		} catch(Exception e){
			System.err.println("Error sending CheckIn Event: "+e.getMessage());
		}
	}

	@Override
	public List<Passenger> leave(String train, String station) {
		// TODO throw exceptions instead of null return?

		if(!checkValidTrainLocation(train, station)){
			System.err.println("Cannot board the train as it is not in the station");
			return null;
		}

		if(!passengersInStation.containsKey(station)){
			System.err.println("Station "+station+" is not managed by this StationsManager");
			return null;
		}
		
		try {
			lock.writeLock().lock();
			List<Passenger> onTrain = passengersOnTrain.get(train);
			List<Passenger> inStation = passengersInStation.get(station);
			
			Iterator<Passenger> it = inStation.iterator();
			while(it.hasNext()){
				Passenger p = it.next();
				// TODO should we check with operator whether this train stops at destination station?! 

				System.out.println(p.person.firstName+" "+p.person.lastName+" boards train "+train+" at station "+station);

				onTrain.add(p);
				it.remove();
			}
			
			return onTrain;
		} finally {
			lock.writeLock().unlock();
			
			try {
				StationObservation o = new StationObservation();
				o.type = Type.DEPARTURE;
				o.train = train;
				o.station = station;
				ea.postEvent(new Event(StationObservation.TOPIC, dtos.asMap(o)));
			} catch (Exception e) {
				System.err.println("Error sending departure event");
			}
		}
	}

	@Override
	public void arrive(String train, String station) {
		// TODO throw exceptions?
		if(!checkValidTrainLocation(train, station)){
			System.err.println("Cannot unboard the train as it is not in the station");
			return;
		}

		if(!passengersInStation.containsKey(station)){
			System.err.println("Station "+station+" is not managed by this StationsManager");
			return;
		}
		
		try {
			StationObservation o = new StationObservation();
			o.type = Type.ARRIVAL;
			o.train = train;
			o.station = station;
			ea.postEvent(new Event(StationObservation.TOPIC, dtos.asMap(o)));
		} catch (Exception e) {
			System.err.println("Error sending arrival event");
		}
		
		try {
			lock.writeLock().lock();
			List<Passenger> onTrain = getPassengersOnTrain(train);
			
			Iterator<Passenger> it = onTrain.iterator();
			while(it.hasNext()){
				Passenger p = it.next();
				
				if(p.destination.equals(station)){
					System.out.println(p.person.firstName+" "+p.person.lastName+" checked out "+station);
					
					checkOut(p.person.id, station);
					it.remove();
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void checkOut(String personId, String station){
		StationObservation checkOut = new StationObservation();
		checkOut.type = Type.CHECK_OUT;
		checkOut.personId = personId;
		checkOut.station = station;
		try {
			Event event = new Event(StationObservation.TOPIC, dtos.asMap(checkOut));
			ea.postEvent(event);
		} catch(Exception e){
			System.err.println("Error sending CheckOut Event: "+e.getMessage());
		}
	}

	private boolean checkValidPersonLocation(String personId, String station){
		try {
			lock.readLock().lock();
			for(List<Passenger> p : passengersInStation.values()){
				if(p.stream().filter(passenger -> passenger.person.id.equals(personId)).findFirst().isPresent())
					return false;
			}
			for(List<Passenger> p : passengersOnTrain.values()){
				if(p.stream().filter(passenger -> passenger.person.id.equals(personId)).findFirst().isPresent())
					return false;
			}			
		} finally {
			lock.readLock().unlock();
		}
		
		return true;
	}
	
	private boolean checkValidTrainLocation(String train, String station){
		// TODO implement a check whether a train is actually in this station
		return true;
	}

}
