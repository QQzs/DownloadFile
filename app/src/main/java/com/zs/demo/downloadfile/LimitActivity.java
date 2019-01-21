package com.zs.demo.downloadfile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.widget.Toast;

import com.zs.demo.downloadfile.adapter.LimitDownloadAdapter;
import com.zs.demo.downloadfile.download.DownloadInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zs
 * Date：2018年 09月 11日
 * Time：17:57
 * —————————————————————————————————————
 * About: 限制同时下载文件的最大个数
 * —————————————————————————————————————
 */
public class LimitActivity extends AppCompatActivity {

    private RecyclerView recycler_view;
    private ExecutorService mLimitThreadPool;
    private LimitDownloadAdapter mAdapter;
    private List<DownloadInfo> mData = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);
        EventBus.getDefault().register(this);
        recycler_view = findViewById(R.id.recycler_view);

        mData.add(new DownloadInfo(Constant.URL_1));
        mData.add(new DownloadInfo(Constant.URL_2));
        mData.add(new DownloadInfo(Constant.URL_3));
        mData.add(new DownloadInfo(Constant.URL_4));
        mData.add(new DownloadInfo(Constant.URL_5));
        mData.add(new DownloadInfo(Constant.URL_6));
        mData.add(new DownloadInfo(Constant.URL_7));
        mData.add(new DownloadInfo(Constant.URL_8));
        mData.add(new DownloadInfo(Constant.URL_9));


        mAdapter = new LimitDownloadAdapter(this,mData);
        recycler_view.setLayoutManager(new LinearLayoutManager(this));
        recycler_view.setAdapter(mAdapter);
        // 取消item刷新的动画
        ((SimpleItemAnimator)recycler_view.getItemAnimator()).setSupportsChangeAnimations(false);

        /**
         * newFixedThreadPool 可以控制最大并发数的线程池。超过最大并发数的线程进入等待队列。
         */
        mLimitThreadPool = Executors.newFixedThreadPool(2);

    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void update(DownloadInfo info){
        if (DownloadInfo.DOWNLOAD.equals(info.getDownloadStatus())){

            mAdapter.updateProgress(info);

        }else if (DownloadInfo.DOWNLOAD_OVER.equals(info.getDownloadStatus())){

            mAdapter.updateProgress(info);

        }else if (DownloadInfo.DOWNLOAD_PAUSE.equals(info.getDownloadStatus())){
            mAdapter.updateProgress(info);
            Toast.makeText(this,"下载暂停",Toast.LENGTH_SHORT).show();

        }else if (DownloadInfo.DOWNLOAD_CANCEL.equals(info.getDownloadStatus())){

            mAdapter.updateProgress(info);
            Toast.makeText(this,"下载取消",Toast.LENGTH_SHORT).show();

        }else if (DownloadInfo.DOWNLOAD_WAIT.equals(info.getDownloadStatus())){

            mAdapter.updateProgress(info);
            Toast.makeText(this,"等待下载",Toast.LENGTH_SHORT).show();

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
