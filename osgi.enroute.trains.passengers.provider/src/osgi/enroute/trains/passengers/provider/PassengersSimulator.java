package osgi.enroute.trains.passengers.provider;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.passenger.api.Person;
import osgi.enroute.trains.passenger.api.PersonDatabase;
import osgi.enroute.trains.stations.api.StationsManager;

/**
 * 
 */
@Component(name = "osgi.enroute.trains.passengers", immediate=true)
public class PassengersSimulator {

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
		
		personDB.register("Peter", "Kriens", "aQute", "peter.kriens@aqute.biz", null);
		personDB.register("Tim", "Verbelen", "iMinds", "tim.verbelen@intec.ugent.be", null);
		personDB.register("Dereck", "Baum", "Paremus", "dereck.baum@paremus.com", null);
		personDB.register("Tim", "Ward", "Paremus", "tim.ward@paremus.com", null);
		personDB.register("Mike", "Francis", "Paremus", "mike.francis@paremus.com", null);
		personDB.register("Venelin", "Arnaudov", "ProSyst", "v.arnaudov@prosyst.com", null);
		personDB.register("Walt", "Bowers", "Eurotech", "walt.bowers@eurotech.com", null);

	}

	@Deactivate
	void deactivate() throws IOException {
		this.tick.close();
	}

	void tick() throws Exception {
		// randomly try to check in a passenger at a station
		try {
			Person p = personDB.getPersons().get(r.nextInt(personDB.getPersons().size()));
			
			List<String> s = stations.getStations();
			int i = r.nextInt(s.size());
			String station = s.get(i);
			s.remove(i);
			i = r.nextInt(s.size());
			String destination = s.get(i);
			
			stations.checkIn(p.id, station , destination);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
