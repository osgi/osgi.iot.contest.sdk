# OSGI ENROUTE TRAINS HARDWARE PROVIDER

${Bundle-Description}

## Example

Obtain demo RSA licence from mailto:license@paremus.com

	Copy RSA licence to ~/etc/license.ini on remote (and local) systems
	Copy cnf, osgi.enroute.trains.hardware.provider and biz.aQute.bnd-3.1.0.jar to remote system.

Launch on remote system:

	java -jar biz.aQute.bnd-3.1.0.jar osgi.enroute.trains.hardware.provider/hardware-rsa.bndrun

Open webconsole and create Hardware Configuration:

	http://rasbperry-pi:8081/system/console/configMgr
	
Launch osgi.enroute.trains.application/trains-rsa.bndrun from Eclipse on local system

	Observe messages from ExampleHardwareDetector in Eclipse console
	


## Configuration
	Pid: osgi.enroute.trains.hardware.provider

	Field				Type			Description
	
	irLed				boolean			specifies whether IR LED is installed
	signals				String[]		names of segments containing these signals
	switches			String[]		names of segments containing these switches
	locators			String[]		names of segments containing these locators
		
	etc/osgi.enroute.trains.hardware.provider.config
		Hardware Configuration (created in webconsole, and persisted by ConfigPersister)

## References

