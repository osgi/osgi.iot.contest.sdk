package osgi.enroute.trains.hw.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import osgi.enroute.trains.controller.api.RFIDSegmentController;

@Designate(ocd=RFIDSegmentImpl.Config.class, factory=true)
@Component(name = "osgi.enroute.trains.hw.rfid", property="service.exported.interfaces=*", configurationPolicy=ConfigurationPolicy.REQUIRE)
public class RFIDSegmentImpl implements RFIDSegmentController {

	private Config config;

	private String lastRFID = null;
	private Deferred<String> nextRFID = new Deferred<String>();
	
	
	@ObjectClassDefinition
	@interface Config {
		int controller();
		
		// TODO also specify which hardware pins/port RFID reader to read out in config
	}

	@Activate
	void activate(Config config) {
		this.config = config;
		
		// TODO initialize reading out the RFID reader - call trigger each time RFID is detected
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
	private synchronized void trigger(String rfid){
		Deferred<String> toResolve = nextRFID;
		nextRFID = new Deferred<String>();
		toResolve.resolve(rfid);
		this.lastRFID = rfid;
	}
}
