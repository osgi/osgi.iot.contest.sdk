package osgi.enroute.trains.stations.api;


// TODO do we need a separate Station interface?

// could for example poll the TrainManagers to get the next arriving train based on their schedules?
public interface Station {

	String getName();
	
	String getSegment();
	
}
