#include <Arduino.h>
#include "User_Setup.h"

const int RELAY_PIN = 7;

void setup() {
    pinMode(RELAY_PIN, OUTPUT);
    Serial.begin(9600);
}

void loop() {
    while(Serial.available() > 0){
        char c = Serial.read();
        if(c == '\n'){
            digitalWrite(RELAY_PIN, HIGH);
            delay(700);
            digitalWrite(RELAY_PIN, LOW);
        }
    }
    delay(10);
}
