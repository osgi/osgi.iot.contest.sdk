package osgi.enroute.mqtt.api;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Properties;

public interface MQTTServiceBuilder {
	
	// create the actual MQTTService
	public MQTTService build() throws Exception;

	// connect to a broker
	public MQTTServiceBuilder connect(String serverURI, String clientId);

	// clean the session when the client reconnects
	public MQTTServiceBuilder clean();

	// automatically reconnect
	public MQTTServiceBuilder autoReconnect();

	// set max inflight
	public MQTTServiceBuilder maxInFlight(int max);

	// set connection timeout
	public MQTTServiceBuilder timeout(Duration timeout);

	// set keepAlive interval
	public MQTTServiceBuilder keepAlive(Duration keepAlive);

	// set username
	public MQTTServiceBuilder username(String username);

	// set password
	public MQTTServiceBuilder password(String password);

	// set ssl properties
	public MQTTServiceBuilder ssl(Properties props);

	// set last will
	public MQTTServiceBuilder lastWill(String topic, ByteBuffer data, Qos qos, boolean retained);
	
}
