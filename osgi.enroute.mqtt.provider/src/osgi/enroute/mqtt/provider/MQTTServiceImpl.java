package osgi.enroute.mqtt.provider;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;

import osgi.enroute.mqtt.api.MQTTMessage;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.mqtt.api.Qos;

@Component(name = "osgi.enroute.mqtt",
	immediate=true,
	configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MQTTServiceImpl implements MQTTService, AutoCloseable, MqttCallback {

	private MqttClient mqtt;

	private PushStreamProvider provider = new PushStreamProvider();

	private Map<String, SimplePushEventSource<MQTTMessage>> subscriptions = new ConcurrentHashMap<>();
	
	public MQTTServiceImpl(){
		// to be used with @Activate
	}
	
	public MQTTServiceImpl(MqttClient mqtt){
		this.mqtt = mqtt;
		this.mqtt.setCallback(this);
	}
	
	@ObjectClassDefinition
	@interface MqttConfig {

		String broker();
	
	}		
	
	@Activate	
	void activate(MqttConfig config, BundleContext context) throws Exception {
		String id = context.getProperty(Constants.FRAMEWORK_UUID).toString();
		try {
			mqtt = new MqttClient(config.broker(), id);
			mqtt.connect();
			mqtt.setCallback(this);
		} catch(Exception e){
			System.err.println("Error connecting to MQTT broker "+config.broker());
			throw e;
		}
	}

	@Deactivate
	void deactivate() throws Exception {
		close();
	}
	
	@Override
	public PushStream<MQTTMessage> subscribe(String topic) throws Exception {
		return subscribe(topic, Qos.AT_LEAST_ONCE);
	}

	@Override
	public PushStream<MQTTMessage> subscribe(String topic, Qos qos) throws Exception {
		try {
			// TODO topic might be filter?!
			mqtt.subscribe(topic, qos.ordinal());
			SimplePushEventSource<MQTTMessage> source = subscriptions.get(topic);
			if(source == null){
				source = provider.buildSimpleEventSource(MQTTMessage.class).create();
				subscriptions.put(topic, source);
			}
			return provider.buildStream(source).create();
		} catch(MqttException e){
			throw new Exception(e.getMessage(), e);
		}
	}

	@Override
	public void publish(String topic, ByteBuffer data) throws Exception {
		publish(topic, data, Qos.AT_LEAST_ONCE);
	}

	@Override
	public void publish(String topic, ByteBuffer data, Qos qos) throws Exception {
		mqtt.publish(topic, data.array(), qos.ordinal(), false);
	}

	@Override
	public void publishRetained(String topic, ByteBuffer data, Qos qos) throws Exception {
		mqtt.publish(topic, data.array(), qos.ordinal(), true);
	}

	@Override
	public void close() throws Exception {
		if(mqtt != null)
			mqtt.close();
	}

	
	@Override
	public void connectionLost(Throwable ex) {
		// TODO log?
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryComplete) {
		// TODO do something with this
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO match topic filters
		SimplePushEventSource<MQTTMessage> source = subscriptions.get(topic);
		if(source != null){
			if(!source.isConnected()){
				source.close();
				subscriptions.remove(topic);
				return;
			}
			
			MQTTMessage msg = new MQTTMessageImpl(topic, ByteBuffer.wrap(message.getPayload()));
			source.publish(msg);
		}
		
	}

}
