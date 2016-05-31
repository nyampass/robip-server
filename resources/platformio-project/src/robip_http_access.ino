#include <ESP8266HTTPClient.h>

String robip_http_access(char* url) {
  String response = "";

  HTTPClient http;
  http.begin(url);

  if(http.GET() == 200) {
	response = http.getString();
  }

  http.end();

  return response;
}
