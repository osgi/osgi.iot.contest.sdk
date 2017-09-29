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
import osgi.enroute.scheduler.api.Scheduler;
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
			Debug.COMMAND_FUNCTION + "=container",
			Debug.COMMAND_FUNCTION + "=service"
	}, service=TrainsDemo.class)
public class TrainsDemo {

	private Random random = new Random();
	
	private List<String> trains = new ArrayList<>();
	
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
			station.name = split[0];
			station.segment = split[1];
			
			stationList.add(station.name);
			stations.put(station.name, station);
		}
		
		for(String s : config.trains()){
			trains.add(s);
			scheduleRandomStation(s);
		}
		
		mqtt.subscribe(Assignment.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(Assignment.class))
			.filter(a -> a.type == Assignment.Type.REACHED)
			.forEach(a -> {
				// train reached assignment 
				String train = a.train;
				Optional<Station> station = stations.values().stream()
					.filter(s -> s.segment.equals(a.segment))
					.findFirst();
				
				if(station.isPresent()){
					System.out.println("Train "+a.train+" reached station "+station.get().name);
					
					// TODO send out station reached event
					
				}
				
				// reschedule?
				scheduleRandomStation(train);
				
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
	
	
	// send a train to the servicing station
	public void service(String train){
		
	}
	
	// load/unload container
	public void container(){
		
	}
	
	
	private void scheduleRandomStation(String train){
		final String station = stationList.get(random.nextInt(stationList.size()));
		int seconds = random.nextInt(20);
		System.out.println("Send "+train+" to "+station+" in "+seconds+" s.");
		scheduler.schedule(()-> station(train, station), seconds, TimeUnit.SECONDS);
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
