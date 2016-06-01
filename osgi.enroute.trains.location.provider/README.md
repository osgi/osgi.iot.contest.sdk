### CeBIT changes to IoT Trains Demo

The existing architecture is described at  [enroute.osgi.org/trains/200-architecture.html](http://enroute.osgi.org/trains/200-architecture.html)


#### Hardware changes

In November 2015, the original RFID readers did not work and we were forced to use micro-switches instead.

For CeBIT 2016 we used a different RFID reader, which we mounted on the train.
It uses bluetooth to transmit the RFID tag ids to a bluetooth reader (running on a Pi).

#### Software changes

We changed as little as possible and we didn't delete anything so, for example, the microswitch-based locator is still available.

All changes have been made to a new **cebit** branch of the osgi.iot.contest.sdk git repo.
**Going forward, we need to agree where future changes should be made.**


Walt wrote a new bluetooth reader to run on the Pi (using facilities provided in Kura).
This pairs with the bluetooth dongle on the train, receives and decodes each RFID tag id
and publishes it to MQTT.

At CeBIT, we used Eurotech's Everywhere Device Cloud as the MQTT server, although we had a fallback of using a local MQTT server.


We added a new TrainLocator interface:

    public interface TrainLocator {
	  /**
	   * get train location. Promise is resolved when train passes next location.
	   * @return colon separated string "trainId:segmentName"
	   */
	  Promise<String> nextLocation();
    }

and a new ``osgi.enroute.trains.location.provider`` project that implements this interface, by subscribing to the MQTT server to obtain the RFID tag ids.

We had intended to change the publication of RFID tags, so it used EventAdmin locally and only used MQTT for statistical purposes. However, the round-trip to MQTT for each location event didn't cause any problems at CeBIT, so we haven't done this.


#### Configuration

RFID tags have 10 hex-digit IDs, which we didn't want to have to transcribe to the track configuration.

The ``location.provider`` contains a configuration to map the 10-digit IDs into small integers matching a sticker on each tag:

        ".comment" : "map RFID tag UUIDs to simple codes on stickers attached to tags",
        "code2tag" : [
			"1:010E9CD250",
			"2:010E9EF905",
			"3:010E9CD8F6",
			"4:010E9EF8F9",

Straight tracks have an even tag id and curved tracks have an odd tag id, allowing the possibility of automatically discovering the track layout - although we haven't attempted this!


We added an extra 'tag' column to the track configuration, which was set to 0 if a segment did not contain an RFID tag.
We left the original LOCATOR segment markers, as these are shown in the UI as destinations, and we didn't want the UI to show a drop-down with every track segment as a destination (even although this is now possible).

``TrackForSegment.trainLocatedAt(rfid, segment)`` was originally only called from the old RFID mechanism; it is now also called from the new bluetooth/MQTT locator mechanism and the ``ExampleTrackManagerImpl`` has been updated to treat this as a locator event for LOCATOR segments or an observation otherwise.



        "service.pid":"osgi.enroute.trains.track.manager",
        "name": "main",
        "segments": [
            "# ID       : TYPE       : TAG    : CTRLR  : TO",
            "A00        : STRAIGHT   : 26      : -1     : A01",
            "A01        : CURVED     : 33      : -1     : A02",
            "A02        : CURVED     : 35      : -1     : A03",
            "A03        : CURVED     : 37      : -1     : A04",
            "A04        : CURVED     : 39      : -1     : A05",
            "A05        : STRAIGHT   : 28      : -1     : A06",
            "A06        : STRAIGHT   : 30      : -1     : A07",
            "A07        : STRAIGHT   : 32      : -1     : A08",
            "A08        : STRAIGHT   : 34      : -1     : A09",
            "A09        : STRAIGHT   : 36      : -1     : A99_R",
            "A99_R      : STRAIGHT   : 38      : -1     : A11",
            "A11        : STRAIGHT   : 40      : -1     : A12",
            "A12        : STRAIGHT   : 42      : -1     : A13",
            "A13        : STRAIGHT   : 44      : -1     : A14",
            "A14        : STRAIGHT   : 46      : -1     : A15",
            "A15        : STRAIGHT   : 48      : -1     : A16",
            "A16        : STRAIGHT   : 50      : -1     : A17",
            "A17        : CURVED     : 41      : -1     : A18",
            "A18        : CURVED     : 43      : -1     : A19",
            "A19        : CURVED     : 0      : -1     : A19_L",
            "A19_L      : LOCATOR    : 1      : 2      : A20",
            "A20        : CURVED     : 45      : -1     : A20_S",
            "A20_S      : SIGNAL     : 0      : 3      : A21",
            "A21        : STRAIGHT   : 52      : -1     : X01",

            "X01        : SWITCH     : 0      : 4      : B00,C00",

#### Deployment

Walt's bluetooth bundle was deployed to kura/ESF from Eurotech's Everywhere Device Cloud.

The rest of the demo was deployed using bnd-exported jars, as it requires OSGi R6.
The .bndrun files are all in the ``trains.application`` project.

``hardware-rsa?.bndrun`` are deployed to pi1, pi2, and pi3.
``trains-rsa.bndrun`` is deployed to the server.

The -rsa indicates use of the Paremus RSA stack, which requires a licence which Mike can provide you.

``debug.bndrun`` is configured to run the emulator and two trains.


#### Bugs

In 2015 we were only able to run one train, so we didn't discover some bugs in the track manager.
It was failing to release the track, which prevented a second train from running.

This was quickly resolved, although the track manager can still get confused if a train misses some RFID tags, or doesn't stop when expected. Due to the infra-red interference issue at CeBIT, the train quite often failed to receive a 'stop' command, which compounded this problem.

As we now receive location events for almost all segments, we modified the track manager to stop the train if it was discovered running off-route.

To overcome the infra-red interference problem we experienced at CeBIT, we allowed for multiple IR senders to be activated and issued each command to all available senders (in the hope that at least one would succeed).






