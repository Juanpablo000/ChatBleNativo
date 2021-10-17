package com.example.chatblenativo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtils chatUtils;

    private final int location_permission_request=101;
    private final int select_device =102;

    public static final int mensaje_estado_cambiado = 0;
    public static final int mensaje_lectura = 1;
    public static final int mensaje_escritura = 2;
    public static final int mensaje_nombre_dispositivo = 3;
    public static final int mensaje_toast = 4;

    public static final String nombre_dispositivo = "nombreDispositivo";
    public static final String TOAST = "toast";

    private String dispositivoConectado;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what){
                case mensaje_estado_cambiado:
                    switch (message.arg1){
                        case ChatUtils.estado_none:
                            setEstado("No conectado");
                            break;
                        case ChatUtils.estado_escuchar:
                            setEstado("No conectado");
                            break;
                        case ChatUtils.estado_conectando:
                            setEstado("Conectando...");
                            break;
                        case ChatUtils.estado_conectado:
                            setEstado("Conectado: " + dispositivoConectado);
                            break;
                    }
                    break;
                case mensaje_lectura:
                    break;
                case mensaje_escritura:
                    break;
                case mensaje_nombre_dispositivo:
                    dispositivoConectado = message.getData().getString(nombre_dispositivo);
                    Toast.makeText(context, dispositivoConectado, Toast.LENGTH_SHORT).show();
                    break;
                case mensaje_toast:
                    Toast.makeText(context,message.getData().getString(TOAST),Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setEstado(CharSequence subTitle){
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        initBluetooth();
        chatUtils = new ChatUtils(context, handler);
    }

    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_buscar_dispositivos:
                revisarPermisos();
                return true;
            case R.id.menu_enable_bluetooth:
                habilitarBluetooth();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void revisarPermisos(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, location_permission_request);
        }else{
            Intent intent = new Intent(context, ListaDispositivosActivity.class);
            startActivityForResult(intent, select_device);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == select_device && resultCode == RESULT_OK) {
            String address = data.getStringExtra("deviceAddress");
            //Toast.makeText(context, "Dirección: " + address, Toast.LENGTH_SHORT).show();
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == location_permission_request){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(context, ListaDispositivosActivity.class);
                startActivityForResult(intent,select_device);
            }else{
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Permiso local es requerido. \n Por favor suministre permisos")
                        .setPositiveButton("Concedido", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                revisarPermisos();
                            }
                        })
                        .setNegativeButton("Denegado", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void habilitarBluetooth(){
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }else{
            Toast.makeText(context, "Bluetooth ya esta activado", Toast.LENGTH_SHORT).show();
        }

        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoveryIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatUtils!=null){
            chatUtils.stop();
        }
    }
}













