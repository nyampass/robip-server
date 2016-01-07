#ifndef _ROBIP_H_
#define _ROBIP_H_

typedef struct {
  float yaw;
  float pitch;
  float roll;
} RobipMotion;

void robip_currentMotion(RobipMotion *motion);
RobipMotion robip_getCurrentMotion();

#endif  /* _ROBIP_H_ */
