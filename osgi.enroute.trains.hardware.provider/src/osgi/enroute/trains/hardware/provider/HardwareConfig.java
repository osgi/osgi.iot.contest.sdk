package osgi.enroute.trains.hardware.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Defines which parts of the hardware are connected to which track segments.
 */
@ObjectClassDefinition(name = "Hardware Configuration",
        description = "Defines which parts of the hardware are connected to which track segments")
@interface HardwareConfig {
    String HARDWARE_CONFIGURATION_PID = "osgi.enroute.trains.hardware.provider";

    @AttributeDefinition(name = "IR LED", description = "Indicates whether the infra-red LED is installed")
    boolean irLed() default false;

    @AttributeDefinition(description = "Name of segment where each Lego signal tower is installed")
    String[]signals() default {"", ""};

    @AttributeDefinition(description = "Name of segment where each Lego track switch is installed")
    String[]switches() default {"", ""};

    @AttributeDefinition(description = "Name of segment where each RFID locator is installed")
    String[]locators() default {"", ""};

}