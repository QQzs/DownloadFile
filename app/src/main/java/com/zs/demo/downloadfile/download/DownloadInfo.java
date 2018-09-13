package com.zs.demo.downloadfile.download;

/**
 * Created by xiaobai on 2018/3/30/030.
 */

public class DownloadInfo {
    public static final long TOTAL_ERROR = -1;//获取进度失败

    public static final String DOWNLOAD = "download";
    public static final String DOWNLOAD_PAUSE = "pause";
    public static final String DOWNLOAD_CANCEL = "cancel";
    public static final String DOWNLOAD_OVER = "over";
    public static final String DOWNLOAD_ERROR = "error";

    private String url;
    private String fileName;
    private String downloadStatus;
    private long total;
    private long progress;

    public DownloadInfo(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public String getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(String downloadStatus) {
        this.downloadStatus = downloadStatus;
    }
}
