package osgi.enroute.trains.application;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.configurer.api.RequireConfigurerExtender;
import osgi.enroute.eventadminserversentevents.capabilities.RequireEventAdminServerSentEventsWebResource;
import osgi.enroute.google.angular.capabilities.RequireAngularWebResource;
import osgi.enroute.jsonrpc.api.JSONRPC;
import osgi.enroute.jsonrpc.api.RequireJsonrpcWebResource;
import osgi.enroute.stackexchange.pagedown.capabilities.RequirePagedownWebResource;
import osgi.enroute.trains.application.LayoutAdapter.Layout;
import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.cloud.api.TrackForCommand;
import osgi.enroute.trains.track.util.Tracks;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;
import osgi.enroute.twitter.bootstrap.capabilities.RequireBootstrapWebResource;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

@RequireAngularWebResource(resource = { "angular.js", "angular-resource.js", "angular-route.js" }, priority = 1000)
@RequireBootstrapWebResource(resource = "css/bootstrap.css")
@RequireWebServerExtender
@RequireConfigurerExtender
@RequireEventAdminServerSentEventsWebResource
@RequireJsonrpcWebResource
@RequirePagedownWebResource(resource="enmarkdown.js")
@Component(name = "osgi.enroute.trains", property = JSONRPC.ENDPOINT + "=trains")
public class TrainsApplication implements JSONRPC {

	@Reference
	private TrackForCommand ti;

	
	private Tracks<Layout> track;
	private Map<String,SegmentPosition> positions;

	@Activate
	void activate() throws Exception {
		try {
			track = new Tracks<>(ti.getSegments().values(), new LayoutAdapter());
			track.getRoot().get().layout(0, 0, null);
			for ( SegmentHandler<Layout> sh : track.getHandlers()) {
				sh.get().adjustWidth();
			}
			positions = Collections.unmodifiableMap(
					track.getHandlers().stream().map(sh -> sh.get().getPosition()).collect(Collectors.toMap( p -> ((SegmentPosition)p).segment.id, p->p)));
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Map<String, Segment> getSegments() {
		security();
		return ti.getSegments();
	}

	public List<String> getTrains() {
		security();
		return ti.getTrains();
	}
	
	public void assign(String train, String segment) {
		security();
		ti.assign(train, segment);;
	}
	
	public Map<String,SegmentPosition> getPositions() {
		return positions;
	}
	

	private void security() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getDescriptor() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
