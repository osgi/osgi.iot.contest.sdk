package osgi.enroute.trains.train.manager.example.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import osgi.enroute.trains.cloud.api.Observation.Type;
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

    private Set<String> darkSegments = new HashSet<>();
    
    private Thread mgmtThread;
    private boolean waiting = false;
    
    private boolean emergency = false;
    private Set<String> emergencies = new HashSet<>();

    @Activate
    public void activate(Config config) throws Exception {
        name = config.name();
        rfid = config.rfid();
        speed = config.speed();
        
        if (name ==  null || rfid == null) {
            throw new IllegalArgumentException("name=" + name + " rfid=" + rfid);
        }

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

            if (!observations.isEmpty()) {
                Observation lastObs = observations.get(observations.size() - 1);
                lastObsId = lastObs.id;
                info("disgard old observations: last: {}", lastObs);
            }

            stop();

            while (isActive()) {
                List<Observation> myObs = new ArrayList<>();
                Observation lastLocated = null;
                observations = trackManager.getRecentObservations(lastObsId);
                
                for (int i = 0; i < observations.size(); i++) {
                    Observation o = observations.get(i);
                    lastObsId = o.id;

                    tracks.event(o);

                    if (o.type == Type.DARK) {
                    	if (o.dark) {
                    		darkSegments.add(o.segment);
                    	} else {
                    		darkSegments.remove(o.segment);
                    	}
                    }
                    
                    // populate myObs for our train, with last LOCATED observation first
                    if (name.equals(o.train)) {
//                        if (o.type == Type.LOCATED) {
//                           lastLocated = o; 
//                        }
//                        else {
                            myObs.add(o);
//                        }
                    }
                }
                
                if (lastLocated != null) {
                    myObs.add(0, lastLocated);
                }

                for (Observation o : myObs) {
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
                            followRoute();
                        }
                        break;

                    case LOCATED:
                        //info("Located @ {}", o.segment);

                        if (currentLocation == null && currentAssignment != null) {
                            currentLocation = o.segment;
                            move(0);
                            planRoute();
                            followRoute();
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
                            if(!followRoute()){
                            	// if cannot follow (e.g. got off route), plan new route
                            	planRoute();
                            	followRoute();
                            }
                        }
                        break;

                    case EMERGENCY:
                    	emergency = o.emergency;
                    	if(emergency){
                    		emergencies.add(o.message);
                    	} else {
                    		emergencies.remove(o.message);
                    	}
                    	
                    	if(emergencies.size() == 0){
                    		// no more emergencies, follow route
                    		followRoute();
                    	} else {
                    		stop();
                    		blink(3);
                    	}
                    	break;

                    default:
                        break;
                    }
                }
                
                if(!emergency && waiting){
                	followRoute();
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
            if (route == null || route.isEmpty() || emergency)
                return true;

            Optional<SegmentHandler<Object>> mySegment = route.stream()
                    .filter(sh -> sh.segment.id.equals(currentLocation))
                    .findFirst();

            if (!mySegment.isPresent()) {
                info("location<{}> is not on route?", currentLocation);
                move(0);
                return false;
            }

            String fromTrack = mySegment.get().getTrack();
            String toTrack = null;

            // remove the part of the route we have covered
            while (route.size() > 0 && !route.getFirst().segment.id.equals(currentLocation)) {
                SegmentHandler<Object> removed = route.removeFirst();
            }

            if(route.size() <= 1){
            	info("location<{}> is end of the route?", currentLocation);
                abort();
                return true;
            }
            
            boolean dark = false;
            
            // figure out where to go to next - check next segments
            for(int i=0; i < 8 ;i++){
            	if(route.size() > i){
            		if(!(route.get(i).isSwitch() || route.get(i).isMerge())){ // switches don't have a track
            			toTrack = route.get(i).getTrack();
            		}
            		
            		if(darkSegments.contains(route.get(i).segment.id)){
            			dark = true;
            		}
            	}
            }
            
            // if there is a dark segment upcoming, turn the lights on
            if(dark){
            	light(true);
            } else {
            	light(false);
            }
            
            // if we have to go to other track, request access
            info("from={} to={} access={} location={}", fromTrack, toTrack, currentAccess, currentLocation);
            if (!fromTrack.equals(toTrack) && !toTrack.equals(currentAccess)) {
                info("stop and request access to track<{}> from <{}>", toTrack, currentLocation);
                move(0);

                if(!requestAccess(fromTrack, toTrack)){
                	// don't wait in a loop here, this allows other events to be processed meanwhile
                	waiting = true;
                	return true;
                } else {
                	waiting = false;
                }
            }

            // just go forward
            move(speed);
            return true;
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
