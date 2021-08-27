package com.example.controlmunhecamano;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    // Depuración de LOGCAT
    private static final String TAG = "MainActivity";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    //Botones y demas elementos del UI
    private TextView textView_PosicionX;
    private TextView textView_PosicionY;
    private SeekBar seekBar_PosicionX;
    private SeekBar seekBar_PosicionY;

    // Nombre del dispositivo conectado
    private String mConnectedDeviceName = null;

    //String buffer para comandos salientes
    private StringBuffer mOutStringBuffer;

    //Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null; //Objeto del tipo BluetoothAdapter de nombre btAdapter

    // Objeto instancia de la clase Configuracion_Servicio_BT
    private Configuracion_Servicio_BT mConfifBT = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false); //Para ocultar el nombre de la aplicación por defecto

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Se comprueba que btAdapter distinto de null
        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        }

        //Enlaza los controles con sus respectivas vistas
        textView_PosicionX = findViewById(R.id.textView_PosicionX);
        textView_PosicionY = findViewById(R.id.textView_PosicionY);
        seekBar_PosicionX = findViewById(R.id.seekBar_PosicionX);
        seekBar_PosicionY = findViewById(R.id.seekBar_PosicionY);

        //Se muestra el valor del seekBar en el textview
        short posicionX = (short) seekBar_PosicionX.getProgress();
        String textX = getString(R.string.textView_PosicionX);
        textX += posicionX;
        textView_PosicionX.setText(textX);

        short posicionY = (short) seekBar_PosicionY.getProgress();
        String textY = getString(R.string.textView_PosicionY);
        textY += posicionY;
        textView_PosicionY.setText(textY);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mBluetoothAdapter == null) {
            return;
        }
        // Si BT no esta encendido, se solicita activarlo.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) { //si btadapter no está enabled, devuelve FALSE
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mConfifBT == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConfifBT != null) {
            mConfifBT.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Realizar esta comprobación en onResume () cubre el caso
        // en el que BT no estaba habilitado durante onStart (),
        // por lo que nos detuvimos para habilitarlo ...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mConfifBT != null) {
            // Solo si el estado es STATE_NONE, sabemos que aún no hemos comenzado
            if (mConfifBT.getState() == Configuracion_Servicio_BT.STATE_NONE) {
                // Start the Bluetooth chat services
                mConfifBT.start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bt, menu);
        return true;
    }

    //Se crea una funcion cuyo unico cometido es cambiar a la actividad BT_Devices al pulsar el boton IconoBluetooth
    public void conectar_dispositivo(){
        Intent serverIntent = new Intent(this, BT_Devices.class); //Creamos un Intent, al que se le pasa como parámetros la actividad actual (this) y la actividad a la que queremos pasar (en este caso DisplayMessageActivity.class).
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE); //iniciamos la nueva actividad pasando como parámetro el Intent que hemos creado.
        /*
         * Launch an activity for which you would like a result when it finished. When this activity exits, your onActivityResult() method will be called with the given requestCode.
         * Parametros:
         *  1) Intent: The intent to start.
         *  2) requestCode: this code will be returned in onActivityResult() when the activity exits.         *
         */
    }

    //Metodo que permite elegir que pasa al hacer click en los items del menu, suele utilizarse con un switch case
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.IconoBluetooth:
                conectar_dispositivo();
                return true;
            case R.id.IconoBluetoothDisable:
                mConfifBT.stop();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Configura el background de la UI para el chat.
    private void setupChat() {
        Log.d(TAG, "setupChat()");
        // Initialize the array adapter for the conversation thread
        Context activity = getBaseContext();
        if (activity == null) {
            return;
        }
        // Listeners para los SeekBars
        seekBar_PosicionX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            short progressChangedValueX = 200; //Se inicia a 200
            String textX; //Este String almacenara el mensaje de textView_PosicionX  + el valor de textView_DisplaySeek_Value
            String textView_DisplaySeek_Value;
            //Este metodo listener se invocara cuando cualquier cambio ocurra en el SeekBar
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValueX = (short) progress; //Se convierte el int a short
                textView_DisplaySeek_Value = Short.toString(progressChangedValueX);
                textX = getString(R.string.textView_PosicionX );
                textX += textView_DisplaySeek_Value;
                textView_PosicionX.setText(textX);
            }
            //This listener method will be invoked at the start of user’s touch event. Whenever a user touch the thumb for dragging this method will automatically called.
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            //This listener method will be invoked at the end of user touch event. Whenever a user stop dragging the thump this method will be automatically called.
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendMessage(progressChangedValueX, false); //Posicion actual del SeekBar
            }
        });

        seekBar_PosicionY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            short progressChangedValueY = 200; //Se inicia a 200
            char Y;
            String textY; //Este String almacenara el mensaje de texView_PosicionY  + el valor de textView_DisplaySeek_Value
            String textView_DisplaySeek_Value;
            //Este metodo listener se invocara cuando cualquier cambio ocurra en el SeekBar
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValueY = (short) progress; //Se convierte el int a short
                textView_DisplaySeek_Value = Short.toString(progressChangedValueY);
                textY = getString(R.string.textView_PosicionY);
                textY += textView_DisplaySeek_Value;
                textView_PosicionY.setText(textY);
            }
            //This listener method will be invoked at the start of user’s touch event. Whenever a user touch the thumb for dragging this method will automatically called.
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            //This listener method will be invoked at the end of user touch event. Whenever a user stop dragging the thump this method will be automatically called.
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendMessage(progressChangedValueY, true); //Posicion actual del SeekBar
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mConfifBT = new Configuracion_Servicio_BT(activity, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer();
    }

    //Envía la posicion.
    private void sendMessage(short posicion, boolean nombre_eje) {
        // Check that we're actually connected before trying anything
        if (mConfifBT.getState() != Configuracion_Servicio_BT.STATE_CONNECTED) {
            Toast.makeText(getBaseContext(), "No Conectado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Se comprueba que posicion sea distinto de 200 (valor por defecto)
        if (posicion != 200) {
            //Valor que se enviara al arduino
            short number;
            try
            {
                number = posicion;
            }
            catch (NumberFormatException nfe)
            {
                Toast.makeText(getBaseContext(), "Introduce un número entre 0 y 180 ", Toast.LENGTH_SHORT).show();
                //Si falla se pone number a 200 para no enviar nada
                number = 200;
            }

            //Solo si el valor está entre 0 y 180 se envía
            if (number >= 0 && number <= 180){
                if (!nombre_eje){
                    short posicionY = (short) seekBar_PosicionY.getProgress();
                    byte[] send = ByteBuffer.allocate(4).putShort(number).putShort(posicionY).array();
                    //Tambien se pondria poner .allocate(number.length + posicionY.length)
                    mConfifBT.write(send);
                }else{
                    short posicionX = (short) seekBar_PosicionX.getProgress();
                    byte[] send = ByteBuffer.allocate(4)
                            .putShort(posicionX).putShort(number)
                            .array();
                    mConfifBT.write(send);
                }
            }else{
                Toast.makeText(getBaseContext(), "Introduce un número entre 0 y 180 ", Toast.LENGTH_SHORT).show();
            }

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Context activity = getBaseContext();
            switch (msg.what) {
                case Constants.MESSAGE_WRITE:
                    break;
                /*case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mComandosArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;

                 */
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_CONNECT_DEVICE_SECURE:
                // When BT_Devices returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Context activity = getBaseContext();
                    if (activity != null) {
                        Toast.makeText(activity, "BT not enable",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        }
    }

    /**
     * Establish connection with other device
     *  @param data   An {@link Intent} with {@link BT_Devices#EXTRA_DEVICE_ADDRESS} extra.
     *
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(BT_Devices.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mConfifBT.connect(device, true);
    }
}

