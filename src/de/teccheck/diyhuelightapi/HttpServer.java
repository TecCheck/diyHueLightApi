package de.teccheck.diyhuelightapi;

import java.io.IOException;
import java.util.ArrayList;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

	public static boolean init = false;
	public static double time = 0;

	static ArrayList<DiyHueLight> lights = new ArrayList<>();

	public HttpServer(int port) {
		super(port);

		try {
			start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		init = true;
	}

	@Override
	public Response serve(IHTTPSession session) {
		System.out.println("last request time: " + (System.currentTimeMillis() - time) + " millis");
		time = System.currentTimeMillis();
		String response = "httpServer";

		for (DiyHueLight light : lights) {
			if (String.valueOf(light.lightID).equals(session.getParms().get("light"))) {
				response = light.serve(session);
			}
		}

		return newFixedLengthResponse(response);
	}

	public static void add(DiyHueLight light) {
		lights.add(light);
	}

}
