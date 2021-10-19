package com.example.chatblenativo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Inicio extends AppCompatActivity {

    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        context = this;
    }


    public void MainAct(View view){
        Intent siguiente = new Intent(context, MainActivity.class);
        startActivity(siguiente);
    }
}