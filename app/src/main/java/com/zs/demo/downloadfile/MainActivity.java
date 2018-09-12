package com.zs.demo.downloadfile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void singleDownload(View view){
        Intent intent = new Intent(this,SingleActivity.class);
        startActivity(intent);
    }

    public void multipleDownload(View view){
        Intent intent = new Intent(this,ListActivity.class);
        startActivity(intent);
    }
}
