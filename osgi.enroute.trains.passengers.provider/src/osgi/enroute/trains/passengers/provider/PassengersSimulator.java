package osgi.enroute.trains.passengers.provider;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.passenger.api.Person;
import osgi.enroute.trains.passenger.api.PersonDatabase;
import osgi.enroute.trains.stations.api.Station;
import osgi.enroute.trains.stations.api.StationsManager;

/**
 * Listens for checkins from events
 * 
 * Also randomly selects a persons to check in at a certain station
 * 
 * A person might be selected twice to check in at different stations ... in this case
 * the StationsManager should detect this and refuse these check-ins
 */
@Component(name = "osgi.enroute.trains.passengers", 
	property={"event.topics=osgi/trains/passengers/checkin"},
	immediate=true)
public class PassengersSimulator implements EventHandler {

	@Reference
	private Scheduler scheduler;
	
	@Reference
	private PersonDatabase personDB;
	
	@Reference
	private StationsManager stations;
	
	private Closeable tick;
	private Random r = new Random(System.currentTimeMillis());
	
	@Activate
	void activate() throws Exception{
		this.tick = scheduler.schedule(this::tick, 1000, 10000);
	}

	@Deactivate
	void deactivate() throws IOException {
		this.tick.close();
	}

	void tick() throws Exception {
		// randomly try to check in a passenger at a station
		try {
			Person p = personDB.getPersons().get(r.nextInt(personDB.getPersons().size()));
			
			List<Station> s = stations.getStations();
			int i = r.nextInt(s.size());
			String station = s.get(i).name;
			s.remove(i);
			i = r.nextInt(s.size());
			String destination = s.get(i).name;
			
			stations.checkIn(p.id, station , destination);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(Event event) {
		String personId = event.getProperty("personId").toString();
		String station = event.getProperty("station").toString();
		String destination = event.getProperty("destination").toString();
		
		stations.checkIn(personId, station, destination);
	}
}
