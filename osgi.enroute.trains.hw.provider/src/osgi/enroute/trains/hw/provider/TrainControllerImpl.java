package osgi.enroute.trains.hw.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.iot.lego.adapter.Channel;
import osgi.enroute.iot.lego.adapter.LegoRC;
import osgi.enroute.iot.pi.lirc.provider.LIRCImpl;
import osgi.enroute.trains.hw.provider.TrainControllerImpl.Config;
import osgi.enroute.trains.train.api.TrainController;

/**
 *  Train controller
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "osgi.enroute.trains.hw.train", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
    "service.exported.interfaces=*",
    EventConstants.EVENT_TOPIC + "=" + "osgi/trains/trainlight"
})
public class TrainControllerImpl extends LegoRC implements TrainController, EventHandler {

  @ObjectClassDefinition
  @interface Config {
    Channel channel() default Channel.CH1;

    int divider() default 100;
  }

  private double divider;
  private Config config;

  @Activate
  void start(Config config) throws Exception {
    this.config = config;
    System.out.println("activate: " + toString());
    this.setWave(new LIRCImpl());
    super.activate(config.channel());
    this.divider = config.divider();
  }

  @Deactivate
  void stop() {
    System.out.println("deactivate: " + toString());
  }

  @Override
  public String toString() {
    return "TrainController[channel=" + config.channel() + ", divider=" + config.divider() + "]";
  }

  @Override
  public void move(int directionAndSpeed) {
    try {
      Double speed = directionAndSpeed / divider;
      A(speed);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void light(boolean on) {
    try {
      B(on ? 1D : 0D);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /* (non-Javadoc)
  * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
  */
  @Override
  public void handleEvent(Event event) {
    if (event.getProperty("trainname").equals(getName())) {
      if (event.getProperty("lights").equals("true")) {
        light(true);
        System.out.println("Turning on light!Train=" + event.getProperty("trainname"));
      } else {
        light(false);
        System.out.println("Turning off light!" + event.getProperty("trainname"));
      }
    }
  }

}
