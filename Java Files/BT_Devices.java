package com.example.controlmunhecamano;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class BT_Devices extends AppCompatActivity {
    /**
     * La clase Log te permite crear mensajes del registro que se muestran en logcat.
     * Logcat es una herramienta de línea de comandos que vuelca un registro de mensajes del sistema, incluidos los seguimientos
     * de pila, los casos de error del sistema y los mensajes que escribes desde tu app con la clase Log.
     * La etiqueta de un mensaje de registro del sistema es una string corta que indica el componente del sistema desde el que se origina el mensaje.
     */
    private static final String TAG = "BT_Devices"; // El TAG debe tener el nombre de la clase

    // String que se enviara a la actividad principal, mainactivity, es un identificador para colocar en intent.putExtra()
    public static String EXTRA_DEVICE_ADDRESS = "com.example.Covid19_chat.device_address";

    //Variables y Constantes
    static final byte REQUEST_ENABLE_BT = 1; // Constante
    private BluetoothAdapter mbtAdapter = null; //Variable del tipo BluetoothAdapter de nombre btAdapter

    // Declaracion de ListView
    ListView Lista_dispositivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btdevices);

        // Set result CANCELED en caso de que se vuelva a la actividad principal sin seleccionar un dispositivo
        setResult(Activity.RESULT_CANCELED);

        //---------------------------------
        mbtAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        VerificarEstadoBT();

        //------------------------------------------------------------------------------
        Set<BluetoothDevice> pairedDevices = mbtAdapter.getBondedDevices(); // el metodo getBondedDevices() devuelve un conjunto de objetos BluetoothDevice que representa a los dispositivos sincronizados y los guarda en paired devices.
        ArrayList<String> lista_dispositivos = new ArrayList<String>();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName(); // Nombre del dispositivo
                String deviceHardwareAddress = device.getAddress(); // direccion MAC
                lista_dispositivos.add( deviceName  + "\n" + deviceHardwareAddress);
            }
        }else {
            lista_dispositivos.add( "No Devices");
        }

        //------------------------------------------------------------------------------
        /*
         * Inicializa la array que contendra la lista de los dispositivos bluetooth vinculados
         * An ArrayAdapter converts an ArrayList of objects into View items loaded into the ListView container.
         * The ArrayAdapter fits in between an ArrayList (data source) and the ListView (visual representation)
         * Accepts three arguments: context (activity instance), XML item layout, and the array of data.
         * guides.codepath.com/android/Using-an-ArrayAdapter-with-ListView
         */
        ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter <String>(this, R.layout.devices_name, lista_dispositivos);
        Lista_dispositivos = (ListView) findViewById(R.id.Lista_dispositivos); //Se vincula la variable con el objeto view
        Lista_dispositivos.setAdapter(mPairedDevicesArrayAdapter); // Se fija el adapter

        //Al hacer click en un elemento de la lista se llama la funcion mDeviceClickListener
        Lista_dispositivos.setOnItemClickListener(mDeviceClickListener);
    }


    /**
     * Comprueba que el dispositivo Bluetooth está disponible y solicita que se active si está desactivado
     * Llama al metodo isEnabled() para verificar si Bluetooth se encuentra actualmente habilitado. Si este método muestra “false”,
     * Bluetooth no estará habilitado. Para solicitar que Bluetooth esté habilitado, llama a startActivityForResult() pasando
     * una acción de intent ACTION_REQUEST_ENABLE. Esto emite una solicitud para habilitar Bluetooth mediante la configuración
     * del sistema (sin detener tu aplicación).
     */
    public void VerificarEstadoBT() {
        if(mbtAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (mbtAdapter.isEnabled()) { //si btadapter está enabled, devuelve TRUE
                Log.d(TAG, "...Bluetooth Activado...");
            }
            else{
                //Solicita al usuario que active Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                /*
                 * Se muestra un cuadro de diálogo en el que se solicita permiso al usuario
                 * para habilitar Bluetooth. Si el usuario responde “Sí”, el sistema comienza a habilitar Bluetooth y el enfoque vuelve a tu aplicación
                 * una vez que el proceso se completa con éxito (o no).
                 * La constante requestCode : 1 que se pasa a startActivityForResult() es un valor entero definido localmente que debe ser superior a 0.
                 * Como caso especial, si llama a startActivityForResult() con requestCode >= 0 durante el onCreate inicial (Bundle savedInstanceState)/onResume()
                 * de su actividad, su ventana no se mostrará hasta que se devuelva un resultado de la actividad iniciada.
                 * Esto es para evitar parpadeos visibles al redirigir a otra actividad
                 */
            }
        }
    }

    // Configura un (on-click) para la lista
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
            // Obtener la dirección MAC del dispositivo, que son los últimos 17 caracteres en la vista
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Realiza un intent para iniciar la siguiente actividad
            // mientras toma un EXTRA_DEVICE_ADDRESS que es la dirección MAC.
            Intent i = new Intent(BT_Devices.this, MainActivity.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, i);
            finish();
        }
    };
}

