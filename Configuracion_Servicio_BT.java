package com.example.controlmunhecamano;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Configuracion_Servicio_BT {
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // UUID (Universally unique identifier) para esta aplicación
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Atributos
    private final BluetoothAdapter mAdapter;
    /*
     * Permiten poner en cola una acción que se realiza en un subproceso distinto al suyo.
     * Es aquella que permite manejar y procesar mensajes, proporcionando un mecanismo para
     * su envío (a modo de puente) entre threads o hilos, y así poder enviar mensajes desde nuestro hilo secundario al UIThread o hilo principal.
     * https://academiaandroid.com/multitarea-android-clases-asynctask-thread-handler-runnable/
     */
    private final Handler mHandler; //Cada instancia de Handler está asociada con un solo hilo y la cola de mensajes de ese hilo
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;

    // Constantes que indican el estado actual de la conexión
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepara una sesion nueva de BluetoothChat.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public Configuracion_Servicio_BT(Context context, Handler handler) {
        /*
         * BluetoothAdapter = Represents the local device Bluetooth adapter. The BluetoothAdapter lets you perform fundamental
         * Bluetooth tasks, such as initiate device discovery, query a list of bonded (paired) devices, instantiate a BluetoothDevice
         * using a known MAC address, and create a BluetoothServerSocket to listen for connection requests from other devices,
         * and start a scan for Bluetooth LE devices.
         * https://developer.android.com/reference/android/bluetooth/BluetoothAdapter
         */
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler; //Aquí se instancia la clase handler al igualarla al parametro que se pasa al constructor de BlueetoothChat
    }

    /**
     * Actualiza UI title de acuerdo con el estado actual de la conexión
     * La palabra reservada synchronized se usa para indicar que ciertas partes del código,
     * (habitualmente, una función miembro) están sincronizadas, es decir, que solamente
     * un subproceso puede acceder a dicho método a la vez.
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;
    }

    /**
     * Devuelve el estado actual de connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Arranca la comunicación como servidor. Especificamente arranca AcceptThread para comenzar
     * una sesion en modo servidor. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Inicia el metodo run() de ConnectThread para iniciar la conexión con un dispositivo.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure); // Cuando se ha preparado
        // se crea un objeto de la clase ConnectThread
        /*
         * Como ConnectThread es una subclase de Thread se pueden usar los métodos de esta última con objetos de la clase ConnectThread
         * The start() method of thread class is used to begin the execution of thread.
         * The result of this method is two threads that are running concurrently:
         * the current thread (which returns from the call to the start method) and the other thread (which executes its run method)
         * The start() method internally calls the run() method to execute the code specified in the run() method in a separate thread.
         */
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Inicia ConnectedThread para comenzar a manejar la conexión Bluetooth
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        //ESTA PARTE ES QUIZÁS LA QUE NO PERMITE CONECTAR AL ARDUINO
        /*
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

         */

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Detiene todos los hilos activos
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indica que el intento de conexión ha fallado y notifica a UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        Configuracion_Servicio_BT.this.start();
    }

    /**
     * Indica que se perdió la conexión y notifica a UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST); //Hay un select case del que se obtiene el msg al pasar el codigo que da la constante Constants.MESSAGE_TOAST
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        Configuracion_Servicio_BT.this.start();
    }

    /**
     * Extiende a la clase Thread.
     * Este hilo se ejecuta al intentar hacer una conexión saliente con un dispositivo.
     * Corre independientemente de que la conexión tenga exito o falle.
     */
    private class ConnectThread extends Thread {
        //Atributos de la clase ConnectThread
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        //Constructor de la clase
        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                if (secure) {
                    /*
                     * Este método inicializa un objeto BluetoothSocket que le permite al cliente conectarse con un BluetoothDevice.
                     * El UUID que se pasa aquí debe coincidir con el UUID empleado por el dispositivo del servidor cuando hizo la
                     * llamada a listenUsingRfcommWithServiceRecord(String, UUID) para abrir su BluetoothServerSocket.
                     * Para usar un UUID que coincida, codifica la cadena del UUID en tu aplicación y, luego, haz referencia a ella en el código del servidor y el cliente.
                     */
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp; //Socket
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                /*
                 * Después de que el cliente llama a este método, el sistema realiza una búsqueda de SDP para encontrar un dispositivo remoto que tenga el mismo UUID.
                 * Si la búsqueda es correcta y el dispositivo remoto acepta la conexión, este último comparte el canal RFCOMM que se usará durante la conexión, y se devuelve el método connect().
                 * Si la conexión no funciona o si se agota el tiempo de espera del método connect() (después de unos 12 segundos), dicho método devuelve una IOException.
                 */
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (Configuracion_Servicio_BT.this) {
                mConnectThread = null; // Hay que igualar a null para que al lanzar la función connected no se cierre el socket establecido
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * Este hilo se ejecuta durante una conexión con un dispositivo remoto.
     * Maneja todas las transmisiones entrantes y salientes, básicamente gestiona la entrada y salida de mensajes cuando existe un socket de comunicacion
     * entre al menos dos dispositivos.
     * El procedimiento general para transferir datos es el siguiente:
     * Obtén los objetos InputStream y OutputStream que administran transmisiones a través del socket mediante getInputStream() y getOutputStream(), respectivamente.
     * Lee y escribe datos en las emisiones con read(byte[]) y write(byte[]).
     *
     * Por supuesto, existen detalles relacionados con la implementación que deben tenerse en cuenta. En especial, debes usar un subproceso dedicado para leer de la emisión y escribir en ella.
     * Esto es importante porque los métodos read(byte[]) y write(byte[]) son llamadas de bloqueo.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] buffer; // buffer store for the stream

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Se obtienen las secuencias de entrada y salida de BluetoothSocket
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            buffer = new byte[1024]; //Buffer de 1024 bytes, un char son 16bits
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * Llamar a este metodo desde main activity para enviar datos por BT
         *
         * @param buffer The bytes to write         *
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        //Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
