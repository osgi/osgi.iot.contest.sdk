package osgi.enroute.mqtt.provider;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.osgi.service.component.annotations.Component;

import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.mqtt.api.MQTTServiceBuilder;
import osgi.enroute.mqtt.api.Qos;

@Component
public class MQTTServiceBuilderImpl implements MQTTServiceBuilder {

	private String serverURI;
	private String clientId;
	private MqttConnectOptions options = new MqttConnectOptions();

	@Override
	public MQTTService build() throws Exception {
		MqttClient client = new MqttClient(serverURI, clientId);
		client.connect(options);
		return new MQTTServiceImpl(client);
	}

	@Override
	public MQTTServiceBuilder connect(String serverURI, String clientId) {
		this.serverURI = serverURI;
		this.clientId = clientId;
		return this;
	}

	@Override
	public MQTTServiceBuilder clean() {
		options.setCleanSession(true);
		return this;
	}

	@Override
	public MQTTServiceBuilder autoReconnect() {
		options.setAutomaticReconnect(true);
		return this;
	}

	@Override
	public MQTTServiceBuilder maxInFlight(int max) {
		options.setMaxInflight(max);
		return this;
	}

	@Override
	public MQTTServiceBuilder timeout(Duration timeout) {
		options.setConnectionTimeout((int)timeout.getSeconds());
		return this;
	}

	@Override
	public MQTTServiceBuilder keepAlive(Duration keepAlive) {
		options.setKeepAliveInterval((int)keepAlive.getSeconds());
		return this;
	}

	@Override
	public MQTTServiceBuilder username(String username) {
		options.setUserName(username);
		return this;
	}

	@Override
	public MQTTServiceBuilder password(String password) {
		options.setPassword(password.toCharArray());
		return this;
	}

	@Override
	public MQTTServiceBuilder ssl(Properties props) {
		options.setSSLProperties(props);
		return this;
	}

	@Override
	public MQTTServiceBuilder lastWill(String topic, ByteBuffer data, Qos qos, boolean retained) {
		options.setWill(topic, data.array(), qos.ordinal(), retained);
		return this;
	}

}
