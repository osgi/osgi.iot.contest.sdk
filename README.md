<h1><img src="http://enroute.osgi.org/img/enroute-logo-64.png" witdh=40px style="float:left;margin: 0 1em 1em 0;width:40px">

OSGi IoT Trains Demo 2017</h1>

For the 2017 demo, we extend the demo to have trains transporting goods. These goods are tracked using wireless sensors (by Bosch), and can be loaded on and off a train using a robotic arm (by imec). On top of that, the demo will showcase some of the upcoming R7 specs and RFC work being done in the OSGi Alliance. 

## Architecture overview

The overall architecture has been slightly changed in comparison with previous years. As opposed to last years, now all messages and (remote) commands are published/consumed via MQTT. To facilitate this, an MQTTService is available according to the current RFC design (RFC-229). The payload of the MQTTMessages is a json representation of the DTO types used for messages/commands.

* trains.controller.segment : does low level control of the signals/switches. Listens for `SegmentCommand`s on the `osgi/trains/command/segment` topic. This bundle runs on a Raspberry Pi connected to the signal/switch. Signal colors can be set to `RED`,`GREEN` or `YELLOW`. A switch can be put in normal or alternate state. When a signal or a switch change status, they send out a corresponding `Observation` message on the `osgi/trains/observation` topic.

* trains.controller.train : does the low level control of a train. Listens for `TrainCommand`s on the `osgi/trains/command/train` topic. There are two possible actions: move the train with a certain direction and speed (zero speed to stop the train) or turn the train lights on/off. This bundle runs on a Raspberry Pi that has an infrared signal sender connected.

* trains.location.provider : publishes train locations. Listens to codes from the RFID scanner on the train that are sent over via Bluetooth. These are then translated to `Observation` messages sent on the `osgi/trains/observation` topic. Note: the RFID is not 100% reliable so one can miss observations of segments. In order for this to work, the device this bundle runs on should be paired and connected with the Bluetooth sender on the train.

* trains.manager.track : listens to `Observation`s and maintains a state of the tracks. Offers a `TrackManager` service that is used by the `TrainManager` to request access to a track for a train.

* trains.manager.trains : controls a train moving to a certain `Assignment`, tracking its location, calculating a route, and requesting access to the `TrackManager` to the correct tracks. Listens to `Assignment` commands on the `osgi/trains/assignment` topic, and publishes on the same topic when an assignment is reached or aborted.

All DTO classes for the messages can be found in the api bundle.

## Listening / publishing to MQTT via MQTTService

Publishing to MQTT can be done as follows:

```
	@Reference
	protected MQTTService mqtt;

	@Reference
	protected Converter converter;

	public void publishObservation(){		
		Observation o = new Observation();
		o.time = System.currentTimeMillis();
		o.type = Observation.Type.SWITCH;
		o.segment = segment;
		o.alternate = alt;
		mqtt.publish(Observation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));		
	}
```

In our DS component we get a reference to the MQTTService and Converter service. We then create an instance of the DTO class of the message we want to send. To actually publish this we use the `publish()` method of the MQTTService, that accepts a topic and Bytebuffer payload. To convert the DTO to a byte[] we use the Converter service.

Similarly to listen for messages, we subscribe to the PushStream provided by the MQTTService:

```
	@Reference
	protected MQTTService mqtt;
	
	@Reference
	protected Converter converter;
	
	@Activate
	void activate() throws Exception{
		// listen for SegmentCommands
			mqtt.subscribe(SegmentCommand.TOPIC).forEach(msg ->{
				SegmentCommand c = converter.convert(msg.payload().array()).to(SegmentCommand.class);
				switch(c.type){
					// do something witht he command
				}
			});
		
	}
```

When the MQTTMessage is received, we first convert it back to a type-safe DTO type using the Converter.


## Running the demo

To run the demo, the trains.config project contains a couple of .bndrun files and configurations. The configurations are written according to the R7 Configurator specification. We also require an MQTT server to be running in the network, who's uri is configured via the various json configuration files.


