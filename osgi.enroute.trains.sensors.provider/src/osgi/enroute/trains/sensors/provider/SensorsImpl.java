package osgi.enroute.trains.sensors.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.trains.cloud.api.TrackForCommand;
import osgi.enroute.trains.event.util.EventConvertor;
import osgi.enroute.trains.sensor.api.SensorEvent;

/**
 * 
 */
@Component(name = "osgi.enroute.trains.sensors",
	property={"event.topics="+SensorEvent.TOPIC},
	immediate=true)
public class SensorsImpl implements EventHandler {

	@Reference
	DTOs dtos;
	
	@Reference
	TrackForCommand trains;
	
	@Override
	public void handleEvent(Event event) {
		try {
			SensorEvent e = EventConvertor.eventToSensorEvent(event, dtos);
			
			switch(e.type){
			case WATER:
				trains.blocked(e.segment, "Flooding", e.water);
				break;
			case LIGHT:
				trains.dark(e.segment, e.dark);
				break;
			case MOTION:
				trains.emergency(e.train, "Emergency brake", e.motion);
				break;
			case DOOR:
				trains.emergency(e.train, "Open door", e.open);
				break;
			case PASSENGER:
				// Brake the train
				trains.emergency(e.train, "Passenger Emergency Brake", e.passenger);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
