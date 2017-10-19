package osgi.enroute.trains.demo;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.converter.Converter;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.trains.demo.api.DemoCommand;
import osgi.enroute.trains.demo.api.DemoObservation;
import osgi.enroute.trains.robot.api.RobotCommand;
import osgi.enroute.trains.robot.api.RobotObservation;
import osgi.enroute.trains.train.api.Assignment;
import osgi.enroute.trains.train.api.TrainCommand;
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
			Debug.COMMAND_FUNCTION + "=robot",
			Debug.COMMAND_FUNCTION + "=robotHasContainer",
			Debug.COMMAND_FUNCTION + "=container",
			Debug.COMMAND_FUNCTION + "=stop",
			Debug.COMMAND_FUNCTION + "=start",
			Debug.COMMAND_FUNCTION + "=blink",
			Debug.COMMAND_FUNCTION + "=emergency"},
	service=TrainsDemo.class)
public class TrainsDemo {

	private Random random = new Random();
	
	private volatile boolean emergency = false;
	private volatile boolean blink = true;
	
	private boolean robot = true;
	private volatile boolean robotHasContainer = false;
	
	private Map<String, Train> trains = new HashMap<>();
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
			stations.put(station.name, station);
		}
		
		for(String train : config.trains()){
			Train t = new Train();
			t.name = train;
			t.assignment = null;
			t.state = Train.State.RUNNING;
			trains.put(train, t);
			scheduleRandomStation(train);
		}
		
		mqtt.subscribe(Assignment.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(Assignment.class))
			.forEach(a -> {
				Train train = trains.get(a.train);
				if(train == null){
					System.err.println("Invalid train? "+a.train);
					return;
				}
				
				switch(a.type){
				case REACHED:
				{
					// train reached assignment 
					train.assignment = null;

					Optional<Station> station = stations.values().stream()
						.filter(s -> s.segment.equals(a.segment))
						.findFirst();
					
					if(station.isPresent()){
						System.out.println("Train "+a.train+" reached station "+station.get().name);
						
						station.get().train = train.name;
						
						message("Train "+a.train+" reached station "+station.get().name);

						if(station.get().type == Station.Type.CARGO && robot){
							scheduler.schedule(() -> container(), 3, TimeUnit.SECONDS);
						} else if(train.state == Train.State.STOPPING){
							// we reached the parking station, so stop now
							// TODO send out stopped event
							System.out.println("Train "+train.name+" stopped!");
							stopped(train.name, station.get().name);
							
							train.state = Train.State.STOPPED;
						} else {
							scheduleRandomStation(train.name);
						}
					} else {
						// reschedule when assigned to custom segment?
						scheduleRandomStation(train.name);
					}
					
					
					break;
				}
				case ASSIGN:
				{	
					train.assignment = a.segment;
					
					Optional<Station> station = stations.values().stream()
						.filter(s -> a.train.equals(s.train))
						.findFirst();	
					
					if(station.isPresent()){
						message("Train "+a.train+" leaving station "+station.get().name);
						station.get().train = null;
					}
					break;
				}
				case ABORTED:
				{
					train.assignment = null;
					
					// re-assign?
					scheduleRandomStation(train.name);
					break;
				}
			}
		});
		
		
		mqtt.subscribe(RobotObservation.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(RobotObservation.class))
			.forEach(o -> {

				// check which train is in cargo station
				Optional<Station> station = stations.values().stream()
						.filter(s -> s.type == Station.Type.CARGO)
						.findFirst();
				
				if(station.isPresent()){
					Train train = trains.get(station.get().train);
					
					if(train != null){
						// update loaded state
						switch(o.type){
						case LOADED:
							robotHasContainer = !o.success;
							break;
						case UNLOADED:
							robotHasContainer = o.success;
							break;
						}
						
						// leave the station after one second
						scheduleRandomStation(train.name, 1);	
					}
				}
		});
		
		
		mqtt.subscribe(DemoCommand.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(DemoCommand.class))
			.forEach(c -> {
				switch(c.type){
				case START:
					start(c.train);
					break;
				case STOP:
					stop(c.train);
					break;
				case EMERGENCY:
					emergency(c.emergency);
					break;
				}
			});
	}

	public boolean robot(){
		return robot;
	}
	
	public void robot(boolean r){
		this.robot = r;
	}
	
	public boolean robotHasContainer(){
		return robotHasContainer;
	}
	
	public void robotHasContainer(boolean c){
		this.robotHasContainer  = c;
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
	public void container(){
		RobotCommand c = new RobotCommand();
		// if robot has container, try to load it on train - will fail if train is already loaded?
		c.type = robotHasContainer ? RobotCommand.Type.LOAD : RobotCommand.Type.UNLOAD;
		
		try {
			mqtt.publish(RobotCommand.TOPIC,  ByteBuffer.wrap( converter.convert(c).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void stop(String train){
		Train t = trains.get(train);
		if(t == null){
			return;
		}
		
		// cancel any running schedules
		if(t.schedule != null){
			t.schedule.cancel(false);
		}
		
		// send the train to a "parking" station
		Optional<Station> parkingStation = stations.values().stream()
			.filter(station -> station.type == Station.Type.PARKING) // only parking stations
			.filter(station -> station.train == null || station.train.equals(t.name)) // ignore stations that currently have train parked
			.filter(station -> trains.values().stream().filter(tt -> !tt.name.equals(t.name)).filter(tt -> station.segment.equals(tt.assignment)).count() == 0) // filter stations that are assigned to other trains
			.findFirst();
		
		if(!parkingStation.isPresent()){
			System.err.println("No parking station available?!");
			System.err.println("Stations");
			for(Station station : stations.values()){
				System.err.println("* "+station.name+" "+station.type+" "+station.train);
			}
			System.err.println("Trains");
			for(Train tt : trains.values()){
				System.err.println("* "+tt.name+" "+tt.assignment);
			}
			return;
		}
		
		if(t.name.equals(parkingStation.get().train)){
			// train is already at parking station!
			t.state = Train.State.STOPPED;
			
			System.out.println("Train "+t.name+" stopped at station "+parkingStation.get().name);
			stopped(t.name, parkingStation.get().name);
		} else {
			t.state = Train.State.STOPPING;
			station(train, parkingStation.get().name);

			System.out.println("Train "+t.name+" stopping!");
			message("Train "+t.name+" stopping");
		}

	}
	
	public void emergency(boolean emergency){
		this.emergency = emergency;
		
		if(emergency){
			// stop everything
			for(Train t : trains.values()){
				
				if(t.schedule != null){
					t.schedule.cancel(false);
				}
				
				Assignment a = new Assignment();
				a.type = Assignment.Type.ABORT;
				a.train = t.name;
				
				try {
					mqtt.publish(Assignment.TOPIC,  ByteBuffer.wrap( converter.convert(a).to(byte[].class)));
				} catch(Exception e){
					System.out.println("Failed publishing command");
				}		
			}
			resetRobot();
			blink();
			message("EMERGENCY! EMERGENCY!");
			
		} else {
			// start trains again
			for(Train t : trains.values()){
				scheduleRandomStation(t.name, 0);
			}
		}
	}

	public void blink(){
		for(Train train : trains.values()){
			TrainCommand c = new TrainCommand();
			c.type = TrainCommand.Type.LIGHT;
			c.on = blink;
			c.train = train.name;
			
			try {
				mqtt.publish(TrainCommand.TOPIC,  ByteBuffer.wrap( converter.convert(c).to(byte[].class)));
			} catch(Exception e){
				System.out.println("Failed publishing command");
			}
		}
		
		if(emergency || blink){
			blink = !blink;
			scheduler.schedule(()->blink(), 1, TimeUnit.SECONDS);
		}
	}
	
	public void resetRobot(){
		RobotCommand c = new RobotCommand();
		c.type = RobotCommand.Type.RESET;
		robotHasContainer = false;
		
		try {
			mqtt.publish(RobotCommand.TOPIC,  ByteBuffer.wrap( converter.convert(c).to(byte[].class)));
		} catch(Exception e){
			System.out.println("Failed publishing command");
		}
	}
	
	
	public void start(String train){
		Train t = trains.get(train);
		if(t == null){
			return;
		}
		
		System.out.println("Train "+t.name+" started!");
		started(t.name);
		
		scheduleRandomStation(train, 0);
	}
	
	private void scheduleRandomStation(String train){
		int delay = random.nextInt(10);
		scheduleRandomStation(train, delay);
	}

	private void scheduleRandomStation(String train, int delay){
		if(emergency){
			return;
		}
		
		Train t = trains.get(train);
		if(t == null){
			return;
		}
		
		// cancel any previous schedules
		if(t.schedule != null){
			t.schedule.cancel(false);
		}
		
		// filter out stations that currently have a train
		List<Station> stationList = stations.values().stream()
				.filter(station -> station.train == null)
		//		.filter(station -> trains.values().stream().filter(tt -> station.segment.equals(tt.assignment)).count() == 0)
				.collect(Collectors.toList());
		
		System.out.println("Potential stations: ");
		stationList.stream().forEach(s -> System.out.println("* "+s.name+" "+s.type));
		
		Station assignment;
		Optional<Station> cargo = stationList.stream().filter(station -> station.type == Station.Type.CARGO).findFirst(); 
		if(cargo.isPresent()){
			System.out.println("Send "+train+" to cargo station");
			assignment = cargo.get();
		} else {
			assignment = stationList.get(random.nextInt(stationList.size()));
		}
		
		final String station = assignment.name;

		System.out.println("Send "+train+" to "+station+" in "+delay+" s.");
		t.schedule = scheduler.schedule(()-> station(train, station), delay, TimeUnit.SECONDS);
	}
	
	public void started(String train){
		DemoObservation o = new DemoObservation();
		o.time = System.currentTimeMillis();
		o.type = DemoObservation.Type.TRAIN_STARTED;
		o.train = train;
		o.message = "Train "+train+" started";

		try {
			mqtt.publish(DemoObservation.TOPIC,  ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void stopped(String train, String station){
		DemoObservation o = new DemoObservation();
		o.time = System.currentTimeMillis();
		o.type = DemoObservation.Type.TRAIN_STOPPED;
		o.train = train;
		o.message = "Train "+train+" stopped at station "+station;

		try {
			mqtt.publish(DemoObservation.TOPIC,  ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public void message(String message){
		DemoObservation o = new DemoObservation();
		o.time = System.currentTimeMillis();
		o.message = message;
		o.type = DemoObservation.Type.MESSAGE;
		
		try {
			mqtt.publish(DemoObservation.TOPIC,  ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
