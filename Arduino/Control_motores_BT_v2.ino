#include <SoftwareSerial.h>
#include <LiquidCrystal.h>
#include <Servo.h>
//Declarar LCD y pines
LiquidCrystal lcd(7,6,5,4,3,2);

//Crea el objeto servo
Servo servo1;
Servo servo2;
int giroX_previo = 90;
int giroY_previo = 90;
int giroX = 0;
int giroY = 0;
int posServo1 = 90;
int posServo2 = 90;
int incrementoPosicion = 0;
bool inicio = 0;

typedef union {
    int number;
    byte bytes[2];
} INTUNION_t;

INTUNION_t giroX_union;
INTUNION_t giroY_union;

int calcular_desplazamiento(int posServo1, int posServo2, int giro){
  int desplazamiento1 = abs (posServo1 - giro);
  int desplazamiento2 = abs (posServo2 - giro);
  if(desplazamiento1 < desplazamiento2){
    return desplazamiento1;
  }
  else{
    return desplazamiento2;
  }
}

void setLCD(int posServo1, int posServo2){  
  lcd.setCursor(7,0);
  lcd.print("   ");
  lcd.setCursor(7,0);
  lcd.print(String(posServo1));
  lcd.setCursor(7,1);
  lcd.print("   ");
  lcd.setCursor(7,1);
  lcd.print(String(posServo2));
}

void setup() {
  //Definir las dimensiones del LCD (16x2)
  lcd.begin(16,2);
  
  //Comunicacion entre el modulo y el arduino
  Serial.begin(38400);

  //Se vincula el servo al pin digital 9
  servo1.attach(8);
  servo2.attach(9);  

  servo1.write(posServo1); 
  servo2.write(posServo2); 

  lcd.setCursor(4,0);
  lcd.print("Iniciado"); //Se imprime el dato en pantalla
  inicio = 0;
}

void loop() {     
  if(Serial.available()>=4) {
     
    for(int i = 1; i>=0; i--){
      giroX_union.bytes[i] = Serial.read(); 
    }
    for(int u = 1; u>=0; u--){
      giroY_union.bytes[u] = Serial.read(); 
    }

    giroX = giroX_union.number;
    giroY = giroY_union.number;

    if(inicio == 0){ //Se elimina lo que hay en toda la pantalla      
      lcd.clear();
      lcd.setCursor(0,0);
      lcd.print("Motor1:");
      lcd.setCursor(0,1);
      lcd.print("Motor2:");
      lcd.setCursor(7,0);
      inicio = 1;
    }    

    if(giroX != giroX_previo){
      incrementoPosicion = calcular_desplazamiento(posServo1, posServo2, giroX);

      if(giroX > giroX_previo){
        posServo1 = posServo1 + incrementoPosicion;
        posServo2 = posServo2 - incrementoPosicion; 
      }
      else{
        posServo1 = posServo1 - incrementoPosicion;
        posServo2 = posServo2 + incrementoPosicion; 
      }
       
      servo1.write(posServo1);
      servo2.write(posServo2);
      setLCD(posServo1, posServo2);

      giroX_previo = giroX;
      giroY_previo = giroY;
      
    } 
    else if(giroY != giroY_previo){      
      incrementoPosicion = calcular_desplazamiento(posServo1, posServo2, giroY);
      
      if(giroY > giroY_previo){
        posServo1 = posServo1 + incrementoPosicion;
        posServo2 = posServo2 + incrementoPosicion; 
      }
      else{
        posServo1 = posServo1 - incrementoPosicion;
        posServo2 = posServo2 - incrementoPosicion; 
      }
       
      servo1.write(posServo1);
      servo2.write(posServo2);
      setLCD(posServo1, posServo2);

      giroX_previo = giroX;
      giroY_previo = giroY;
    }
        
  }
}
