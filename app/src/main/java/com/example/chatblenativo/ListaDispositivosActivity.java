package com.example.chatblenativo;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Set;

public class ListaDispositivosActivity extends AppCompatActivity {

    private ListView lista_dispositivos_emparejados,lista_dispositivos_disponibles;
    private ArrayAdapter<String> adapterDispositivosEmparejados, adapterDispositivosDisponibles;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_dispositivos);

        context = this;
        init();
    }

    private void init(){
        lista_dispositivos_emparejados = findViewById(R.id.lista_dispositivos_emparejados);
        lista_dispositivos_disponibles = findViewById(R.id.lista_dispositivos_disponibles);

        adapterDispositivosEmparejados = new ArrayAdapter<String>(context,R.layout.device_list_item);
        adapterDispositivosDisponibles = new ArrayAdapter<String>(context,R.layout.device_list_item);

        lista_dispositivos_emparejados.setAdapter(adapterDispositivosEmparejados);
        lista_dispositivos_disponibles.setAdapter(adapterDispositivosDisponibles);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> dispositivosEmparejados = bluetoothAdapter.getBondedDevices();

        if(dispositivosEmparejados!=null && dispositivosEmparejados.size()>0){
            for(BluetoothDevice device: dispositivosEmparejados){
                adapterDispositivosEmparejados.add(device.getName() + "\n" + device.getAddress());
            }


        }
    }
}