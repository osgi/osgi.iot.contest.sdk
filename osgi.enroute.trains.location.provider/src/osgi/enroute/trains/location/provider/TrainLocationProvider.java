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
import osgi.enroute.trains.track.api.Observation;
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
	private Map<Integer, String> tagToSegment;
	
	private int lastTag = 0;
	
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
							String code = new String(buffer, 5, 5);
							Integer tag = codeToTag.get(code);
							if(tag == null){
								System.out.println("Unknown code: "+code);
								continue;
							}
							
							String segment = tagToSegment.get(tag);
							if(segment == null){
								System.out.println("Unknown tag: "+tag);
							}
							
							if(lastTag != tag){
								// send out observation
								try {
									Observation o = new Observation();
									o.time = System.currentTimeMillis();
									o.type = Observation.Type.LOCATED;
									o.segment = segment;
									o.train = config.train();
									mqtt.publish(Observation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
								} catch(Exception e){
									System.err.println("Failed to publish observation");
									e.printStackTrace();
								}
								
							}
							
							lastTag = tag;
						}
					} catch(Exception e){
						e.printStackTrace();
					}
				}
			});
			reader.start();
		} catch (IOException e) {
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
		
		codeToTag = new HashMap<String, Integer>();
		for(String line : config.code2tag()){
			String[] split = line.split(":");
			int tag = Integer.parseInt(split[0]);
			String code = split[1];
			// only keep last 5 characters
			code = code.substring(5);
			codeToTag.put(code, tag);
		}
	}
}
