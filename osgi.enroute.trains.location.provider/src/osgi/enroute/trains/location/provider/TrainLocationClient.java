package osgi.enroute.trains.location.provider;

import org.osgi.service.component.annotations.Component;

@Component(
        immediate = true)
public class TrainLocationClient {

	// TODO this should listen for Bluetooth events and send them out over MQTT to replace Walt's old kura-based code
	
	// Should reuse the configuration with the code 2 tag map
}
