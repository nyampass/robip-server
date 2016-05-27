#include <Wire.h>
 
#define    MPU9250_ADDRESS            0x69
#define    MAG_ADDRESS                0x0C
 
#define    GYRO_FULL_SCALE_250_DPS    0x00  
#define    GYRO_FULL_SCALE_500_DPS    0x08
#define    GYRO_FULL_SCALE_1000_DPS   0x10
#define    GYRO_FULL_SCALE_2000_DPS   0x18
 
#define    ACC_FULL_SCALE_2_G        0x00  
#define    ACC_FULL_SCALE_4_G        0x08
#define    ACC_FULL_SCALE_8_G        0x10
#define    ACC_FULL_SCALE_16_G       0x18
 
void I2Cread(uint8_t Address, uint8_t Register, uint8_t Nbytes, uint8_t* Data)
{
  Wire.beginTransmission(Address);
  Wire.write(Register);
  Wire.endTransmission();
 
  Wire.requestFrom(Address, Nbytes); 
  uint8_t index=0;
  while (Wire.available())
    Data[index++]=Wire.read();
}
 
void I2CwriteByte(uint8_t Address, uint8_t Register, uint8_t Data)
{
  Wire.beginTransmission(Address);
  Wire.write(Register);
  Wire.write(Data);
  Wire.endTransmission();
}
 
void robip_accelerator_setup()
{
  Wire.begin(4, 14);
  delay(40);
 
  I2CwriteByte(MPU9250_ADDRESS,27,GYRO_FULL_SCALE_2000_DPS);
  I2CwriteByte(MPU9250_ADDRESS,28,ACC_FULL_SCALE_16_G);
  I2CwriteByte(MPU9250_ADDRESS,0x37,0x02);
  // I2CwriteByte(MAG_ADDRESS,0x0A,0x01);
  I2CwriteByte(MAG_ADDRESS,0x0A,1 << 4 || 0x02);

  delay(10);
}

uint8_t robip_accelerator_buf[14];

void robip_accelerator_update() {
  I2Cread(MPU9250_ADDRESS, 0x3B, 14, robip_accelerator_buf);
}

float robip_accelerator_get(int highIndex, int lowIndex) {
  int16_t v = -(robip_accelerator_buf[highIndex]<<8 | robip_accelerator_buf[lowIndex]);
  return ((float)v) * 16.0/32768.0;
}

float robip_accelerator_x() {
  robip_accelerator_update();
  return robip_accelerator_get(0, 1);
}

float robip_accelerator_y() {
  robip_accelerator_update();
  return robip_accelerator_get(2, 3);
}

float robip_accelerator_z() {
  robip_accelerator_update();
  return robip_accelerator_get(4, 5);
}

float robip_accelerator_complex() {
  robip_accelerator_update();
  return sqrt(pow(robip_accelerator_get(0, 1), 2) +
    pow(robip_accelerator_get(2, 3), 2) +
    pow(robip_accelerator_get(4, 5), 2));
}

void robip_accelerator_print_xyz() {
  float x = robip_accelerator_x();
  float y = robip_accelerator_y();
  float z = robip_accelerator_z();

  Serial.print(x); 
  Serial.print(",");
  Serial.print(y);
  Serial.print(",");
  Serial.print(z);  
  Serial.print(": ");
  Serial.print(robip_accelerator_complex());
  Serial.println();
}

