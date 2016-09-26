package osgi.enroute.trains.mqtt.provider;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.dto.api.DTOs;

@Component(name = "osgi.enroute.trains.mqtt",
    service = {
        EventHandler.class
    },
    configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MQTTEventAdapter implements EventHandler, MqttCallback {

  private MqttClient mqtt;
  private String     id;

  @Reference
  private DTOs       dtos;

  @Reference
  private EventAdmin ea;

  @ObjectClassDefinition
  @interface Config {
    String[] event_topics() default {};

    String broker();

  }

  @Activate
  void activate(Config config, BundleContext context) throws Exception {
    id = context.getProperty(Constants.FRAMEWORK_UUID).toString();
    try {
      mqtt = new MqttClient(config.broker(), id);
      mqtt.connect();
      mqtt.setCallback(this);
      for (String topic : config.event_topics()) {
        mqtt.subscribe(topic.replaceAll("\\*", "#"));
      }
    } catch (Exception e) {
      System.err.println("Error connecting to MQTT broker " + config.broker());
      throw e;
    }
  }

  /**
   * EventHandler 
   */
  @Override
  public void handleEvent(Event event) {
    // serialize OSGi event to JSON object and send out as MQTT event
    Map<String, Object> eventMap = new HashMap<>();
    for (String k : event.getPropertyNames()) {
      eventMap.put(k, event.getProperty(k));
    }

    // tag with the sender id to filter out on receiving messages
    String sender = (String) eventMap.get("_sender");
    if (sender != null && sender.equals(id)) {
      return;
    } else {
      eventMap.put("_sender", id);
    }

    try {
      String json = dtos.encoder(eventMap).ignoreNull().put();
      MqttMessage msg = new MqttMessage(json.getBytes());
      mqtt.publish(event.getTopic(), msg);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Mqtt callback
   */
  @Override
  public void connectionLost(Throwable t) {
    t.printStackTrace();
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken t) {
    // ignore?
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    // parse message payload as JSON object and send as OSGi event
    try {
      Map<String, Object> eventMap = dtos.decoder(Map.class).get(new ByteArrayInputStream(message.getPayload()));

      // tag with the sender id to filter out on receiving messages
      String sender = (String) eventMap.get("_sender");
      if (sender != null && sender.equals(id)) {
        return;
      } else {
        eventMap.put("_sender", id);
      }

      Event event = new Event(topic, eventMap);
      System.out.println("MQTT Event received! Topic = " + topic);
      ea.sendEvent(event);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
