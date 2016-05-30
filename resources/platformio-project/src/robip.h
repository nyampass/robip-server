#ifndef _ROBIP_H_
#define _ROBIP_H_

void robip_setupWifi();
void robip_update();

size_t robip_serialWrite(int n);
size_t robip_serialWrite(double n);
size_t robip_serialWrite(char *s);

void robip_sendIR(unsigned int data[], int length);

#endif  /* _ROBIP_H_ */
