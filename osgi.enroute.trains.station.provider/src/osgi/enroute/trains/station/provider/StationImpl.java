package osgi.enroute.trains.station.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

import osgi.enroute.trains.stations.api.Station;
import osgi.enroute.trains.stations.api.StationConfiguration;

@Designate(ocd = StationConfiguration.class, factory = true)
@Component(name = StationConfiguration.STATION_CONFIGURATION_PID, immediate = true, 
		property = { "service.exported.interfaces=*"},
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class StationImpl implements Station {

	private String name;
	private String segment;
	
	@Activate
	void activate(StationConfiguration config) {
		name = config.name();
		segment = config.segment();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSegment() {
		return segment;
	}

}
