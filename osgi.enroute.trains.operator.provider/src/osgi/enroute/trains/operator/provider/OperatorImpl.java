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

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.Observation.Type;
import osgi.enroute.trains.cloud.api.TrackForCommand;
import osgi.enroute.trains.event.util.EventToObservation;
import osgi.enroute.trains.operator.api.Schedule;
import osgi.enroute.trains.operator.api.ScheduleEntry;
import osgi.enroute.trains.operator.api.TrainOperator;
import osgi.enroute.trains.operator.api.TrainOperatorInfo;
import osgi.enroute.trains.passenger.api.Passenger;
import osgi.enroute.trains.stations.api.StationsManager;

/**
 * This simple operator will cycle through the stations with 1 minute in between...
 * 
 * This component will also run in the cloud so it can just listen to the Events from eventadmin?
 */
@Component(
		name = "osgi.enroute.trains.operator",
		property={"event.topics="+Observation.TOPIC},
		immediate=true)
public class OperatorImpl implements TrainOperator, EventHandler {

	@Reference
	private StationsManager stationsMgr;

	@Reference
	private TrackForCommand tc; 

	@Reference
	private EventAdmin ea;
	
	@Reference
	private DTOs dtos;
	
	// keep the passengers currently on the trains
	private Map<String, List<Passenger>> passengersOnTrains = new HashMap<>();
	
	// keep the schedule for each train
	private Map<String, Schedule> schedules = new HashMap<>();
	
	private TrainOperatorInfo info;
	
	@Activate
	void activate(){
		// TODO from configuration
		info = new TrainOperatorInfo();
		info.id = "My Operator";
		info.stations = new ArrayList<>();
		// fill stations
		info.stations.add("Station1");
		info.stations.add("Station2");
		
		info.trains = new ArrayList<>();
		// fill trains
		info.trains.add("DemoTrain");
		
		
		// create schedule
		for(String train : info.trains){
			Schedule schedule = new Schedule();
			schedule.train = train;
			schedule.entries = new ArrayList<>();
			// TODO create schedule entries...
			ScheduleEntry e = new ScheduleEntry();
			e.start = info.stations.get(0);
			e.destination = info.stations.get(1);
			schedule.entries.add(e);
			schedules.put(train, schedule);
		}
		
		
		// send trains to first scheduled start location
		for(String train : info.trains){
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
				stationsMgr.unboard(train, s.start);
				
				// TODO wait for start time?!
				
				List<Passenger> onBoard = stationsMgr.board(train, s.start);
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
	public TrainOperatorInfo getInfo() {
		return info;
	}

	@Override
	public List<String> getTrains() {
		return info.trains;
	}

	@Override
	public List<Passenger> getPassengersOnTrain(String train) {
		return Collections.unmodifiableList(passengersOnTrains.get(train));
	}

	@Override
	public List<String> getStations() {
		return info.stations;
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
