package osgi.enroute.trains.emulator.provider;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;
import osgi.enroute.trains.train.api.TrainController;

public class TrainControllerImpl implements TrainController, EventHandler {

  private double                               distance;
  private int                                  desiredSpeed;
  private int                                  actualSpeed;
  private int                                  lastSpeed;
  private Traverse                             current;
  private String                               rfid;
  private ServiceRegistration<TrainController> registration;
  private EmulatorImpl                         emulatorImpl;
  private String                               name;
  private final Random                         random = new Random();
  private ServiceRegistration<EventHandler>    regEvent;

  private double                               rfidProb;

  private double                               playSpeed;

  public TrainControllerImpl(String name, String rfid, double rfidProb, double playSpeed,
      SegmentHandler<Traverse> start, EmulatorImpl emulatorImpl) {
    this.rfid = rfid;
    this.playSpeed = playSpeed / 10;
    this.name = name;
    this.rfidProb = rfidProb;
    this.emulatorImpl = emulatorImpl;
    this.current = start.get();
  }

  @Override
  public void move(int directionAndSpeed) {
    this.desiredSpeed = directionAndSpeed;
  }

  @Override
  public void light(boolean on) {
  }

  void tick() {
    try {
      if (current == null)
        return;

      actualSpeed = desiredSpeed + (desiredSpeed - actualSpeed + 2) / 4;

      if (actualSpeed != lastSpeed) {
        emulatorImpl.observation(Observation.Type.EMULATOR_TRAIN_SPEED, name, current.getSegment().id,
            actualSpeed);
        lastSpeed = actualSpeed;
      }

      String rfid = this.rfid;

      double chance = (rfidProb - actualSpeed) / 100D;
      if (random.nextDouble() > chance) {
        rfid = null;
      }

      distance += playSpeed * actualSpeed;

      double l = current.l();
      if (distance > l) {
        current = current.next(rfid);
        distance -= l;

        emulatorImpl.observation(Observation.Type.EMULATOR_TRAIN_MOVES, name, current.getSegment().id,
            actualSpeed);

      } else if (distance < 0) {
        current = current.prev(rfid);
        distance = 0;
        emulatorImpl.observation(Observation.Type.EMULATOR_TRAIN_MOVES, name, current.getSegment().id,
            actualSpeed);
      } else {
        // System.out.println("->" + current + " " + l + " " +
        // distance);

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void close() {
    registration.unregister();
    regEvent.unregister();
  }

  public void register(BundleContext context, String channel) {
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put("train.rfid", rfid);
    properties.put("train.name", name);
    properties.put("channel", channel);
    registration = context.registerService(TrainController.class, this, properties);

    String[] topics = new String[] {
        "osgi/trains/trainlight"
    };
    Hashtable ht = new Hashtable();
    ht.put(EventConstants.EVENT_TOPIC, topics);
    regEvent = context.registerService(EventHandler.class, this, ht);
  }

  /* (non-Javadoc)
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event event) {
    if (event.getProperty("trainname").equals(name)) {
      if (event.getProperty("lights").equals("true")) {
        light(true);
        System.out.println("Turning on light! Train=" + event.getProperty("trainname"));
      } else {
        light(false);
        System.out.println("Turning off light! Train=" + event.getProperty("trainname"));
      }
    }
  }

}
