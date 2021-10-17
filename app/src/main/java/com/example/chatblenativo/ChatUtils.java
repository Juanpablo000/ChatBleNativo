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
import java.util.UUID;

public class ChatUtils {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectedThread connectedThread;
    private AcceptThread acceptThread;

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
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread==null) {
            acceptThread=new AcceptThread();
            acceptThread.start();
        }

        setEstado(estado_escuchar);
    }

    public synchronized void stop(){
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread=null;
        }

        if(acceptThread!=null){
            acceptThread.cancel();
            acceptThread=null;
        }

        setEstado(estado_none);
    }


    public void connect(BluetoothDevice device){
        if(estado== estado_conectando){
            connectedThread.cancel();
            connectedThread=null;
        }

        connectedThread = new ConnectedThread(device);
        connectedThread.start();

        setEstado(estado_conectando);
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



    private class ConnectedThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectedThread(BluetoothDevice device){
            this.device = device;

            BluetoothSocket tmp = null;

            try {
                tmp=device.createInsecureRfcommSocketToServiceRecord(app_uuid);
            }catch (IOException e){
                Log.e("Conectado->constructor", e.toString());
            }

            socket = tmp;
        }

        public void run(){
            try {
                socket.connect();
            }catch (IOException e){
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
                connectedThread =null;
            }

            connected(device);
        }

        public void cancel(){
            try {
                socket.close();
            }catch (IOException e){
                Log.e("Conectado->Cancel", e.toString());
            }
        }
    }


    private synchronized void conexionfallida(){
        Message message = handler.obtainMessage(MainActivity.mensaje_toast);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "No se logro conectar el dispositivo");
        message.setData(bundle);
        handler.sendMessage(message);
        ChatUtils.this.start();
    }

    private synchronized void connected(BluetoothDevice device){
        if (connectedThread!=null){
            connectedThread.cancel();
            connectedThread=null;
        }

        Message message = handler.obtainMessage(MainActivity.mensaje_nombre_dispositivo);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.nombre_dispositivo,device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setEstado(estado_conectado);
    }

}
