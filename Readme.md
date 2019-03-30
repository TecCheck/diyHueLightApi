## diyHueApi

A simple library to program lights for the diyHue platform in Java

This Library requires
* nanohttp 2.3
* gson
* huesdk (download: https://github.com/PhilipsHue/PhilipsHueSDK-Java-MultiPlatform-Android/tree/master/JavaDesktopApp/libs) (you need the sdkresources.jar)

To make a light add a new Class
```java
public class YourLight extends DiyHueLight {

	public YourLight(String name, int lightID, boolean doDebug) {
		//name, lightId, doDebug, port
		super(name, lightID, doDebug, 25566);
	}

	@Override
	public void onUpdate(int lightID, int r, int g, int b, boolean lightState) {
		//lightState shows if the light ison or off
		System.out.println("Light " + lightID + ": R:" + r + ", G:" + g + ", B:" + b);
	}

}
```
Then create an instance of it and add that to the HttpServer
```java
YourLight light = new YourLight();
HttpServer.add(light);
```

If the diyHue bridge can't find your lights you can add them manually
The two important things to change are the ip adress and the light number (it shold match the light id)
```json
"lights": {
        "1": {
            "config": {
                "archetype": "sultanbulb",
                "direction": "omnidirectional",
                "function": "mixed",
                "startup": {
                    "configured": false,
                    "mode": "safety"
                }
            },
            "manufacturername": "Philips",
            "modelid": "LCT015",
            "name": "LIGHT NAME HERE",
            "state": {
                "alert": "select",
                "bri": 254,
                "colormode": "hs",
                "ct": 0,
                "effect": "none",
                "hue": 0,
                "on": false,
                "reachable": false,
                "sat": 0,
                "transitiontime": 9,
                "xy": [
                    0,
                    0
                ]
            },
            "swversion": "1.46.13_r26312",
            "type": "Extended color light",
            "uniqueid": "00:17:88:01:00:7d:78:f4-0b"
        }
    },
    "lights_address": {
        "1": {
            "ip": "IP OF THE LIGHT HERE",
            "light_nr": 1,
            "mac": "74:D4:35:2F:88:9A",
            "protocol": "native"
        } 
    },
```
