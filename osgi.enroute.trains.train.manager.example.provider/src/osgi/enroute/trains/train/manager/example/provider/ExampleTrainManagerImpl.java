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

import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.TrackForTrain;
import osgi.enroute.trains.track.util.Tracks;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;
import osgi.enroute.trains.train.api.TrainConfiguration;
import osgi.enroute.trains.train.api.TrainController;

/**
 * 
 */
@Component(name = TrainConfiguration.TRAIN_CONFIGURATION_PID, configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true, service = Object.class)
public class ExampleTrainManagerImpl {
    static Logger logger = LoggerFactory.getLogger(ExampleTrainManagerImpl.class);

    private TrackForTrain trackManager;
    private TrainController trainCtrl;
    private String name;
    private String rfid;
    private Tracks<Object> tracks;

    private Thread mgmtThread;

    @Reference
    private Scheduler scheduler;

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
        info("deactivate");
        try {
            mgmtThread.interrupt();
            mgmtThread.join(5000);
        } catch (InterruptedException e) {
        }
        // stop when deactivated
        trainCtrl.move(0);
        // turn lights off
        trainCtrl.light(false);
    }

    @Reference
    public void setTrainController(TrainController t) {
        this.trainCtrl = t;
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
            long lastObservation = -1;
            boolean blocked = false;

            while (isActive()) {

                List<Observation> observations = trackManager
                        .getRecentObservations(lastObservation - (blocked ? 1 : 0));

                if (blocked && observations.size() > 1) {
                    // remove the observation that caused block and process next
                    // otherwise re-process previous observation, which should
                    // drop back to followRoute()
                    observations.remove(0);
                    blocked = false;
                }

                for (Observation o : observations) {
                    lastObservation = o.id;

                    tracks.event(o);

                    if (name == null || !name.equals(o.train)) {
                        continue;
                    }

                    switch (o.type) {
                    case ASSIGNMENT:
                        // new assignment, plan and follow the route
                        currentAssignment = o.assignment;
                        info("Assignment: {} -> {}", name, currentAssignment);

                        if (currentLocation == null) {
                            // start moving to find location
                            trainCtrl.move(50);
                            trainCtrl.light(true);
                        } else {
                            planRoute();
                            blocked = followRoute();
                        }
                        break;

                    case LOCATED:
                        // if first time location found and an assignment is
                        // already set plan route
                        if (currentLocation == null && currentAssignment != null) {
                            currentLocation = o.segment;
                            planRoute();
                        } else {
                            currentLocation = o.segment;
                        }

                        // stop current assignment reached (no assignment =
                        // assignment reached)
                        if (assignmentReached()) {
                            trainCtrl.move(0);
                            trainCtrl.light(false);
                            blink(3);
                        } else {
                            blocked = followRoute();
                        }
                        break;

                    case BLOCKED:
                    case CHANGE:
                    case SIGNAL:
                    case SWITCH:
                    case TIMEOUT:
                    default:
                        break;
                    }
                }
            }
            info("management loop terminated.");
        }

        private void blink(int n) {
            trainCtrl.light(false);
            if (n > 0) {
                scheduler.after(() -> {
                    trainCtrl.light(true);
                    scheduler.after(() -> blink(n - 1), 500);
                } , 500);
            }
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

        private boolean followRoute() {
            if (route == null || route.isEmpty())
                return false;

            trainCtrl.light(true);

            boolean found = false;
            for (SegmentHandler<Object> s : route) {
                if (s.segment.id.equals(currentLocation)) {
                    found = true;
                    break;
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
                trainCtrl.move(0);
                return false;
            }

            String toTrack = nextLocator.get().getTrack();

            // check if we have to go to other track, in that case request
            // access
            if (!fromTrack.equals(toTrack)) {
                // stop and request access
                trainCtrl.move(0);

                boolean access = false;
                // simply keep on trying until access is given
                while (!access && isActive()) {
                    try {
                        access = trackManager.requestAccessTo(name, fromTrack, toTrack);
                    } catch (Exception e) {
                        currentLocation = null;
                        trainCtrl.move(40);
                    }

                    if (!access) {
                        return true;
                    }
                }
            }

            // just go forward
            trainCtrl.move(50);
            return false;
        }

        private boolean isActive() {
            return !Thread.currentThread().isInterrupted();
        }

        private boolean assignmentReached() {
            if (currentAssignment == null || currentAssignment.equals(currentLocation)) {
                if (currentAssignment != null) {
                    info(name + " has reached assignment " + currentAssignment);
                } else {
                    info(name + " is waiting for an assignment");
                }
                return true;
            }
            return false;
        }
    }

    private static void info(String fmt, Object... args) {
        System.out.printf("Train: " + fmt.replaceAll("\\{}", "%s") + "\n", args);
        logger.info(fmt, args);
    }

}
