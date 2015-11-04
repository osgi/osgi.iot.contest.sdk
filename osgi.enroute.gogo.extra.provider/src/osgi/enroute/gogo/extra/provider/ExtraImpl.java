package osgi.enroute.gogo.extra.provider;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.libg.glob.Glob;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.dto.api.DTOs;

/**
 * 
 */
@Component(name = "osgi.enroute.gogo.extra", property = { Debug.COMMAND_SCOPE + "=extra",
		Debug.COMMAND_FUNCTION + "=extra", Debug.COMMAND_FUNCTION + "=srv",
		Debug.COMMAND_FUNCTION + "=log",
		Debug.COMMAND_FUNCTION + "=logt" }, service = ExtraImpl.class)
public class ExtraImpl {
	BundleContext context = FrameworkUtil.getBundle(ExtraImpl.class).getBundleContext();
	private LogTracker logTracker;

	@Reference
	DTOs dtos;

	@Activate
	void act(BundleContext context) {
		logTracker = new LogTracker(context);
		logTracker.open();
	}

	@Deactivate
	void deact(BundleContext context) {
		logTracker.close();
	}

	public String extra() {
		return "Extra commands\n" //
				+ "srv [-v|--not] [ glob [columns...]] \n" + "log [ glob ]\n";
	}

	public List<String> srv(
			@Parameter(names = { "-v", "--not" }, presentValue = "true", absentValue = "false") boolean v, Glob g,
			String... keys) throws Exception

	{
		ArrayList<String> sb = new ArrayList<>();
		ArrayList<Glob> columns = new ArrayList<>();
		for (String key : keys) {
			columns.add(new Glob(key));
		}

		ServiceReference<?>[] refs = context.getAllServiceReferences(null, null);
		if (refs != null) {
			for (ServiceReference<?> ref : refs) {
				Formatter sub = new Formatter();
				String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);
				String del = ", ";
				for (String oc : objectClass) {
					sub.format("%-24s", toShortName(oc));
				}
				del = " : ";
				for (String key : ref.getPropertyKeys()) {

					if (columns.isEmpty() || has(columns, key)) {
						sub.format("%s%s=%s", del, key, toString(ref.getProperty(key)));
						del = ", ";
					}
				}
				String s = sub.toString();
				if (g.matcher(s).find() == !v)
					sb.add(s);
			}
		}
		return sb;
	}

	private Object toShortName(String oc) {
		int n = oc.lastIndexOf('.');
		return oc.substring(n + 1);
	}

	private boolean has(ArrayList<Glob> columns, String key) {
		for (Glob g : columns) {
			if (g.matcher(key).matches())
				return true;
		}
		return false;
	}

	private String toString(Object o) throws Exception {
		if (o == null)
			return "null";

		return dtos.convert(o).to(String.class).toString();
	}

	public List<String> srv() throws Exception {
		return srv(false, new Glob("*"));
	}

	@Descriptor("Show the contents of the log")
	public List<String> log(
			//
			@Descriptor("Reverse the printout order to oldest is last") @Parameter(names = { "-r",
					"--reverse" }, absentValue = "true", presentValue = "false") boolean reverse, //
			@Descriptor("Skip the first entries") @Parameter(names = { "-s", "--skip" }, absentValue = "0") int skip, //
			@Descriptor("Maximum number of entries to print") @Parameter(names = { "-m",
					"--max" }, absentValue = "100") int maxEntries, //
			@Descriptor("Minimum level (error,warning,info,debug). Default is warning.") @Parameter(names = { "-l",
					"--level" }, absentValue = "warning") String level, //
			@Descriptor("Print style (classic,abbr)") @Parameter(names = { "-y",
					"--style" }, absentValue = "classic") String style, //
			@Descriptor("Do not print exceptions.") @Parameter(names = { "-n",
					"--noexceptions" }, absentValue = "false", presentValue = "true") boolean noExceptions //
	) {
		return logTracker.log(maxEntries, skip, LogTracker.Level.valueOf(level), reverse, noExceptions,
				LogTracker.Style.valueOf(style));
	}

	@Descriptor("display all matching log entries")
	public List<String> log(@Descriptor("minimum log level [ debug | info | warn | error ]") String logLevel) {

		LogTracker.Level l = LogTracker.Level.valueOf(logLevel);
		return logTracker.log(Integer.MAX_VALUE, 0, l, true, false, LogTracker.Style.classic);
	}

	@Descriptor("display some matching log entries")
	public List<String> log(@Descriptor("maximum number of entries") int maxEntries,
			@Descriptor("minimum log level [ debug | info | warn | error ]") String logLevel) {
		LogTracker.Level l = LogTracker.Level.valueOf(logLevel);
		return logTracker.log(maxEntries, 0, l, false, false, LogTracker.Style.classic);
	}

	@Descriptor("Trace the log")
	public void logt(CommandSession session) throws IOException {
		PrintStream console = session.getConsole();
		logTracker.addConsole(console);
		try {
			session.getKeyboard().read();
		} finally {
			logTracker.removeConsole(console);
		}
	}
}
