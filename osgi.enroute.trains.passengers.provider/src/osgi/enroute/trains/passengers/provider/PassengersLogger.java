package osgi.enroute.trains.passengers.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import osgi.enroute.trains.stations.api.StationObservation;

@Component(property={"event.topics="+StationObservation.TOPIC})
public class PassengersLogger implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		try {			
			System.out.println("Person "+event.getProperty("personId")+" "+event.getProperty("type").toString().toLowerCase().replace('_', ' ')+" at "+event.getProperty("station"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
