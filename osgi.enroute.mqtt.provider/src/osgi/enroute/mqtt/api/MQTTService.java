package osgi.enroute.mqtt.api;

import java.nio.ByteBuffer;

import org.osgi.util.pushstream.PushStream;

public interface MQTTService {
	
	public PushStream<MQTTMessage> subscribe(String topic) throws Exception;

	public PushStream<MQTTMessage> subscribe(String topic, Qos qos) throws Exception;

	public void publish(String topic, ByteBuffer data) throws Exception;

	public void publish(String topic, ByteBuffer data, Qos qos) throws Exception;

	public void publishRetained(String topic, ByteBuffer data, Qos qos) throws Exception;
}
