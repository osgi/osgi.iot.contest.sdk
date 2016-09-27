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

        String TrainController_target();
    }

    static Logger logger = LoggerFactory.getLogger(ExampleTrainManagerImpl.class);

    TrainController trainController;

    // TrainController.target is set in config
    @Reference(name = "TrainController")
    void setTrainController(TrainController trainCtrl) {
        trainController = trainCtrl;
    }

    @Reference
    private TrackForTrain trackManager;

    @Reference
    private Scheduler scheduler;

    private Tracks<Object> tracks;
    private String name;
    private String rfid;
    private int speed;
    private int lastMove = 0;

    private Thread mgmtThread;

    @Activate
    public void activate(Config config) throws Exception {
        name = config.name();
        rfid = config.rfid();
        speed = config.speed();
        info("activate: speed<{}> rfid<{}>", speed, rfid);

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
        stop();
    }

    private void move(int moveSpeed) {
        if (lastMove != moveSpeed || moveSpeed == 0) {
            info("move({})", moveSpeed);
            lastMove = moveSpeed;

            trainController.move(moveSpeed);
        }
    }

    private void stop() {
        move(0);
        light(false);
    }

    private void light(boolean on) {
        trainController.light(on);
    }

    private class TrainMgmtLoop implements Runnable {

        private String currentAssignment = null;
        private String currentLocation = null;
        private String currentAccess = null;
        private LinkedList<SegmentHandler<Object>> route = null;

        private void abort() {
            stop();
            currentAssignment = null;
            route = null;
        }

        @Override
        public void run() {
            // get observations, without blocking, to find lastObservation
            // and to avoid re-processing same observations on restart
            List<Observation> observations = trackManager.getRecentObservations(-2);
            long lastObsId = -1;
            boolean blocked = false;

            if (!observations.isEmpty()) {
                Observation lastObs = observations.get(observations.size() - 1);
                lastObsId = lastObs.id;
                info("disgard old observations: last: {}", lastObs);
            }

            stop();

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
                        info("New Assignment<{}>", o.assignment);
                        currentAssignment = o.assignment;

                        if (currentLocation == null) {
                            info("start moving to find location");
                            move(speed);
                        } else {
                            planRoute();
                            blocked = followRoute();
                        }
                        break;

                    case LOCATED:
                        info("Located @ {}", o.segment);

                        if (currentLocation == null && currentAssignment != null) {
                            currentLocation = o.segment;
                            move(0);
                            planRoute();
                        } else {
                            currentLocation = o.segment;
                        }

                        // stop when assignment reached
                        if (currentAssignment == null) {
                            info("Waiting for an assignment");
                            stop();
                        } else if (currentAssignment.equals(currentLocation)) {
                            info("Reached assignment<{}>", currentAssignment);
                            currentAssignment = null;
                            stop();
                            blink(3);
        					trackManager.assignmentReached(name, currentLocation);
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
            light(false);
            if (n > 0) {
                scheduler.after(() -> {
                    light(true);
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

            light(true);

            Optional<SegmentHandler<Object>> mySegment = route.stream()
                    .filter(sh -> sh.segment.id.equals(currentLocation))
                    .findFirst();

            if (!mySegment.isPresent()) {
                error("location<{}> is not on route. stop.", currentLocation);
                abort();
                return false;
            }

            Optional<SegmentHandler<Object>> nextLocator;
            String fromTrack;
            String toTrack;

            // update the remaining part of the current route
            while (route.size() > 0 && !route.getFirst().segment.id.equals(currentLocation)) {
                SegmentHandler<Object> removed = route.removeFirst();
                if (removed.isLocator()) {
                    info("eek! missed locator <{}>", removed.segment.id);
                    nextLocator = route.stream().filter(sh -> sh.isLocator()).findFirst();

                    if (nextLocator.isPresent()) {
                        fromTrack = removed.getTrack();
                        toTrack = nextLocator.get().getTrack();
                        if (!fromTrack.equals(toTrack)) {
                            info("retrospectively request access to <{}> from <{}>", toTrack, fromTrack);
                            move(0);
                            requestAccess(fromTrack, toTrack);
                        }
                    }
                }
            }

            // figure out where to go to next
            fromTrack = route.removeFirst().getTrack();

            // check if we have to go to a new track before we have a new
            // Locator
            nextLocator = route.stream().filter(sh -> sh.isLocator()).findFirst();

            if (!nextLocator.isPresent()) {
                error("no locator to go to, stop now");
                abort();
                return false;
            }

            toTrack = nextLocator.get().getTrack();

            // if we have to go to other track, request access
            if (!fromTrack.equals(toTrack) && !toTrack.equals(currentAccess)) {
                info("stop and request access to track<{}> from <{}>", toTrack, currentLocation);
                move(0);

                boolean granted = false;

                // simply keep on trying until access is given
                while (!granted && isActive()) {
                    granted = requestAccess(fromTrack, toTrack);

                    if (!granted) {
                        // allow mgmt loop to process other events
                        // return true;
                    }
                }
            }

            // just go forward
            move(speed);
            return false;
        }

        private boolean requestAccess(String fromTrack, String toTrack) {
            boolean granted = false;

            try {
                granted = trackManager.requestAccessTo(name, fromTrack, toTrack);
            } catch (Exception e) {
                error("request access failed: " + e);
                abort();
            }

            info("access is {}", granted ? "granted" : "blocked");
            currentAccess = (granted ? toTrack : null);

            return granted;
        }

        private boolean isActive() {
            return !Thread.currentThread().isInterrupted();
        }
    }

    private void info(String fmt, Object... args) {
        String ident = String.format("Train<%s>: ", name);
        System.out.printf(ident + fmt.replaceAll("\\{}", "%s") + "\n", args);
    }

    private void error(String fmt, Object... args) {
        String ident = String.format("Train<%s>: ", name);
        System.err.printf("ERROR: " + ident + fmt.replaceAll("\\{}", "%s") + "\n", args);
        logger.error(ident + fmt, args);
    }

}
