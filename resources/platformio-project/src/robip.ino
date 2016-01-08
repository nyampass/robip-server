#include "robip.h"

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
