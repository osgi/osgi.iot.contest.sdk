package osgi.enroute.trains.demo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.converter.Converter;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.trains.robot.api.RobotCommand;
import osgi.enroute.trains.robot.api.RobotObservation;
import osgi.enroute.trains.train.api.Assignment;
import osgi.enroute.trains.train.api.Assignment.Type;

/**
 * Train demo.
 */
@Component(immediate = true,
	name="osgi.enroute.trains.demo",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	property = {
			Debug.COMMAND_SCOPE +"=trains",
			Debug.COMMAND_FUNCTION + "=station",
			Debug.COMMAND_FUNCTION + "=container"},
	service=TrainsDemo.class)
public class TrainsDemo {

	private Random random = new Random();
	
	private Map<String, Boolean> trains = new HashMap<>();
	
	// separate list of station names to facilitate random station selection
	private List<String> stationList = new ArrayList<>();
	private Map<String, Station> stations = new HashMap<>();

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
	
	@Reference
	private Converter converter;

	@Reference
	private MQTTService mqtt;
	
	
	@ObjectClassDefinition
	@interface Config {
		/**
		 * A list of stations specified as "station:segment"
		 */
		String[] stations();
		
		/**
		 * A list of trains to control in the demo
		 */
		String[] trains();
	}
	
	@Activate
	void activate(Config config) throws Exception {

		for(String s : config.stations()){
			String[] split = s.split(":");
			Station station = new Station();
			station.name = split[0].trim();
			station.segment = split[1].trim();
			station.type = Station.Type.valueOf(split[2].trim());
			
			stationList.add(station.name);
			stations.put(station.name, station);
		}
		
		for(String s : config.trains()){
			String[] split = s.split(":");
			String train = split[0].trim();
			boolean loaded = Boolean.parseBoolean(split[1].trim());
			trains.put(train, loaded);
			scheduleRandomStation(train);
		}
		
		mqtt.subscribe(Assignment.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(Assignment.class))
			.forEach(a -> {
				switch(a.type){
				case REACHED:
				{
					// train reached assignment 
					String train = a.train;
					Optional<Station> station = stations.values().stream()
						.filter(s -> s.segment.equals(a.segment))
						.findFirst();
					
					if(station.isPresent()){
						System.out.println("Train "+a.train+" reached station "+station.get().name);
						
						station.get().train = train;
						
						event("Train "+a.train+" reached station "+station.get().name);
						
						if(station.get().type == Station.Type.CARGO){
							if(trains.get(a.train)){
								// this train is loaded?
								event("Unloading container from "+a.train);
								container(false);
							} else {
								event("Loading container onto "+a.train);
								container(true);
							}
						} else {
							scheduleRandomStation(train);
						}
					} else {
						// reschedule when assigned to custom segment?
						scheduleRandomStation(train);
					}
					
					
					break;
				}
				case ASSIGN:
				{	
					Optional<Station> station = stations.values().stream()
						.filter(s -> a.train.equals(s.train))
						.findFirst();	
					
					if(station.isPresent()){
						event("Train "+a.train+" leaving station "+station.get().name);
						station.get().train = null;
					}
					break;
				}
			}
		});
		
		
		mqtt.subscribe(RobotObservation.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(RobotObservation.class))
			.forEach(a -> {

				// check which train is in cargo station
				Optional<Station> station = stations.values().stream()
						.filter(s -> s.type == Station.Type.CARGO)
						.findFirst();
				
				if(station.isPresent()){
					String train = station.get().train;
					
					if(train != null){
						// reschedule
						scheduleRandomStation(train);	
					}
				}
		});
		
		
	}
	
	// send a train to a station
	public void station(String train, String station){
		Assignment a = new Assignment();
		a.type = Type.ASSIGN;
		a.train = train;
		a.segment = stations.get(station).segment;
		
		System.out.println("Assign "+train+" to station "+station+" ("+a.segment+")");
		try {
			mqtt.publish(Assignment.TOPIC,  ByteBuffer.wrap( converter.convert(a).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	// load/unload container
	public void container(boolean load){
		RobotCommand c = new RobotCommand();
		c.type = load ? RobotCommand.Type.LOAD : RobotCommand.Type.UNLOAD;
		
		try {
			mqtt.publish(RobotCommand.TOPIC,  ByteBuffer.wrap( converter.convert(c).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	private void scheduleRandomStation(String train){
		final String station = stationList.get(random.nextInt(stationList.size()));
		int seconds = random.nextInt(20);
		System.out.println("Send "+train+" to "+station+" in "+seconds+" s.");
		scheduler.schedule(()-> station(train, station), seconds, TimeUnit.SECONDS);
	}
	
	
	public void event(String message){
		DemoEvent event = new DemoEvent();
		event.time = System.currentTimeMillis();
		event.message = message;
		
		try {
			mqtt.publish(DemoEvent.TOPIC,  ByteBuffer.wrap( converter.convert(event).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//
	
	// Demo actions
	// - for each train: command to go to station vs random loop
	// - trigger the robot (only when train in station?!)
	
	// Events to send out
	// - train arriving in station
	// - train departing station
	// - load/unload events
	
	// Other scenarios
	// - send train to servicing station
}
