package osgi.enroute.trains.application;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import osgi.enroute.configurer.api.RequireConfigurerExtender;
import osgi.enroute.eventadminserversentevents.capabilities.RequireEventAdminServerSentEventsWebResource;
import osgi.enroute.github.angular_ui.capabilities.RequireAngularUIWebResource;
import osgi.enroute.google.angular.capabilities.RequireAngularWebResource;
import osgi.enroute.jsonrpc.api.JSONRPC;
import osgi.enroute.jsonrpc.api.RequireJsonrpcWebResource;
import osgi.enroute.stackexchange.pagedown.capabilities.RequirePagedownWebResource;
import osgi.enroute.trains.application.LayoutAdapter.Layout;
import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.cloud.api.TrackForCommand;
import osgi.enroute.trains.cloud.api.TrackForSegment;
import osgi.enroute.trains.passenger.api.Passenger;
import osgi.enroute.trains.passenger.api.Person;
import osgi.enroute.trains.passenger.api.PersonDatabase;
import osgi.enroute.trains.stations.api.Station;
import osgi.enroute.trains.stations.api.StationsManager;
import osgi.enroute.trains.track.util.Tracks;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;
import osgi.enroute.trains.train.api.TrainController;
import osgi.enroute.twitter.bootstrap.capabilities.RequireBootstrapWebResource;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

@RequireAngularWebResource(resource = { "angular.js", "angular-resource.js", "angular-route.js" }, priority = 1000)
@RequireBootstrapWebResource(resource = "css/bootstrap.css")
@RequireAngularUIWebResource(resource="ui-bootstrap-tpls.js")
@RequireWebServerExtender
@RequireConfigurerExtender
@RequireEventAdminServerSentEventsWebResource
@RequireJsonrpcWebResource
@RequirePagedownWebResource(resource="enmarkdown.js")
@Component(name = "osgi.enroute.trains", property = JSONRPC.ENDPOINT + "=trains")
public class TrainsApplication implements JSONRPC {
	final Map<String,String> name2rfid = new ConcurrentHashMap<>();
	
	@Reference
	private TrackForCommand ti;
	@Reference
	private TrackForSegment ts;
	
	@Reference
	private StationsManager stations;
	
	@Reference
	private PersonDatabase pdb;
	
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

	public List<Station> getStations(){
		security();
		return stations.getStations();
	}
	
	public List<Passenger> getPassengersInStation(String station){
		security();
		return stations.getPassengersWaiting(station);
	}
	
	public List<Passenger> getPassengersOnTrain(String train){
		security();
		return stations.getPassengersOnTrain(train);
	}
	
	public List<String> getTrains() {
		security();
		return ti.getTrains();
	}
	
	public Person getPerson(String id){
		security();
		return pdb.getPerson(id);
	}
	
	public Person getPerson(String firstName, String lastName){
		security();
		try {
			return pdb.getPersons().stream().filter(p -> p.firstName.equals(firstName) && p.lastName.equals(lastName)).findFirst().get();
		} catch(NoSuchElementException e){
			return pdb.register(null, firstName, lastName);
		}
	}
	
	public List<String> getFirstNames(String firstName, String lastName){
		security();
		return pdb.getPersons().stream().filter(p -> (firstName == null || p.firstName.startsWith(firstName)) 
												&& (lastName==null || p.lastName.startsWith(lastName))).map(p -> p.firstName).distinct().limit(10).collect(Collectors.toList());
	}

	public List<String> getLastNames(String firstName, String lastName){
		security();
		return pdb.getPersons().stream().filter(p -> (firstName == null || p.firstName.startsWith(firstName)) 
												&& (lastName==null || p.lastName.startsWith(lastName))).map(p -> p.lastName).distinct().limit(10).collect(Collectors.toList());
	}
	
	public void assign(String train, String segment) {
		security();
		ti.assign(train, segment);;
	}

	public void checkIn(String firstName, String lastName, String station, String destination){
		security();
		Person p = getPerson(firstName, lastName);
		stations.checkIn(p.id, station, destination);
	}
	
	public void checkIn(String personId, String station, String destination){
		security();
		stations.checkIn(personId, station, destination);
	}
	
	public Map<String,SegmentPosition> getPositions() {
		security();
		return positions;
	}
	
	public void setPosition( String train, String segment) {
		security();
		ts.locatedTrainAt(name2rfid.get(train), segment);
	}

	private void security() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getDescriptor() throws Exception {
		return null;
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	void addTrain(TrainController tm, Map<String,Object> map) {
		name2rfid.put( (String)map.get("train.name"), (String)map.get("train.rfid"));
	}
	
	void removeTrain(TrainController tm) {
		
	}

}
