package com.example.controlmunhecamano;

/**
 * Defines several constants used between {@link Configuracion_Servicio_BT} and the UI.
 */
public interface Constants {

    // Message types sent from the BluetoothChatService Handler
    // Defines several constants used when transmitting messages between the
    // service and the UI.
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

}