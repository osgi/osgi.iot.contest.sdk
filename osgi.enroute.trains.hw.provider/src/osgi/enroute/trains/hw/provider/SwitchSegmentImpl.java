package osgi.enroute.trains.hw.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

import osgi.enroute.trains.controller.api.SwitchSegmentController;

@Designate(ocd=SwitchSegmentImpl.Config.class, factory=true)
@Component(name = "osgi.enroute.trains.hw.switch", property="service.exported.interfaces=*", configurationPolicy=ConfigurationPolicy.REQUIRE)
public class SwitchSegmentImpl implements SwitchSegmentController {
	private GpioPinDigitalOutput alt;

	@Reference
	private GpioController gpio;

	private Config config;

	@ObjectClassDefinition
	@interface Config {
		int controller();

		String segment();
		
		String swtch();
	}

	@Activate
	void activate(Config config) {
		this.config = config;
		alt = setup(config.swtch());
		swtch(false);;
	}


	private GpioPinDigitalOutput setup(String name) {
		Pin pin = RaspiPin.getPinByName(name);
		if ( pin == null) {
			System.out.println("Pin is " + name + " is null");
			return null;
		}
		for (GpioPin e : gpio.getProvisionedPins()) {
			if (e.getPin().equals(pin)) {
				gpio.unprovisionPin(e);
				break;
			}
		}
		return this.gpio.provisionDigitalOutputPin(pin);
	}

	@Override
	public String toString() {
		return "Switch[alt="+getSwitch() + ", pin=" + alt.toString() + ", cntl="+config.controller() + ", seg="+config.segment() + "]";
	}


	@Override
	public void swtch(boolean alternative) {
		alt.setState(alternative);
	}


	@Override
	public boolean getSwitch() {
		return alt.getState().isHigh();
	}

}
