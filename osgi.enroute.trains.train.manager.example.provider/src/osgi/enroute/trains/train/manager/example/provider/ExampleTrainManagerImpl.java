package osgi.enroute.trains.train.manager.example.provider;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.TrackForTrain;
import osgi.enroute.trains.track.util.Tracks;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;
import osgi.enroute.trains.train.api.TrainController;
import osgi.enroute.trains.train.manager.example.provider.ExampleTrainManagerImpl.Config;

/**
 * Train manager.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = Config.TRAIN_CONFIG_PID, configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true, service = Object.class)
public class ExampleTrainManagerImpl {

    @ObjectClassDefinition
    @interface Config {
        final static public String TRAIN_CONFIG_PID = "osgi.enroute.trains.train.manager";

        String name();

        String rfid();

        int speed() default 50;

        String target_TrainController();
    }

    static Logger logger = LoggerFactory.getLogger(ExampleTrainManagerImpl.class);

    // TrainController.target is set in config
    @Reference(name = "TrainController")
    private TrainController trainCtrl;

    @Reference
    private TrackForTrain trackManager;

    @Reference
    private Scheduler scheduler;

    private Tracks<Object> tracks;
    private String name;
    private String rfid;
    private int speed;

    private Thread mgmtThread;

    @Activate
    public void activate(Config config) throws Exception {
        name = config.name();
        rfid = config.rfid();
        speed = config.speed();
        info("activate: {} speed={}", name, speed);
        
        // register train with Track Manager
        trackManager.registerTrain(name, rfid);

        // create Track
        tracks = new Tracks<Object>(trackManager.getSegments().values(), new TrainManagerFactory());

        mgmtThread = new Thread(new TrainMgmtLoop());
        mgmtThread.start();
    }

    @Deactivate
    public void deactivate() {
        info("deactivate: {}", name);
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

    private class TrainMgmtLoop implements Runnable {

        private String currentAssignment = null;
        private String currentLocation = null;
        private LinkedList<SegmentHandler<Object>> route = null;

        @Override
        public void run() {
            // get all recent observations, without blocking
            // to determine lastObservation and to avoid re-processing same
            // observations on restart
            List<Observation> observations = trackManager.getRecentObservations(-1);
            long lastObsId = -1;
            boolean blocked = false;

            if (!observations.isEmpty()) {
                Observation lastObs = observations.get(observations.size() - 1);
                lastObsId = lastObs.id;
                info("disgarding old observations: last<{}>", lastObs);
            }

            trainCtrl.move(0);
            trainCtrl.light(false);

            while (isActive()) {

                observations = trackManager
                        .getRecentObservations(lastObsId - (blocked ? 1 : 0));

                if (blocked && observations.size() > 1) {
                    // remove the observation that caused block and process next
                    // otherwise re-process previous observation, which should
                    // drop back to followRoute()
                    observations.remove(0);
                    blocked = false;
                }

                for (Observation o : observations) {
                    lastObsId = o.id;

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
                            trainCtrl.move(speed);
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
                }, 500);
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
                        trainCtrl.move(speed);
                    }

                    if (!access) {
                        return true;
                    }
                }
            }

            // just go forward
            trainCtrl.move(speed);
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
        System.out.printf("TrainMgr: " + fmt.replaceAll("\\{}", "%s") + "\n", args);
        logger.info(fmt, args);
    }

}
