#include "robip.h"
#include "settings.h"

#include <Arduino.h>

#define DEBUG_ESP_WIFI
#define DEBUG_ESP_PORT

#include <ESP8266WiFiMulti.h>

#include <ESP8266HTTPClient.h>
#include <ESP8266httpUpdate.h>
#include <ESP8266mDNS.h>

#include <WiFiUdp.h>
#include <ArduinoOTA.h>

#define ROBIP_WIFI_AP_MODE_GPIN 0

#define ROBIP_IR_GPOUT 5

ESP8266WiFiMulti robip_wifi;
int robip_updateStatus = 0;
boolean robip_accesspoint_mode = false;

void robip_update() {
  if (robip_accesspoint_mode || robip_updateStatus >= 1) {
     ArduinoOTA.handle();

	 if (robip_accesspoint_mode) {
	   Serial.println("ap mode.\n");
	   Serial.flush();
	 }

     return;
  }

  if (robip_wifi.run() != WL_CONNECTED) {
	return;
  }

  robip_updateStatus = 1;

  String urlStr = "http://robip.halake.com/api/";
  urlStr.concat(ROBIP_ID);
  urlStr.concat("/latest?since=");
  urlStr.concat(ROBIP_BUILD);

  char url[128];
  urlStr.toCharArray(url, 128);

  Serial.printf("[Robip: Update] %s\n", url);
  
  Serial.printf("[Robip: Update: %d] ...\n", robip_updateStatus);

  t_httpUpdate_return ret = ESPhttpUpdate.update(url);
  switch(ret) {
  case HTTP_UPDATE_FAILED:
	Serial.printf("[Robip: Update: %d] update failed\n", robip_updateStatus);
	break;
	
  case HTTP_UPDATE_NO_UPDATES:
	Serial.printf("[Robip: Update: %d] no updates\n", robip_updateStatus);
	break;
	
  case HTTP_UPDATE_OK:
	Serial.printf("[Robip: Update: %d] update ok\n", robip_updateStatus);
	break;
  }
}

void robip_setupWifi() {
  Serial.begin(115200);

  Serial.println();
  Serial.println();
  Serial.println();

  pinMode(ROBIP_WIFI_AP_MODE_GPIN, INPUT); // check wifi access point mode button
  int apModeGPINOnCount = 0, prevGPINStatus = 1;

  for(int t = 3; t > 0; t--) {
    Serial.printf("[Robip: Booting] %d...\n", t);
    Serial.flush();

    for (int in_t = 0; in_t < 500; in_t++) {
	  int status = digitalRead(ROBIP_WIFI_AP_MODE_GPIN);
	  if (status != prevGPINStatus) {
		if (status == 0)
		  apModeGPINOnCount++;
		prevGPINStatus = status;
	  }
	  delay(2);
    }
  }

  robip_accesspoint_mode = (apModeGPINOnCount >= 3);

  char ssid[20];
  (String("robip-") + String(ROBIP_ID).substring(0, 5)).toCharArray(ssid, 20);

  if (robip_accesspoint_mode) {
	WiFi.softAP(ssid, ROBIP_ID);
	
  } else {
	for (int i = 0; i < ROBIP_WIFI_COUNT; i++) {
	  robip_wifi.addAP(ROBIP_WIFI_SSID[i], ROBIP_WIFI_PASS[i]);
	}
  }

  ArduinoOTA.setHostname(ssid);

  ArduinoOTA.onStart([]() {
    Serial.println("Start");
  });
  ArduinoOTA.onEnd([]() {
    Serial.println("End");
  });
  ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
    Serial.printf("Progress: %u%%\n", (progress / (total / 100)));
  });
  ArduinoOTA.onError([](ota_error_t error) {
    Serial.printf("Error[%u]: ", error);
    if (error == OTA_AUTH_ERROR) Serial.println("Auth Failed");
    else if (error == OTA_BEGIN_ERROR) Serial.println("Begin Failed");
    else if (error == OTA_CONNECT_ERROR) Serial.println("Connect Failed");
    else if (error == OTA_RECEIVE_ERROR) Serial.println("Receive Failed");
    else if (error == OTA_END_ERROR) Serial.println("End Failed");
  });
  ArduinoOTA.begin();
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

void robip_sendIR(unsigned int data[]) {
  pinMode(ROBIP_IR_GPOUT, OUTPUT);

  int size = sizeof(data) / sizeof(data[0]);
  for (int i = 0; i < size; i++) {
    unsigned long len = data[i] * 10;
    unsigned long us = micros();

    do {
      digitalWrite(ROBIP_IR_GPOUT, 1 - (i & 1));
      delayMicroseconds(8);

      digitalWrite(ROBIP_IR_GPOUT, 0);
      delayMicroseconds(7);
    } while (long(us + len - micros()) > 0);
  }
}
