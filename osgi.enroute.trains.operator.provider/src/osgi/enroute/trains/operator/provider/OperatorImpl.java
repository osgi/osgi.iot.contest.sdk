package osgi.enroute.trains.operator.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.Observation.Type;
import osgi.enroute.trains.cloud.api.TrackForCommand;
import osgi.enroute.trains.event.util.EventToObservation;
import osgi.enroute.trains.operator.api.Schedule;
import osgi.enroute.trains.operator.api.ScheduleEntry;
import osgi.enroute.trains.operator.api.TrainOperator;
import osgi.enroute.trains.operator.provider.OperatorImpl.Config;
import osgi.enroute.trains.passenger.api.Passenger;
import osgi.enroute.trains.stations.api.StationsManager;

/**
 * This simple operator will cycle through the stations every 2 minutes
 * 
 * This component will also run in the cloud so it can just listen to the Events from EventAdmin
 */
@Designate(ocd = Config.class, factory = true)
@Component(
		name = Config.TRAIN_OPERATOR_PID,
		property={"event.topics="+Observation.TOPIC},
		immediate=true)
public class OperatorImpl implements TrainOperator, EventHandler {

    @ObjectClassDefinition
    @interface Config {
        final static public String TRAIN_OPERATOR_PID = "osgi.enroute.trains.operator";

        String name();

        String[] stations();

        String[] trains();
    }
	
	@Reference
	private StationsManager stationsMgr;

	@Reference
	private TrackForCommand tc; 

	@Reference
	private EventAdmin ea;
	
	@Reference
	private DTOs dtos;
	
	private String name;
	private List<String> trains = new ArrayList<>();
	private List<String> stations = new ArrayList<>();
	
	// keep the passengers currently on the trains
	private Map<String, List<Passenger>> passengersOnTrains = new HashMap<>();
	
	// keep the schedule for each train
	private Map<String, Schedule> schedules = new HashMap<>();
	
	@Activate
	void activate(Config config){
		name = config.name();
		
		for(String train : config.trains()){
			trains.add(train);
		}
		
		for(String station : config.stations()){
			stations.add(station);
		}
		
		// create schedule
		for(String train : trains){
			Schedule schedule = new Schedule();
			schedule.train = train;
			schedule.entries = new ArrayList<>();
			// TODO create schedule entries...
			ScheduleEntry e = new ScheduleEntry();
			e.start = stations.get(0);
			e.destination = stations.get(1);
			schedule.entries.add(e);
			schedules.put(train, schedule);
		}
		
		
		// send trains to first scheduled start location
		for(String train : trains){
			String dest = schedules.get(train).entries.get(0).start;
			String segment = stationsMgr.getStationSegment(dest);
			System.out.println("Assign train "+train+" to station "+dest+" (segment "+segment+")");
			tc.assign(train, segment);
		}
		
	}
	
	@Override
	public void handleEvent(Event event) {
		try {
			Observation o = EventToObservation.eventToObservation(event, dtos);
			
			if(o.type == Type.ASSIGNMENT_REACHED){
				String train = o.train;
				
				System.out.println("TRAIN REACHED ASSIGNMENT");
				Schedule schedule = schedules.get(train);
				ScheduleEntry s = schedule.entries.get(0);
				stationsMgr.arrive(train, s.start);
				
				// TODO wait for start time?!
				
				List<Passenger> onBoard = stationsMgr.leave(train, s.start);
				passengersOnTrains.put(train, onBoard);
				String segment = stationsMgr.getStationSegment(s.destination);
				System.out.println("TRAIN NOW HAS "+onBoard.size()+" PASSENGERS ON BOARD");
				System.out.println("ASSIGN TRAIN TO GO TO "+s.destination+" ("+segment+")");
				tc.assign(train, segment);
				
				// Add new schedule entry
				ScheduleEntry last = schedule.entries.get(schedule.entries.size()-1);
				ScheduleEntry extra = new ScheduleEntry();
				// TODO for now just two stations hard coded ... make more generic for n stations
				extra.start = last.destination;
				extra.destination = last.start;
				schedule.entries.add(extra);
				
				// Remove first entry
				schedule.entries.remove(0);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public List<String> getTrains() {
		return trains;
	}

	@Override
	public List<Passenger> getPassengersOnTrain(String train) {
		return Collections.unmodifiableList(passengersOnTrains.get(train));
	}

	@Override
	public Schedule getSchedule(String train) {
		return schedules.get(train);
	}

	@Override
	public List<Schedule> getSchedules() {
		return new ArrayList<>(schedules.values());
	}

}
