package osgi.enroute.trains.hw.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.trains.cloud.api.TrackForSegment;
import osgi.enroute.trains.controller.api.RFIDSegmentController;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

@Designate(ocd = MicroSwitchSegmentImpl.Config.class, factory = true)
@Component(name = "osgi.enroute.trains.hw.microswitch", immediate = true, property = { "service.exported.interfaces=*", //
		Debug.COMMAND_SCOPE + "=rfid", //
		Debug.COMMAND_FUNCTION + "=lastrfid" }, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MicroSwitchSegmentImpl implements RFIDSegmentController {

	@Reference
	private GpioController gpio;
	
	@Reference
	private TrackForSegment trackManager;
	
	private String lastRFID = null;
	private Deferred<String> nextRFID = new Deferred<String>();
	
	private Config config;
	private long debounce = 0;

	@ObjectClassDefinition
	@interface Config {
		int controller_id();

		String segment();

		String pin();
	}

	@Activate
	void activate(Config config) {
		this.config = config;
		
		Pin pin = RaspiPin.getPinByName(config.pin());
		if ( pin == null) {
			System.out.println("Pin is " + config.pin() + " is null");
		}
		for (GpioPin e : gpio.getProvisionedPins()) {
			if (e.getPin().equals(pin)) {
				gpio.unprovisionPin(e);
				break;
			}
		}
		GpioPinDigitalInput microSwitch = gpio.provisionDigitalInputPin(pin, PinPullResistance.PULL_DOWN);
		microSwitch.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
            	if(event.getState() == PinState.LOW){
            		if (System.currentTimeMillis() > debounce) {
            		    System.out.println("RFID triggered at segment "+config.segment());
            			debounce = System.currentTimeMillis() + 1000L;
            		    trigger("rfid1111");
            		}
            		else {
            		    System.out.println("ignored RFID triggered at segment "+config.segment());
            		}
            	}
            }
            
        });
	}

	@Deactivate
	void deactivate() {
	}

	@Override
	public String lastRFID() {
		return lastRFID;
	}

	@Override
	public synchronized Promise<String> nextRFID() {
		return nextRFID.getPromise();
	}

	// This method is called when an RFID tag detected
	private synchronized void trigger(String rfid) {
		// bypass TrackController and immediately call locatedAt to avoid promise timeouts
		trackManager.locatedTrainAt(rfid, config.segment());
		
		Deferred<String> toResolve = nextRFID;
		nextRFID = new Deferred<String>();
		toResolve.resolve(rfid);
		this.lastRFID = rfid;
	}

	public String lastrfid() {
		return lastRFID;
	}
}
