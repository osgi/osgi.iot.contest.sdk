package osgi.enroute.trains.location.test;

import java.nio.ByteBuffer;

import org.osgi.util.converter.Converter;

import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.mqtt.api.MQTTServiceBuilder;
import osgi.enroute.mqtt.provider.MQTTServiceBuilderImpl;
import osgi.enroute.trains.track.api.Observation;
import osgi.enroute.trains.util.converter.TrainsConverter;

public class LocationTest {

	// simple class to inject some dummy events
	public static void main(String[] args) throws Exception {
		
		MQTTServiceBuilder builder = new MQTTServiceBuilderImpl();
		MQTTService mqtt = builder.connect("tcp://localhost:1883", "test").build();
		
		Converter converter = TrainsConverter.getTrainsConverter();
		
		int i =0;
		while(true){
			try {
				Thread.sleep(500);

				Observation o = new Observation();
				o.time = System.currentTimeMillis();
				o.type = Observation.Type.LOCATED;
				o.segment = "A"+String.format("%02d", i++);;
				o.train = "White";
				mqtt.publish(Observation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));

			} catch(Exception e){
				System.err.println("Failed to publish observation");
				e.printStackTrace();
			}
		}
	}
}
