package osgi.enroute.trains.passengers.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import osgi.enroute.trains.stations.api.CheckIn;
import osgi.enroute.trains.stations.api.CheckOut;

@Component(property={"event.topics="+CheckIn.TOPIC,
		"event.topics="+CheckOut.TOPIC})
public class PassengersLogger implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		try {
			switch(event.getTopic()){
			case CheckIn.TOPIC:
				// TODO use Object Converter
				System.out.println("Person "+event.getProperty("personId")+" checked in at "+event.getProperty("station"));
				break;
			case CheckOut.TOPIC:
				// TODO use Object Converter
				System.out.println("Person "+event.getProperty("personId")+" checked out at "+event.getProperty("station"));
				break;
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
