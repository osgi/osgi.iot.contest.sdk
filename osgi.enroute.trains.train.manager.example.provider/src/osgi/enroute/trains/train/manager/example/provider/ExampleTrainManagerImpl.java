package osgi.enroute.trains.train.manager.example.provider;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.TrackForTrain;
import osgi.enroute.trains.track.util.Tracks;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;
import osgi.enroute.trains.train.api.TrainConfiguration;
import osgi.enroute.trains.train.api.TrainController;

/**
 * 
 */
@Component(name = TrainConfiguration.TRAIN_CONFIGURATION_PID, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = Object.class)
public class ExampleTrainManagerImpl {
	static Logger logger = LoggerFactory.getLogger(ExampleTrainManagerImpl.class);

	private TrackForTrain trackManager;
	private TrainController train;
	private String name;
	private String rfid;
	private Tracks<Object> tracks;

	private Thread mgmtThread;

	@Activate
	public void activate(TrainConfiguration config) throws Exception {
		name = config.name();
		rfid = config.rfid();

		// register train with Track Manager
		trackManager.registerTrain(name, rfid);
		// create Track
		tracks = new Tracks<Object>(trackManager.getSegments().values(), new TrainManagerFactory());

		mgmtThread = new Thread(new TrainMgmtLoop());
		mgmtThread.start();
	}

	@Deactivate
	public void deactivate() {
		try {
			mgmtThread.interrupt();
			mgmtThread.join(5000);
		} catch (InterruptedException e) {
		}
		// stop when deactivated
		train.move(0);
		// turn lights off
		train.light(false);
	}

	@Reference
	public void setTrainController(TrainController t) {
		this.train = t;
	}

	@Reference
	public void setTrackManager(TrackForTrain t) {
		this.trackManager = t;
	}

	private class TrainMgmtLoop implements Runnable {

		private String currentAssignment = null;
		private String currentLocation = null;
		private LinkedList<SegmentHandler<Object>> route = null;

		@Override
		public void run() {
			// turn the train light on
			train.light(true);
			// start moving on activation
			train.move(50);

			// last observation id
			long lastObservation = -1;

			while (isActive()) {
				// mgmt loop
				List<Observation> observations = trackManager.getRecentObservations(lastObservation);
				for (Observation o : observations) {
					lastObservation = o.id;

					tracks.event(o);

					if (name == null || !name.equals(o.train)) {
						continue;
					}

					switch (o.type) {
					case ASSIGNMENT:
						currentAssignment = o.assignment;
						// new assignment, plan and follow the route
						logger.info(name + "/" + rfid + " gets new assignment " + o.assignment);
						planRoute();
						followRoute();
						break;
					case LOCATED:
						currentLocation = o.segment;

						// if first time location found and already an
						// assignment is set,
						// plan route
						if (currentLocation == null && currentAssignment != null) {
							planRoute();
						}

						// stop current assignment reached (no assignment =
						// assignment reached)
						if (assignmentReached()) {
							train.move(0);
						} else {
							followRoute();
						}
						break;
					case BLOCKED:
						break;
					case CHANGE:
						break;
					case SIGNAL:
						break;
					case SWITCH:
						break;
					case TIMEOUT:
						break;
					default:
						break;
					}
				}
			}
			System.out.println("Train manager exited");
		}

		private void planRoute() {
			if (currentLocation == null)
				return;

			if (currentAssignment == null)
				return;

			// plan the route
			SegmentHandler<Object> src = tracks.getHandler(currentLocation);
			SegmentHandler<Object> dest = tracks.getHandler(currentAssignment);
			route = src.findForward(dest);
		}

		private void followRoute() {
			if (route == null || route.isEmpty())
				return;

			System.out.println("XX currentLocation=" + currentLocation);
			boolean found = false;
			for (SegmentHandler<Object> s : route) {
				System.out.println("XX route element=" + s);
				if (s.segment.id.equals(currentLocation)) {
					found = true;
				}
			}

			if (found) {
				// update the remaining part of the current route
				while (route.size() > 0 && !route.getFirst().segment.id.equals(currentLocation)) {
					route.removeFirst();
				}
			}

			// figure out where to go to next
			String fromTrack = route.removeFirst().getTrack();

			// check if we have to go to a new track before we have a new
			// Locator
			Optional<SegmentHandler<Object>> nextLocator = route.stream().filter(sh -> sh.isLocator()).findFirst();
			if (!nextLocator.isPresent()) {
				// no locator to go to, stop now
				train.move(0);
				return;
			}

			String toTrack = nextLocator.get().getTrack();

			// check if we have to go to other track, in that case request
			// access
			if (!fromTrack.equals(toTrack)) {
				// stop and request access
				train.move(0);

				boolean access = false;
				// simply keep on trying until access is given
				while (!access && isActive()) {
					logger.info(name + " requests access from track " + fromTrack + " to " + toTrack);
					try {
						access = trackManager.requestAccessTo(rfid, fromTrack, toTrack);
					} catch (Exception e) {
						currentLocation = null;
						train.move(40);
					}
				}
			}

			// just go forward
			train.move(50);
		}

		private boolean isActive() {
			return !Thread.currentThread().isInterrupted();
		}

		private boolean assignmentReached() {
			if (currentAssignment == null || currentAssignment.equals(currentLocation)) {
				if (currentAssignment != null) {
					logger.info(name + " has reached assignment " + currentAssignment);
				} else {
					logger.info(name + " is waiting for an assignment");
				}
				return true;
			}
			return false;
		}
	}

	// make train move from gogo shell command
	public void move(int directionAndSpeed) {
		this.train.move(directionAndSpeed);
	}

}
