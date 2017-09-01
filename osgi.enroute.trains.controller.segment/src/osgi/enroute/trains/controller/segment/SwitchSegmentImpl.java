package osgi.enroute.trains.controller.segment;

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

import osgi.enroute.trains.segment.api.SwitchSegmentController;

@Designate(ocd = SwitchSegmentImpl.Config.class, factory = true)
@Component(name = "osgi.enroute.trains.hw.switch", property = "service.exported.interfaces=*",
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SwitchSegmentImpl implements SwitchSegmentController {

    private GpioPinDigitalOutput fwd;
    private GpioPinDigitalOutput rev;
    private int duration = 3000;
    private boolean state;

    @Reference
    private GpioController gpio;

    private Config config;

    @ObjectClassDefinition
    @interface Config {
        int controller_id();

        String fwd();

        String rev();

        int duration() default 3000;
    }

    @Activate
    void activate(Config config) {
        this.config = config;
        fwd = setup(config.fwd());
        rev = setup(config.rev());
        duration = config.duration();

        info("activate: force to state<NORMAL>");
        state = true;
        swtch(!state);
    }

    private GpioPinDigitalOutput setup(String name) {
        Pin pin = RaspiPin.getPinByName(name);
        if (pin == null) {
            info("Pin<{}> is null", name);
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
        return "Switch[alt=" + getSwitch() + ", fwd=" + fwd.toString() + ", rev=" + rev.toString() + ", cntl="
                + config.controller_id() + "]";
    }

    @Override
    public void swtch(boolean alt) {
        if (state == alt) {
            info("already at state<{}>", alt ? "ALT" : "NORMAL");
            return;
        }
        if (alt) {
            rev.pulse(duration);
        } else {
            fwd.pulse(duration);
        }
        state = alt;
    }

    @Override
    public boolean getSwitch() {
        return state;
    }

    private void info(String fmt, Object... args) {
        String ident = String.format("Switch<%d>: ", config.controller_id());
        System.out.printf(ident + fmt.replaceAll("\\{}", "%s") + "\n", args);
    }

}
