#include <ESP8266HTTPClient.h>

void robip_http_access(char* url) {
  HTTPClient http;
  http.begin(url);
  http.GET();
  http.end();
}
