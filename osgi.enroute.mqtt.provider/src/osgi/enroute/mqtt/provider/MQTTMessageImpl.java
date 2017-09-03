package osgi.enroute.mqtt.provider;

import java.nio.ByteBuffer;

import osgi.enroute.mqtt.api.MQTTMessage;

public class MQTTMessageImpl implements MQTTMessage {
	
	private final String topic;
	private final ByteBuffer payload;
	
	public MQTTMessageImpl(String topic, ByteBuffer payload){
		this.topic = topic;
		this.payload = payload;
	}
	
	public String topic(){
		return topic;
	}

	public ByteBuffer payload(){
		return payload;
	}
}
