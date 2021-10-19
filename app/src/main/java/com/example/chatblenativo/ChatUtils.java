package com.example.chatblenativo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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

public class ChatUtils {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    private final UUID app_uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private final String app_name="ChatBleNativo";

    public static final int estado_none = 0;
    public static final int estado_escuchar = 1;
    public static final int estado_conectando = 2;
    public static final int estado_conectado = 3;

    private int estado;

    public ChatUtils(Context context, Handler handler){
        this.context = context;
        this.handler = handler;

        estado= estado_none;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getEstado(){
        return estado;
    }

    public synchronized void setEstado(int estado){
        this.estado = estado;
        handler.obtainMessage(MainActivity.mensaje_estado_cambiado, estado, -1).sendToTarget();
    }

    private synchronized void start(){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread==null) {
            acceptThread=new AcceptThread();
            acceptThread.start();
        }

        if(connectedThread!=null){
            connectedThread.cancel();
            connectedThread= null;
        }
        setEstado(estado_escuchar);
    }

    public synchronized void stop(){
        if(connectThread != null){
            connectThread.cancel();
            connectThread=null;
        }

        if(acceptThread!=null){
            acceptThread.cancel();
            acceptThread=null;
        }

        if(connectedThread!=null){
            connectedThread.cancel();
            connectedThread= null;
        }

        setEstado(estado_none);
    }


    public void connect(BluetoothDevice device){
        if(estado== estado_conectando){
            connectThread.cancel();
            connectThread=null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        if(connectedThread!=null){
            connectedThread.cancel();
            connectedThread= null;
        }

        setEstado(estado_conectando);
    }

    public void write(byte[] buffer){
        ConnectedThread connThread;
        synchronized (this){
            if(estado!=estado_conectado){
                return;
            }

            connThread=connectedThread;
        }

        connThread.write(buffer);
    }


    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(app_name, app_uuid);
            } catch (IOException e) {
                Log.e("Aceptar->constructor", e.toString());
            }

            serverSocket = tmp;
        }


        public void run(){
            BluetoothSocket socket = null;
            try {
                socket = serverSocket.accept();
            }catch (IOException e){
                Log.e("Aceptar->Run", e.toString());
                try {
                    serverSocket.close();
                }catch (IOException e1){
                    Log.e("Aceptar->Close", e.toString());
                }
            }

            if(socket!=null){
                switch (estado){
                    case estado_escuchar:
                    case estado_conectando:
                        connect(socket.getRemoteDevice());
                        break;
                    case estado_none:;
                    case estado_conectado:
                        try {
                            socket.close();
                        }catch (IOException e){
                            Log.e("Aceptar->CloseSocket", e.toString());
                        }
                        break;
                }
            }

        }

        public void cancel(){
            try {
                serverSocket.close();
            }catch (IOException e){
                Log.e("Aceptar->CloseSocket", e.toString());
            }
        }
    }



    private class ConnectThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device){
            this.device = device;

            BluetoothSocket tmp = null;

            try {
                tmp=device.createRfcommSocketToServiceRecord(app_uuid);
            }catch (IOException e){
                Log.e("Conectado->constructor", e.toString());
            }

            socket = tmp;
        }

        public void run(){
            try {
                socket.connect();
            }catch (IOException e){
                /*ERROR*/
                Log.e("Conectado->Run", e.toString());
                try {
                    socket.close();
                }catch (IOException e1){
                    Log.e("Conectado->CloseSocket", e.toString());
                }
                conexionfallida();
                return;
            }

            synchronized (ChatUtils.this){
                connectThread =null;
            }

            connected(socket,device);
        }

        public void cancel(){
            try {
                socket.close();
            }catch (IOException e){
                Log.e("Conectado->Cancel", e.toString());
            }
        }
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket){
            this.socket= socket;

            InputStream tmpIn = null;
            OutputStream tmpOut= null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){

            }

            inputStream=tmpIn;
            outputStream=tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                bytes = inputStream.read(buffer);

                handler.obtainMessage(MainActivity.mensaje_lectura,bytes, -1, buffer).sendToTarget();
            }catch (IOException e){
                connectionLost();
            }
        }

        public void write(byte[] buffer){
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.mensaje_escritura,-1, -1,buffer).sendToTarget();

            }catch (IOException e){

            }
        }

        public void cancel(){
            try {
                socket.close();
            }catch (IOException e){

            }
        }
    }

    private void connectionLost(){
        Message message = handler.obtainMessage(MainActivity.mensaje_toast);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST,"Conexión perdida");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void conexionfallida(){
        Message message = handler.obtainMessage(MainActivity.mensaje_toast);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "No se logro conectar el dispositivo");
        message.setData(bundle);
        handler.sendMessage(message);
        ChatUtils.this.start();
    }

    private synchronized void connected(BluetoothSocket socket,BluetoothDevice device){
        if (connectThread!=null){
            connectThread.cancel();
            connectThread=null;
        }

        if(connectedThread!=null){
            connectedThread.cancel();
            connectedThread= null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.mensaje_nombre_dispositivo);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.nombre_dispositivo,device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setEstado(estado_conectado);
    }

}
