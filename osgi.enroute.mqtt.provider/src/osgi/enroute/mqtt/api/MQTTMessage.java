package osgi.enroute.mqtt.api;

import java.nio.ByteBuffer;

public interface MQTTMessage {
	public String topic();

	public ByteBuffer payload();
}
