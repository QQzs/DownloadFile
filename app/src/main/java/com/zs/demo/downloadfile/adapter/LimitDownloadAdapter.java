package com.zs.demo.downloadfile.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.zs.demo.downloadfile.LimitActivity;
import com.zs.demo.downloadfile.R;
import com.zs.demo.downloadfile.download.DownloadInfo;
import com.zs.demo.downloadfile.download.DownloadManager;

import java.util.List;

/**
 * Created by zs
 * Date：2018年 09月 11日
 * Time：18:06
 * —————————————————————————————————————
 * About:
 * —————————————————————————————————————
 */
public class LimitDownloadAdapter extends RecyclerView.Adapter<LimitDownloadAdapter.UploadHolder>  {

    private Context mContext;
    private List<DownloadInfo> mdata;

    public LimitDownloadAdapter(Context context , List<DownloadInfo> mdata) {
        this.mContext = context;
        this.mdata = mdata;
    }

    /**
     * 更新下载进度
     * @param info
     */
    public void updateProgress(DownloadInfo info){
        for (int i = 0; i < mdata.size(); i++){
            if (mdata.get(i).getUrl().equals(info.getUrl())){
                mdata.set(i,info);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public UploadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.item_download_layout,null);
        return new UploadHolder(view);
    }

    @Override
    public void onBindViewHolder(UploadHolder holder, int position) {

        final DownloadInfo info = mdata.get(position);
        if (DownloadInfo.DOWNLOAD_CANCEL.equals(info.getDownloadStatus())){
            holder.main_progress.setProgress(0);
        }else if (DownloadInfo.DOWNLOAD_OVER.equals(info.getDownloadStatus())){
            holder.main_progress.setProgress(holder.main_progress.getMax());
        }else {
            if (info.getTotal() == 0){
                holder.main_progress.setProgress(0);
            }else {
                float d = info.getProgress() * holder.main_progress.getMax() / info.getTotal();
                holder.main_progress.setProgress((int) d);
            }
        }
        holder.main_btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((LimitActivity)mContext).download(info.getUrl());
            }
        });

        holder.main_btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadManager.getInstance().pauseDownload(info.getUrl());
            }
        });

        holder.main_btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadManager.getInstance().cancelDownload(info);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mdata.size();
    }

    public class UploadHolder extends RecyclerView.ViewHolder{

        private ProgressBar main_progress;
        private Button main_btn_down;
        private Button main_btn_pause;
        private Button main_btn_cancel;

        public UploadHolder(View itemView) {
            super(itemView);
            main_progress = itemView.findViewById(R.id.main_progress);
            main_btn_down = itemView.findViewById(R.id.main_btn_down);
            main_btn_pause = itemView.findViewById(R.id.main_btn_pause);
            main_btn_cancel = itemView.findViewById(R.id.main_btn_cancel);
        }
    }

}
