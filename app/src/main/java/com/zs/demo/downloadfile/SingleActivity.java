package com.zs.demo.downloadfile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.zs.demo.downloadfile.download.DownloadInfo;
import com.zs.demo.downloadfile.download.DownloadManager;
import com.zs.demo.downloadfile.download.DownloadObserver;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by zs
 * Date：2018年 09月 12日
 * Time：17:17
 * —————————————————————————————————————
 * About:
 * —————————————————————————————————————
 */
public class SingleActivity extends AppCompatActivity{

    private ProgressBar main_progress;
    private Button main_btn_down;
    private Button main_btn_cancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
        EventBus.getDefault().register(this);
        main_progress = findViewById(R.id.main_progress);
        main_btn_down = findViewById(R.id.main_btn_down);
        main_btn_cancel = findViewById(R.id.main_btn_cancel);

        main_btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadManager.getInstance().download(Constant.URL_1, new DownloadObserver());
            }
        });

        main_btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadManager.getInstance().cancel(Constant.URL_1);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void update(DownloadInfo info){
        if (info.getUrl() != Constant.URL_1){
            return;
        }
        if (DownloadInfo.DOWNLOAD.equals(info.getDownloadStatus())){

            if (info.getTotal() == 0){
                main_progress.setProgress(0);
            }else{
                float progress = info.getProgress() * main_progress.getMax() / info.getTotal();
                main_progress.setProgress((int) progress);
            }

        }else if (DownloadInfo.DOWNLOAD_OVER.equals(info.getDownloadStatus())){

            main_progress.setProgress(main_progress.getMax());

        }else if (DownloadInfo.DOWNLOAD_WAIT.equals(info.getDownloadStatus())){

            Toast.makeText(this,"下载取消",Toast.LENGTH_SHORT).show();

        }else if (DownloadInfo.DOWNLOAD_ERROR.equals(info.getDownloadStatus())){

            Toast.makeText(this,"下载出错",Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
