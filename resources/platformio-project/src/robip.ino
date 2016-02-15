#include "robip.h"
#include "settings.h"

#include <Arduino.h>

#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>

#include <ESP8266HTTPClient.h>
#include <ESP8266httpUpdate.h>

ESP8266WiFiMulti robip_wifi;

void robip_update() {
  String urlStr = "http://robip.halake.com/api/";
  urlStr.concat(ROBIP_ID);
  urlStr.concat("/latest?since=");
  urlStr.concat(ROBIP_BUILD);

  char url[50];
  urlStr.toCharArray(url, 50);

  Serial.printf("[Robip: Update] %s\n", url);
  
  for (uint8_t t = 4; t > 0; t--) {
	Serial.printf("[Robip: Update: %d] %d...\n", t);
	if (robip_wifi.run() != WL_CONNECTED) {
	  delay(500);
	}
	t_httpUpdate_return ret = ESPhttpUpdate.update(url);
	switch(ret) {
	case HTTP_UPDATE_FAILED:
	  Serial.println("`HTTP_UPDATE_FAILD");
	  break;
	  
	case HTTP_UPDATE_NO_UPDATES:
	  Serial.println("HTTP_UPDATE_NO_UPDATES");
	  break;
	  
	case HTTP_UPDATE_OK:
	  Serial.println("HTTP_UPDATE_OK");
	  break;
	}
	return;
  }
}

void robip_setupWifi() {
  Serial.begin(115200);
  Serial.setDebugOutput(true);

  Serial.println();
  Serial.println();
  Serial.println();

  for(uint8_t t = 4; t > 0; t--) {
    Serial.printf("[Robip: Setup] %d...\n", t);
    Serial.flush();
    delay(1000);
  }

  robip_wifi.addAP(ROBIP_WIFI_SSID, ROBIP_WIFI_PASS);
  robip_wifi.addAP("robip", ROBIP_ID);

  robip_update();
}


void robip_currentMotion(RobipMotion *motion) {
  motion->yaw = 0;
  motion->pitch = 0;
  motion->roll = 0;
}

RobipMotion robip_getCurrentMotion() {
  RobipMotion motion;
  robip_currentMotion(&motion);

  return motion;
}

size_t robip_serialWrite(int n) {
  return Serial.print(n);
}

size_t robip_serialWrite(double n) {
  return Serial.print(n, 4);
}

size_t robip_serialWrite(char *s) {
  return Serial.print(s);
}
