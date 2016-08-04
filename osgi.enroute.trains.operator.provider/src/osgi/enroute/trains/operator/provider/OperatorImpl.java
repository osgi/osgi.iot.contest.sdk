package osgi.enroute.trains.operator.provider;

import java.time.Instant;
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
import osgi.enroute.scheduler.api.CancellablePromise;
import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.Observation.Type;
import osgi.enroute.trains.cloud.api.TrackForCommand;
import osgi.enroute.trains.event.util.EventToObservation;
import osgi.enroute.trains.operator.api.Schedule;
import osgi.enroute.trains.operator.api.ScheduleEntry;
import osgi.enroute.trains.operator.api.TrainOperator;
import osgi.enroute.trains.operator.provider.OperatorImpl.Config;
import osgi.enroute.trains.passenger.api.Passenger;
import osgi.enroute.trains.stations.api.StationObservation;
import osgi.enroute.trains.stations.api.StationsManager;

/**
 * This simple operator will cycle through all stations.
 * 
 * This component will also run in the cloud so it can just listen to the Events from EventAdmin
 */
@Designate(ocd = Config.class, factory = true)
@Component(
		name = Config.TRAIN_OPERATOR_PID,
		property={"event.topics="+Observation.TOPIC},
		immediate=true)
public class OperatorImpl implements TrainOperator, EventHandler {

	private static int BOARD_TIME = 30000;
	private static int TRAVEL_TIME = 80000;
	
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
	
	@Reference
	private Scheduler scheduler;
	
	private String name;
	private List<String> trains = new ArrayList<>();
	private List<String> stations = new ArrayList<>();

	private Map<String, CancellablePromise<Instant>> timeouts = new HashMap<>();
	
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
		for(int i=0;i<trains.size();i++){
			String train = trains.get(i);

			Schedule schedule = new Schedule();
			schedule.train = train;
			schedule.entries = new ArrayList<>();
			
			// don't start all the trains at same instant...
			long time = System.currentTimeMillis()+BOARD_TIME*i;
			ScheduleEntry e = new ScheduleEntry();
			// also mixup initial stations for each train
			int k = i % stations.size();
			e.start = stations.get(k);
			e.departureTime = time;
			e.destination = k+1 < stations.size() ? stations.get(k+1) : stations.get(0);
			e.arrivalTime = e.departureTime + TRAVEL_TIME; 
				
			schedule.entries.add(e);
			schedules.put(train, schedule);
			
			// schedule train start
			scheduler.at(time).then(
					p -> {
						ScheduleEntry s = schedules.get(train).entries.get(0);
						String dest = s.start;
						String segment = stationsMgr.getStationSegment(dest);
						System.out.println("Assign train "+train+" to station "+dest+" (segment "+segment+")");
						tc.assign(train, segment);
						
						timeouts.put(train, scheduler.at(s.arrivalTime));
						timeouts.get(train).then(pp -> {
							delayed(train, s.destination);
							return null;
						});
						return null;
					},
					p -> p.getFailure().printStackTrace()
			);
		}
	}
	
	@Override
	public void handleEvent(Event event) {
		try {
			Observation o = EventToObservation.eventToObservation(event, dtos);
			
			if(o.type == Type.ASSIGNMENT_REACHED){
				String train = o.train;
				String station = stationsMgr.getStation(o.assignment);

				// cancel timeout
				CancellablePromise<Instant> cp = timeouts.remove(train);
				if(cp != null){
					cp.cancel();
				}
				
				// notify arrival
				System.out.println("Train "+train+" arrived at "+station);
				stationsMgr.arrive(train, station);

				// schedule next departure
				final Schedule schedule = schedules.get(train);
				final ScheduleEntry s = schedule.entries.get(0);
				
				scheduler.at(s.departureTime).then(p -> {
					
					List<Passenger> onBoard = stationsMgr.leave(train, s.start);
					passengersOnTrains.put(train, onBoard);
					String segment = stationsMgr.getStationSegment(s.destination);
					
					timeouts.put(train, scheduler.at(s.arrivalTime));
					timeouts.get(train).then(pp -> {
						delayed(train, s.destination);
						return null;
					});
					
					System.out.println("Train "+train+" now has "+onBoard.size()+" passengers on board, leaving for "+s.destination);
					tc.assign(train, segment);
					
					// Add new schedule entry
					ScheduleEntry last = schedule.entries.get(schedule.entries.size()-1);
					ScheduleEntry extra = new ScheduleEntry();

					extra.start = last.destination;
					extra.departureTime = Math.max(System.currentTimeMillis()+TRAVEL_TIME, s.arrivalTime) + BOARD_TIME;
					int index = stations.indexOf(last.destination);
					extra.destination = index+1 < stations.size() ? stations.get(index+1) : stations.get(0);
					extra.arrivalTime = extra.departureTime + TRAVEL_TIME;
					schedule.entries.add(extra);
					
					// Remove first entry
					schedule.entries.remove(0);
					
					
					return null;
				}, p -> p.getFailure().printStackTrace());
				
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

	private void delayed(String train, String destination){
		StationObservation delay = new StationObservation();
		delay.type = StationObservation.Type.NOTIFICATION;
		delay.station = destination;
		delay.train = train;
		delay.message = "Train "+train+" traveling to "+destination+" is delayed";
		try {
			Event event = new Event(StationObservation.TOPIC, dtos.asMap(delay));
			ea.postEvent(event);
		} catch(Exception e){
			System.err.println("Error sending notification Event: "+e.getMessage());
		}
	}
}
