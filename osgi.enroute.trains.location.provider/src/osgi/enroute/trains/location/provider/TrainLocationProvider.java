package osgi.enroute.trains.location.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.converter.Converter;

import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.trains.track.api.TrackObservation;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.api.TrackManager;

@Component(
        immediate = true,
        name="osgi.enroute.trains.location.provider",
    	configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TrainLocationProvider {

	private LocationProviderConfig config;
	private Thread reader;
	private volatile boolean running = false;
	
	private Map<String, Integer> codeToTag;
	private Map<Integer, String> tagToCode;
	
	private Map<Integer, String> tagToSegment;
	private Map<String, Integer> segmentToTag;
	
	private int lastTag = 0;
	private String lastSegment = null;
	
	@Reference
	protected Converter converter;
	
	@Reference
	protected MQTTService mqtt;
	
	@Reference
	protected TrackManager trackManager;
	
	
	@Activate
	void activate(LocationProviderConfig config){
		this.config = config;
		
		// build code to segment map
		// TODO for now we use the track manager here ... so it only works when 
		// the locator is co-deployed with track manager
		// to mitigate this we could also parse track config here or use dosgi for track manager
		buildCodeMap();
		
		// try to set up rfcomm port by issuing Linux command...
		// TODO check for root?
		try {
			Runtime.getRuntime().exec("rfcomm bind "+config.rfcomm()+" "+config.mac()+" "+config.channel());
			
			running = true;
			reader = new Thread(new Runnable() {
				@Override
				public void run() {
					try (InputStreamReader r = 
							new InputStreamReader(new FileInputStream(new File(config.rfcomm())))){
						char[] buffer = new char[10];

						while(running){
							r.read(buffer);
							//System.out.println(new String(buffer));
							String code = new String(buffer, 5, 5);
							Integer tag = codeToTag.get(code);
							if(tag == null){
								// Check whether this relates to the next segment?
								if(lastSegment == null)
									continue;
								
								Segment last = trackManager.getSegment(lastSegment);
								for(String to : last.to){
									Integer t = segmentToTag.get(to);
									if(t == null)
										continue;
									
									String c = tagToCode.get(t);
									if(c == null)
										continue;
											
									int distance = distance(c, code);
									// System.out.println("Excpected "+c+", got "+code+", distance "+distance);
									if(distance <= 1){
										// We probably have a match
										// System.out.println(code+" matches "+c+", probably "+to);
										tag = t;
										continue;
									}
								}
							}
							
							String segment = tagToSegment.get(tag);
							if(segment == null){
								continue;
							}
							
							if(!segment.equals(lastSegment)){
								// send out observation
								try {
									TrackObservation o = new TrackObservation();
									o.time = System.currentTimeMillis();
									o.type = TrackObservation.Type.LOCATED;
									o.segment = segment;
									o.train = config.train();
									mqtt.publish(TrackObservation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
								} catch(Exception e){
									System.err.println("Failed to publish observation");
									e.printStackTrace();
								}
								
							}
							
							lastSegment = segment;
							lastTag = tag;
						}
					} catch(Exception e){
						e.printStackTrace();
					}
				}
			});
			reader.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	@Deactivate
	void deactivate(){
		running = false;
		reader.interrupt();
		// cleanup rfcomm
		try {
			Runtime.getRuntime().exec("rfcomm release "+config.rfcomm());
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void buildCodeMap(){
		Map<String, Segment> segments = trackManager.getSegments();
		tagToSegment = segments.values().stream()
				.filter( s -> s.tag != 0)
				.collect(Collectors.toMap(s -> s.tag, s -> s.id));
		
		segmentToTag = segments.values().stream()
				.filter( s -> s.tag != 0)
				.collect(Collectors.toMap(s -> s.id, s -> s.tag));
		
		codeToTag = new HashMap<String, Integer>();
		tagToCode = new HashMap<Integer, String>();
		for(String line : config.code2tag()){
			String[] split = line.split(":");
			int tag = Integer.parseInt(split[0]);
			String code = split[1];
			// only keep last 5 characters
			code = code.substring(5);
			codeToTag.put(code, tag);
			tagToCode.put(tag, code);
		}
	}
	
	private int distance(String s1, String s2){
		int distance = 0;
		for(int i=0;i<s1.length();i++){
			if(s2.length()<=i || s2.charAt(i) != s1.charAt(i))
				distance++;
		}
		distance += (s2.length() - s1.length());
		return distance;
	}
}
