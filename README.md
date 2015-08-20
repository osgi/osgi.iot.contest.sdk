# osgi.iot.contest.sdk
Building on the successful showcase from last year the [OSGi Alliance][osgi] will demonstrate how OSGi was made for IoT on the next [EclipseCon][eclipsecon] in Ludwigsburg. This repository will host the Software Development Kit (SDK) for the associated competition.

The OSGi advantage of being able to reliably share code between the cloud all the way down to small devices is particularly effective in the IoT world. This advantage will be demonstrated with a train track that runs multiple trains.   From the cloud an application manages the overall track and provides a user interface to set assignments and show the system’s state. It communicates with edge devices that control the LEGO trains, the switches, signals, RFID readers, and other elements. The cloud also manages all the software deployments in the system.

The demonstration uses OSGi all the way down to the lowest levels of sensors and actuators. It is underpinned by [OSGi enRoute][enoute], which provides an easy to use tool chain for OSGi that provides the best known practices with a true service oriented architecture.   The demonstration will act as a playground for conference delegates. They will be able to assign task to the trains and see how the trains navigate the tracks. This will be made even more interesting than a pre-fab demonstration because the playground will be associated with a competition. The OSGi Alliance will provide an SDK for the playground that includes a train emulator of the track and train. Contestants can provide bundles that control a train or the overall track. This will undoubtedly give some spectacles while demonstrating the effectiveness of OSGi in IoT. The winners of the competition will be selected and announced on the final day.


[osgi]: http://www.osgi.org
[enroute]: http://enroute.osgi.org
[eclipsecon]: https://www.eclipsecon.org/europe2015/
