package osgi.enroute.mqtt.provider;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Properties;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.mqtt.api.MQTTServiceBuilder;
import osgi.enroute.mqtt.api.Qos;

@Component
public class MQTTServiceBuilderImpl implements MQTTServiceBuilder {

	@Override
	public MQTTService build() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder connect(String serverURI, String clientId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder clean() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder autoReconnect() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder maxInFlight(int max) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder timeout(Duration timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder keepAlive(Duration keepAlive) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder username(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder password(String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder ssl(Properties props) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTServiceBuilder lastWill(String topic, ByteBuffer data, Qos qos, boolean retained) {
		// TODO Auto-generated method stub
		return null;
	}

}
