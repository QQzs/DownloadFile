package com.zs.demo.downloadfile;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private RxPermissions mRxPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRxPermission = new RxPermissions(this);
    }

    public void singleDownload(View view){

        mRxPermission.request(Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean){
                            Intent intent = new Intent(MainActivity.this,SingleActivity.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(MainActivity.this , "请打开读写权限" , Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void multipleDownload(View view){
        mRxPermission.request(Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean){
                            Intent intent = new Intent(MainActivity.this,MultipleActivity.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(MainActivity.this , "请打开读写权限" , Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void limitDownload(View view){
        mRxPermission.request(Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean){
                            Intent intent = new Intent(MainActivity.this,LimitActivity.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(MainActivity.this , "请打开读写权限" , Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}