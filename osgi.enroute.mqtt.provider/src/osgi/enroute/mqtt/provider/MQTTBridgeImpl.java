package osgi.enroute.mqtt.provider;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Helper component to exchange some topics between two brokers
 * 
 * TODO should also be configurable in Mosquitto?
 */
@Component(name = "osgi.enroute.mqtt.bridge",
	immediate=true,
	configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MQTTBridgeImpl {

	private MqttClient mqtt1;
	private MqttClient mqtt2;

	@ObjectClassDefinition
	@interface MqttBridgeConfig {

		String broker_from();
	
		String broker_to();
		
		String[] topics_publish();
		
		String[] topics_subscribe();
	}		
	
	@Activate	
	void activate(MqttBridgeConfig config, BundleContext context) throws Exception {
		mqtt1 = connect(config.broker_from());
		mqtt2 = connect(config.broker_to());
		
		mqtt1.setCallback(new BridgeMqttCallback(mqtt2));
		mqtt1.subscribe(config.topics_publish());
		
		mqtt2.setCallback(new BridgeMqttCallback(mqtt1));
		mqtt2.subscribe(config.topics_subscribe());
	}

	private MqttClient connect(String broker) throws Exception {
		String id = UUID.randomUUID().toString();
		try {
			MqttClient mqtt = new MqttClient(broker, id);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			mqtt.connect(options);
			return mqtt;
		} catch(Exception e){
			System.err.println("Error connecting to MQTT broker "+broker);
			throw e;
		}
	}
	
	@Deactivate
	void deactivate() throws Exception {
	}
	
	private class BridgeMqttCallback implements MqttCallback {
		
		private final MqttClient other;
		
		public BridgeMqttCallback(MqttClient other){
			this.other = other;
		}
		
		@Override
		public void messageArrived(String topic, MqttMessage msg) throws Exception {
			other.publish(topic, msg);
		}
		
		@Override
		public void deliveryComplete(IMqttDeliveryToken t) {
		}
		
		@Override
		public void connectionLost(Throwable t) {
		}
	}
}
