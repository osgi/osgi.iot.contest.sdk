package osgi.enroute.trains.event.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.osgi.service.event.Event;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.sensor.api.SensorEvent;

public class EventConvertor {

		public static Observation eventToObservation(Event event, DTOs dtos) throws Exception {
			Observation o = new Observation();
			for ( Field f : o.getClass().getFields() ) {
				if ( Modifier.isStatic(f.getModifiers()))
					continue;
				
				Object rawValue = event.getProperty(f.getName());
				Object value = dtos.convert(rawValue).to(f.getGenericType());
				f.set(o, value);
			}
			return o;
		}
		
		
		public static SensorEvent eventToSensorEvent(Event event, DTOs dtos) throws Exception {
			SensorEvent o = new SensorEvent();
			for ( Field f : o.getClass().getFields() ) {
				if ( Modifier.isStatic(f.getModifiers()))
					continue;
				
				Object rawValue = event.getProperty(f.getName());
				Object value = dtos.convert(rawValue).to(f.getGenericType());
				f.set(o, value);
			}
			return o;
		}
}
