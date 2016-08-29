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
import osgi.enroute.trains.stations.api.Station;
import osgi.enroute.trains.stations.api.StationsManager;

/**
 * This component randomly selects a persons to check in at a certain station
 * 
 * A person might be selected twice to check in at different stations ... in this case
 * the StationsManager should detect this and refuse these check-ins
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
		
		personDB.register("peter.kriens@aqute.biz", "Peter", "Kriens", "aQute", null, null, null);
		personDB.register("tim.verbelen@intec.ugent.be", "Tim", "Verbelen", "iMinds", null,
				"https://secure.gravatar.com/avatar/87f6eb735322e742b15dc1c42e05805f.jpg?d=mm&s=85&r=G",
				"https://www.eclipsecon.org/europe2016/user/2040");
		personDB.register("dereck.baum@paremus.com", "Dereck", "Baum", "Paremus", null,
				"https://www.eclipsecon.org/europe2016/sites/default/files/styles/site_login_profile_thumbnail/public/profile_pictures/picture-3459-1468826823.jpg?itok=L8KEn6tp",
				"https://www.eclipsecon.org/europe2016/user/3459");
		personDB.register("tim.ward@paremus.com", "Tim", "Ward", "Paremus", null, null, null);
		personDB.register("mike.francis@paremus.com", "Mike", "Francis", "Paremus", null, null, null);
		personDB.register("v.arnaudov@prosyst.com", "Venelin", "Arnaudov", "ProSyst", null, null, null);
		personDB.register("walt.bowers@eurotech.com", "Walt", "Bowers", "Eurotech", null, null, null);

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
}
