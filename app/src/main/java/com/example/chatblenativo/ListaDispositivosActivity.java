package com.example.chatblenativo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Set;

public class ListaDispositivosActivity extends AppCompatActivity {

    private ListView lista_dispositivos_emparejados,lista_dispositivos_disponibles;
    private ProgressBar progressBarScanDevices;

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
        progressBarScanDevices = findViewById(R.id.barra_progreso_escaneo_dispositivos);


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

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener, intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener, intentFilter1);
    }

    private BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    adapterDispositivosDisponibles.add(device.getName() + "\n" + device.getAddress());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                progressBarScanDevices.setVisibility(View.GONE);

                if(adapterDispositivosDisponibles.getCount() == 0){
                    Toast.makeText(context, "No hay nuevos dispositivos encontrados", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context, "Clic en el dispositivo para comenzar el chat", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.menu_scan_devices:
                EscanearDispositivos();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void EscanearDispositivos(){
        progressBarScanDevices.setVisibility(View.VISIBLE);
        adapterDispositivosDisponibles.clear();
        Toast.makeText(context, "Escaneo iniciado", Toast.LENGTH_SHORT).show();

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }
}