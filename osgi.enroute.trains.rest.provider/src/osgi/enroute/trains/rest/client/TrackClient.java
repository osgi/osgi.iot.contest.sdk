package osgi.enroute.trains.rest.client;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.osgi.service.component.annotations.Reference;

import aQute.lib.converter.TypeReference;
import aQute.lib.json.Decoder;
import aQute.lib.json.JSONCodec;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.trains.cloud.api.Observation;

public class TrackClient {
	static JSONCodec codec = new JSONCodec();
	static TypeReference<List<Observation>> LISTOBSERVATIONS = new TypeReference<List<Observation>>() {};
	final URI base;

	@Reference
	DTOs dtos;
	
	public TrackClient(URI base) {
		this.base = base;
	}

	public boolean blocked(String segment, String reason, boolean blocked) throws Exception {
		return (Boolean) send( "blocked", segment, reason, blocked).get();
	}
	
	Decoder send( Object ... params ) throws Exception {
		StringBuilder sb = new StringBuilder();
		String del = base.toString();
		if ( !del.endsWith("/"))
			sb.append("/");
		
		for ( Object p : params ) {
			sb.append(del).append( encode(p));
			del = "/";
		}
		InputStream inputStream = new URL(sb.toString()).openConnection().getInputStream();
		return codec.dec().from(inputStream);
	}


	private String encode(Object blocked) throws UnsupportedEncodingException {
		return URLEncoder.encode( ""+blocked, "UTF-8");
	}

	public List<Observation> getRecentObservations(long time) throws Exception {
		return send("observations", time).get(LISTOBSERVATIONS);
	}
}
