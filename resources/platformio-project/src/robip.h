#ifndef _ROBIP_H_
#define _ROBIP_H_

typedef struct {
  double yaw;
  double pitch;
  double roll;
} RobipMotion;

void robip_setupWifi();
void robip_update();

void robip_currentMotion(RobipMotion *motion);
RobipMotion robip_getCurrentMotion();
size_t robip_serialWrite(int n);
size_t robip_serialWrite(double n);
size_t robip_serialWrite(char *s);

#endif  /* _ROBIP_H_ */
