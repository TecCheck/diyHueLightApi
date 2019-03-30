package de.teccheck.diyhuelightapi;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.Gson;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public abstract class DiyHueLight {

	public static boolean init = false;

	public String lightName = "";
	public int colorCount = 3;
	public boolean doDebug = false;

	public boolean lightState = true;
	public boolean inTransition = false;

	public float transitiontime = 4.0f;

	public int colorMode = 3;
	public int bri = 255;
	public int sat = 0;
	public int hue = 0;
	public int ct = 0;
	public int x = 0;
	public int y = 0;

	public int[] colors = new int[colorCount];
	public float[] currentColors = new float[colorCount];
	public float[] stepLevel = new float[colorCount];

	public int lightID = 0;

	public DiyHueLight(String name, int lightID, boolean doDebug) {
		this(name, lightID, doDebug, 80);
	}

	public DiyHueLight(String name, int lightID, boolean doDebug, int port) {
		lightName = name;
		this.lightID = lightID;
		this.doDebug = doDebug;
		onUpdate(0, colors[0], colors[1], colors[2], lightState);
		if (!HttpServer.init) {
			new HttpServer(port);
		}
		if (!init) {
			init = true;
			startExitListener();
		}
	}

	public void loadConfig() {
		loadFromConfig("config_" + lightID + ".json");
	}

	public void saveConfig() {
		saveToConfig("config_" + lightID + ".json");
	}

	public abstract void onUpdate(int lightId, int r, int g, int b, boolean lightState);

	public String serve(IHTTPSession session) {

		long time = System.currentTimeMillis();

		String response = "empty";

		if (session.getUri().equals("/set") || session.getUri().equals("/set/")) {
			response = onSet(session.getParms());
		} else if (session.getUri().equals("/get") || session.getUri().equals("/get/")) {
			response = onGet(session.getParms());
		} else if (session.getUri().equals("/detect") || session.getUri().equals("/detect/")) {
			response = onDetect(session.getParms());
		} else if (session.getUri().equals("/")) {
			response = onRoot(session.getParms());
		} else if (session.getUri().equals("/debug") || session.getUri().equals("/debug/")) {
			response = onDebug(session.getParms());
		} else if ((session.getUri().equals("/discover") || session.getUri().equals("/discover/")) && doDebug) {
			response = onDetect(session.getParms());
		}

		convertColors(colorMode);

		onUpdate(lightID, colors[0], colors[1], colors[2], lightState);

		System.out.println("time: " + (System.currentTimeMillis() - time) + " millis!");

		return response;
	}

	void lightEngine() {
		for (int color = 0; color < colorCount; color++) {
			if (lightState) {
				if (colors[color] != currentColors[color]) {
					inTransition = true;
					currentColors[color] += stepLevel[color];
					if ((stepLevel[color] > 0.0f && currentColors[color] > colors[color])
							|| (stepLevel[color] < 0.0f && currentColors[color] < colors[color]))
						currentColors[color] = colors[color];
				}
			} else {
				if (currentColors[color] != 0) {
					inTransition = true;
					currentColors[color] -= stepLevel[color];
					if (currentColors[color] < 0.0f)
						currentColors[color] = 0;
				}
			}
		}
		if (inTransition) {
			sleep(6);
			inTransition = false;
		}
	}

	void convertColors(int mode) {
		// modes: 0 from RGB, 3 from HSV, 1 from XY
		if (mode == 0) {
			Color rgb = new Color(colors[0], colors[1], colors[2]);

			float[] xy = PHUtilities.calculateXY(rgb.getRGB(), "LCT015");
			x = (int) xy[0];
			y = (int) xy[1];

			float[] hsv = Color.RGBtoHSB(colors[0], colors[1], colors[2], null);
			hue = (int) hsv[0] * 65535;
			sat = (int) hsv[1];
			bri = (int) hsv[2];
		} else if (mode == 3) {
			float hueF = (float) hue / (float) 65535;
			float satF = (float) sat / (float) 255;
			float briF = (float) bri / (float) 255;
			Color rgb = new Color(Color.HSBtoRGB(hueF, satF, briF));
			colors[0] = rgb.getRed();
			colors[1] = rgb.getGreen();
			colors[2] = rgb.getBlue();

			float[] xy = PHUtilities.calculateXY(rgb.getRGB(), "LCT015");
			x = (int) xy[0];
			y = (int) xy[1];

		} else if (mode == 1) {
			float[] xy = { x, y };
			Color rgb = new Color(PHUtilities.colorFromXY(xy, "LCT015"));
			colors[0] = rgb.getRed();
			colors[1] = rgb.getGreen();
			colors[2] = rgb.getBlue();

			float[] hsv = Color.RGBtoHSB(colors[0], colors[1], colors[2], null);
			hue = (int) hsv[0] * 65535;
			sat = (int) hsv[1];
			bri = (int) hsv[2];

		}
	}

	void set(Map<String, String> args) {

		lightState = true;
		if (args.get("on") != null) {
			lightState = Boolean.parseBoolean(args.get("on").toLowerCase());
		}
		if (args.get("r") != null) {
			colors[0] = Integer.parseInt(args.get("r"));
			colorMode = 0;
		}
		if (args.get("g") != null) {
			colors[1] = Integer.parseInt(args.get("g"));
			colorMode = 0;
		}
		if (args.get("b") != null) {
			colors[2] = Integer.parseInt(args.get("b"));
			colorMode = 0;
		}
		if (args.get("x") != null) {
			x = Integer.parseInt(args.get("x"));
			colorMode = 1;
		}
		if (args.get("y") != null) {
			y = Integer.parseInt(args.get("y"));
			colorMode = 1;
		}
		if (args.get("bri") != null) {
			if (Integer.parseInt(args.get("bri")) != 0)
				bri = Integer.parseInt(args.get("bri"));
			colorMode = 3;
		}
		if (args.get("bri_inc") != null) {
			bri += Integer.parseInt(args.get("bri_inc"));
			if (bri > 255)
				bri = 255;
			else if (bri < 0)
				bri = 0;
		}
		if (args.get("ct") != null) {
			ct = Integer.parseInt(args.get("ct"));
			colorMode = 2;
		}
		if (args.get("sat") != null) {
			sat = Integer.parseInt(args.get("sat"));
			colorMode = 3;
		}
		if (args.get("hue") != null) {
			hue = Integer.parseInt(args.get("hue"));
			colorMode = 3;
		}
		if (args.get("alert") != null && args.get("select") != null) {
			if (lightState) {
				currentColors[0] = 0;
				currentColors[1] = 0;
				currentColors[2] = 0;
			} else {
				currentColors[0] = 255;
				currentColors[1] = 255;
				currentColors[2] = 255;
			}

		}
		if (args.get("transitiontime") != null) {
			transitiontime = Integer.parseInt(args.get("transitiontime"));
		}
		if (args.get("light") != null) {
			lightID = Integer.parseInt(args.get("light"));
		}

		convertColors(colorMode);

	}

	String onDebug(Map<String, String> args) {
		String s = "OK";
		s += ", x:" + x;
		s += ", y:" + y;
		s += ", hue: " + hue;
		s += ", sat: " + sat;
		s += ", bri:" + bri;
		s += ", ct:" + ct;
		s += ", r: " + colors[0];
		s += ", g:" + colors[1];
		s += ", b:" + colors[2];
		s += ", colormode:" + colorMode;
		s += ", state:" + lightState;
		return s;
	}

	String onDetect(Map<String, String> args) {
		return ("{\"hue\": \"bulb\",\"lights\": 1,\"modelid\": \"LCT015\",\"name\": \"" + lightName + "\",\"mac\": \""
				+ "74:D4:35:2F:88:9A" + "\"}");

	}

	String onGet(Map<String, String> args) {

		String colormode = "";
		String power_status;
		power_status = lightState ? "true" : "false";
		if (colorMode == 1)
			colormode = "xy";
		else if (colorMode == 2)
			colormode = "ct";
		else if (colorMode == 3)
			colormode = "hs";
		return "{\"on\": " + power_status + ", \"bri\": " + bri + ", \"xy\": [" + x + ", " + y + "], \"ct\":" + ct
				+ ", \"sat\": " + sat + ", \"hue\": " + hue + ", \"colormode\": \"" + colormode + "\"}";

	}

	String onSet(Map<String, String> args) {
		set(args);

		return "OK, x: " + x + ", y:" + y + ", bri:" + bri + ", ct:" + ct + ", colormode:" + colorMode + ", state:"
				+ lightState;

	}

	String onRoot(Map<String, String> args) {
		set(args);

		String http_content = "<!doctype html>";
		http_content += "<html>";
		http_content += "<head>";
		http_content += "<meta charset=\"utf-8\">";
		http_content += "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">";
		http_content += "<title>";
		http_content += lightName;
		http_content += " - Light Setup</title>";
		http_content += "<link rel=\"stylesheet\" href=\"https://unpkg.com/purecss@0.6.2/build/pure-min.css\">";
		http_content += "</head>";
		http_content += "<body>";
		http_content += "<fieldset>";
		http_content += "<h3>";
		http_content += lightName;
		http_content += " - Light Setup</h3>";
		http_content += "<form class=\"pure-form pure-form-aligned\" action=\"/\" method=\"post\">";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"power\"><strong>Power</strong></label>";
		http_content += "<a class=\"pure-button";
		if (lightState)
			http_content += "  pure-button-primary";
		http_content += "\" href=\"/?on=true\">ON</a>";
		http_content += "<a class=\"pure-button";
		if (!lightState)
			http_content += "  pure-button-primary";
		http_content += "\" href=\"/?on=false\">OFF</a>";
		http_content += "</div>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"startup\">Startup</label>";
		http_content += "<select onchange=\"this.form.submit()\" id=\"startup\" name=\"startup\">";
		http_content += "<option ";
		if (read(1) == 0)
			http_content += "selected=\"selected\"";
		http_content += " value=\"0\">Last state</option>";
		http_content += "<option ";
		if (read(1) == 1)
			http_content += "selected=\"selected\"";
		http_content += " value=\"1\">On</option>";
		http_content += "<option ";
		if (read(1) == 2)
			http_content += "selected=\"selected\"";
		http_content += " value=\"2\">Off</option>";
		http_content += "</select>";
		http_content += "</div>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"scene\">Default Scene</label>";
		http_content += "<select onchange = \"this.form.submit()\" id=\"scene\" name=\"scene\">";
		http_content += "<option ";
		if (read(2) == 0)
			http_content += "selected=\"selected\"";
		http_content += " value=\"0\">Relax</option>";
		http_content += "<option ";
		if (read(2) == 1)
			http_content += "selected=\"selected\"";
		http_content += " value=\"1\">Read</option>";
		http_content += "<option ";
		if (read(2) == 2)
			http_content += "selected=\"selected\"";
		http_content += " value=\"2\">Concentrate</option>";
		http_content += "<option ";
		if (read(2) == 3)
			http_content += "selected=\"selected\"";
		http_content += " value=\"3\">Energize</option>";
		http_content += "<option ";
		if (read(2) == 4)
			http_content += "selected=\"selected\"";
		http_content += " value=\"4\">Bright</option>";
		http_content += "<option ";
		if (read(2) == 5)
			http_content += "selected=\"selected\"";
		http_content += " value=\"5\">Dimmed</option>";
		http_content += "<option ";
		if (read(2) == 6)
			http_content += "selected=\"selected\"";
		http_content += " value=\"6\">Nightlight</option>";
		http_content += "<option ";
		if (read(2) == 7)
			http_content += "selected=\"selected\"";
		http_content += " value=\"7\">Savanna sunset</option>";
		http_content += "<option ";
		if (read(2) == 8)
			http_content += "selected=\"selected\"";
		http_content += " value=\"8\">Tropical twilight</option>";
		http_content += "<option ";
		if (read(2) == 9)
			http_content += "selected=\"selected\"";
		http_content += " value=\"9\">Arctic aurora</option>";
		http_content += "<option ";
		if (read(2) == 10)
			http_content += "selected=\"selected\"";
		http_content += " value=\"10\">Spring blossom</option>";
		http_content += "</select>";
		http_content += "</div>";
		http_content += "<br>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"state\"><strong>State</strong></label>";
		http_content += "</div>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"bri\">Bri</label>";
		http_content += "<input id=\"bri\" name=\"bri\" type=\"text\" placeholder=\"" + String.valueOf(bri) + "\">";
		http_content += "</div>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"hue\">Hue</label>";
		http_content += "<input id=\"hue\" name=\"hue\" type=\"text\" placeholder=\"" + String.valueOf(hue) + "\">";
		http_content += "</div>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"sat\">Sat</label>";
		http_content += "<input id=\"sat\" name=\"sat\" type=\"text\" placeholder=\"" + String.valueOf(sat) + "\">";
		http_content += "</div>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"ct\">CT</label>";
		http_content += "<input id=\"ct\" name=\"ct\" type=\"text\" placeholder=\"" + String.valueOf(ct) + "\">";
		http_content += "</div>";
		http_content += "<div class=\"pure-control-group\">";
		http_content += "<label for=\"colormode\">Color</label>";
		http_content += "<select id=\"colormode\" name=\"colormode\">";
		http_content += "<option ";
		if (colorMode == 1)
			http_content += "selected=\"selected\"";
		http_content += " value=\"1\">xy</option>";
		http_content += "<option ";
		if (colorMode == 2)
			http_content += "selected=\"selected\"";
		http_content += " value=\"2\">ct</option>";
		http_content += "<option ";
		if (colorMode == 3)
			http_content += "selected=\"selected\"";
		http_content += " value=\"3\">hue</option>";
		http_content += "</select>";
		http_content += "</div>";
		http_content += "<div class=\"pure-controls\">";
		http_content += "<span class=\"pure-form-message\"><a href=\"/?alert=1\">alert</a> or <a href=\"/?reset=1\">reset</a></span>";
		http_content += "<label for=\"cb\" class=\"pure-checkbox\">";
		http_content += "</label>";
		http_content += "<button type=\"submit\" class=\"pure-button pure-button-primary\">Save</button>";
		http_content += "</div>";
		http_content += "</fieldset>";
		http_content += "</form>";
		http_content += "</body>";
		http_content += "</html>";

		return http_content;
	}

	public void loadFromConfig(String fileName) {
		Scanner scan = null;
		try {
			scan = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (scan != null) {
			String json = scan.nextLine();
			scan.close();

			Gson gson = new Gson();
			Config config = gson.fromJson(json, Config.class);
			hue = config.hue;
			sat = config.sat;
			bri = config.bri;
			x = config.x;
			y = config.y;
			ct = config.ct;
			colors = config.colors;
			lightState = config.lightState;
		}

		onUpdate(lightID, colors[0], colors[1], colors[2], lightState);

	}

	public void saveToConfig(String fileName) {
		try {
			Gson gson = new Gson();
			Config config = new Config();
			config.hue = hue;
			config.bri = bri;
			config.sat = sat;
			config.x = x;
			config.y = y;
			config.ct = ct;
			config.colors = colors;
			config.lightState = lightState;

			String json = gson.toJson(config);
			try (PrintWriter writer = new PrintWriter(fileName)) {
				writer.println(json);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void startExitListener() {
		Thread exitListener = new Thread(new Runnable() {
			@Override
			public void run() {

				boolean run = true;
				Scanner scan = new Scanner(System.in);
				while (run) {
					if (scan.nextLine().equals("exit")) {
						for (DiyHueLight light : HttpServer.lights) {
							light.saveToConfig("config_" + light.lightID + ".json");
						}
						run = false;
						scan.close();
					}
				}
				System.out.println("exit");

				System.exit(0);
			}
		});
		exitListener.start();
	}

	int read(int i) {
		switch (i) {
		case 0:
			return 0;
		case 1:
			return 0;
		case 2:
			return 1;
		}

		return 1;
	}

	static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}