package osgi.enroute.trains.hw.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.trains.controller.api.RFIDSegmentController;

@Designate(ocd = RFIDSegmentImpl.Config.class, factory = true)
@Component(name = "osgi.enroute.trains.hw.rfid", immediate = true, property = { "service.exported.interfaces=*", //
		Debug.COMMAND_SCOPE + "=rfid", //
		Debug.COMMAND_FUNCTION + "=lastrfid" }, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RFIDSegmentImpl implements RFIDSegmentController, Runnable {

	private String command;
	private Process mfrc522;

	private String lastRFID = null;
	private Deferred<String> nextRFID = new Deferred<String>();

	@ObjectClassDefinition
	@interface Config {
		int controller();

		String segment();

		int channel();
	}

	@Activate
	void activate(Config config) {
		command = "mfrc522-read " + config.channel();
		new Thread(this).start();
	}

	@Deactivate
	void deactivate() {
		if (mfrc522 != null) {
			mfrc522.destroy();
			mfrc522 = null;
		}
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
		Deferred<String> toResolve = nextRFID;
		nextRFID = new Deferred<String>();
		toResolve.resolve(rfid);
		this.lastRFID = rfid;
	}

	public void run() {
		try {
			mfrc522 = Runtime.getRuntime().exec(command);
			System.out.println("Started RFID reader: " + command);
			InputStream in = mfrc522.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			// Card read UID: 180,60,30,87
			final String UID = "Card read UID: ";
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(UID)) {
					System.out.println(line);
					int uid = 0;
					for (String u : line.substring(UID.length()).split(",")) {
						uid = (uid << 8) | Integer.parseInt(u);
					}

					String tag = Integer.toHexString(uid);
					System.out.println(tag);
					trigger(tag);
				}
			}
		} catch (IOException e) {
			System.err.println("mfrc522read failed! " + e);
			throw new UncheckedIOException(e);
		}
		finally {
			System.out.println("RFID reader thread finished");
		}
	}

	public String lastrfid() {
		return lastRFID;
	}
}
