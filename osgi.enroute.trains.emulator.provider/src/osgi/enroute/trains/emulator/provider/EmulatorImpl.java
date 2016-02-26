package osgi.enroute.trains.emulator.provider;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.TrackForSegment;
import osgi.enroute.trains.cloud.api.TrackForTrain;
import osgi.enroute.trains.track.util.Tracks;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;

/**
 * 
 */
@Designate(ocd = EmulatorImpl.Config.class)
@Component(name = "osgi.enroute.trains.emulator", immediate = true, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class EmulatorImpl {
	static Logger logger = LoggerFactory.getLogger(EmulatorImpl.class);

	@Reference
	private TrackForSegment trackForSegment;
	@Reference
	private TrackForTrain trackForTrain;
	@Reference
	private Scheduler scheduler;
	@Reference
	private DTOs dtos;
	@Reference
	private EventAdmin eventAdmin;

	private List<TrainControllerImpl> trainControllers = new ArrayList<>();
	private Tracks<Traverse> track;
	private Closeable trainTick;

	@ObjectClassDefinition
	@interface Config {
		String[]name_rfids() default {};

		@AttributeDefinition(description="Increasing this value will decrease the probability of the RFID check failing. The 90 "
				+ "value has an 80% change of success at speed 1, 70% at speed 2, and 60% at speed 3")
		double rfid_probability() default 90;
		
		double play_speed() default 0.5;

		String channel() default "CH1";
	}

	@Activate	
	void activate(Config config, BundleContext context) throws Exception {
		String name_rfids[] = config.name_rfids();
		track = new Tracks<Traverse>(trackForTrain.getSegments().values(), new EmulatorFactory(trackForSegment));

		track.getHandlers().forEach(sh -> sh.get().register(context));

		for (String name_rfid : name_rfids) {
			String[] parts = name_rfid.split("\\s*:\\s*");
			if (parts.length == 2) {
				TrainControllerImpl trainControllerImpl = new TrainControllerImpl(parts[0], parts[1],
						config.rfid_probability(), config.play_speed(), track.getRoot(), this);
				trainControllers.add(trainControllerImpl);
				trainControllerImpl.register(context, config.channel());
			} else
				logger.error("Invalid emulator train def" + name_rfid);
		}

		trainTick = scheduler.schedule(this::tick, 100, 100);
	}

	@Deactivate
	void deactivate() throws IOException {
		this.trainTick.close();
		for (TrainControllerImpl tci : trainControllers) {
			tci.close();
		}
		for (SegmentHandler<Traverse> sh : track.getHandlers())
			sh.get().close();
	}

	void tick() throws Exception {
		for (TrainControllerImpl tc : trainControllers)
			tc.tick();
	}

	void observation(Observation o) {
		try {
			o.time = System.currentTimeMillis();
			Event event = new Event(Observation.TOPIC, dtos.asMap(o));
			eventAdmin.postEvent(event);
		} catch (Exception e) {
			logger.error("Error posting observation " + o, e);
		}
	}

	void observation(Observation.Type type, String train, String segment, double speed) {
		Observation o = new Observation();
		o.type = type;
		o.train = train;
		o.segment = segment;
		o.speed = speed;
		observation(o);
	}

}
