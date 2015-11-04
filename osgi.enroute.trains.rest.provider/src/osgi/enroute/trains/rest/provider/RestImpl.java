package osgi.enroute.trains.rest.provider;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.rest.api.REST;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.TrackForSegment;

/**
 * 
 */
@Component(name = "osgi.enroute.trains.rest", immediate=true)
public class RestImpl implements REST {

	@Reference
	TrackForSegment ts;
	public List<Observation> getObservations(long time) {
		return ts.getRecentObservations(time);
	}
	
	public boolean getBlocked(String segment, String reason,boolean blocked) {
		ts.blocked(segment, reason, blocked);
		return true;
	}
}
