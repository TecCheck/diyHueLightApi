package de.teccheck.diyhuelightapi;

public class Config {

	String lightName = "";
	int colorCount = 3;
	int port = 80;

	boolean lightState = true;
	boolean inTransition = false;

	float transitiontime = 4.0f;

	int colorMode = 0;
	int bri = 0;
	int sat = 0;
	int hue = 0;
	int ct = 0;
	int x = 0;
	int y = 0;

	int[] colors = new int[colorCount];
	int[] outputColors = new int[colorCount];
	float[] currentColors = new float[colorCount];
	float[] stepLevel = new float[colorCount];

}
