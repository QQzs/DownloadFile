package com.zs.demo.downloadfile.download.limit;

import com.zs.demo.downloadfile.Constant;
import com.zs.demo.downloadfile.download.DownloadIO;
import com.zs.demo.downloadfile.download.DownloadInfo;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zs
 * Date：2018年 09月 12日
 * Time：13:56
 * —————————————————————————————————————
 * About: 下载管理（限制最大个数）
 * —————————————————————————————————————
 */
public class DownloadLimitManager {

    private static final AtomicReference<DownloadLimitManager> INSTANCE = new AtomicReference<>();
    private OkHttpClient mClient;
    private HashMap<String, Call> downCalls; // 用来存放各个下载的请求
    private List<String> downWait;           // 用来存放等待下载的请求
    private int maxCount = 2;                // 同时下载的最大个数

    public static DownloadLimitManager getInstance() {
        for (; ; ) {
            DownloadLimitManager current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new DownloadLimitManager();
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    private DownloadLimitManager() {
        downCalls = new HashMap<>();
        mClient = new OkHttpClient.Builder().build();
        downWait = new ArrayList<>();
    }

    /**
     * 查看是否在下载任务中
     * @param url
     * @return
     */
    public boolean getDownloadUrl(String url){
        return downCalls.containsKey(url);
    }

    /**
     * 查看是否在等待任务中
     * @param url
     * @return
     */
    public boolean getWaitUrl(String url){
        for (String item : downWait){
            if (item.equals(url)){
                return true;
            }
        }
        return false;
    }

    /**
     * 开始下载
     *
     * @param url              下载请求的网址
     */
    public void download(String url) {

        Observable.just(url)
                .filter(new Predicate<String>() { // 过滤 call的map中已经有了,就证明正在下载,则这次不下载
                    @Override
                    public boolean test(String s) {
                        boolean flag = downCalls.containsKey(s);
                        if (flag){
                            // 如果已经在下载，查找下一个文件进行下载
                            downNext();
                            return false;
                        }else{
                            // 判断如果正在下载的个数达到最大限制，存到等下下载列表中
                            if (downCalls.size() >= maxCount){
                                if (!getWaitUrl(s)){
                                    downWait.add(s);
                                    DownloadInfo info = new DownloadInfo(s , DownloadInfo.DOWNLOAD_WAIT);
                                    EventBus.getDefault().post(info);
                                }
                                return false;
                            }else{
                                return true;
                            }
                        }
                    }
                })
                .flatMap(new Function<String, ObservableSource<?>>() { // 生成 DownloadInfo
                    @Override
                    public ObservableSource<?> apply(String s) {
                        return Observable.just(createDownInfo(s));
                    }
                })
                .map(new Function<Object, DownloadInfo>() { // 如果已经下载，重新命名
                    @Override
                    public DownloadInfo apply(Object o) {
                        return getRealFileName((DownloadInfo)o);
                    }
                })
                .flatMap(new Function<DownloadInfo, ObservableSource<DownloadInfo>>() { // 下载
                    @Override
                    public ObservableSource<DownloadInfo> apply(DownloadInfo downloadInfo) {
                        return Observable.create(new DownloadSubscribe(downloadInfo));
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) // 在主线程中回调
                .subscribeOn(Schedulers.io()) //  在子线程中执行
                .subscribe(new DownloadLimitObserver()); //  添加观察者，监听下载进度

    }

    /**
     * 下载等待下载中的第一条
     */
    public void downNext(){
        if (downCalls.size() < maxCount && downWait.size() > 0){
            download(downWait.get(0));
            downWait.remove(0);
        }
    }

    /**
     * 下载取消或者暂停
     * @param url
     */
    public void pauseDownload(String url) {
        Call call = downCalls.get(url);
        if (call != null) {
            call.cancel();//取消
        }
        downCalls.remove(url);
        downNext();
    }

    /**
     * 取消下载 删除本地文件
     * @param info
     */
    public void cancelDownload(DownloadInfo info){
        pauseDownload(info.getUrl());
        info.setProgress(0);
        info.setDownloadStatus(DownloadInfo.DOWNLOAD_CANCEL);
        EventBus.getDefault().post(info);
        Constant.deleteFile(info.getFileName());
    }

    /**
     * 创建DownInfo
     *
     * @param url 请求网址
     * @return DownInfo
     */
    private DownloadInfo createDownInfo(String url) {
        DownloadInfo downloadInfo = new DownloadInfo(url);
        long contentLength = getContentLength(url);//获得文件大小
        downloadInfo.setTotal(contentLength);
        String fileName = url.substring(url.lastIndexOf("/"));
        downloadInfo.setFileName(fileName);
        return downloadInfo;
    }

    /**
     * 如果文件已下载重新命名新文件名
     * @param downloadInfo
     * @return
     */
    private DownloadInfo getRealFileName(DownloadInfo downloadInfo) {
        String fileName = downloadInfo.getFileName();
        long downloadLength = 0, contentLength = downloadInfo.getTotal();
        File path = new File(Constant.FILE_PATH);
        if (!path.exists()) {
            path.mkdir();
        }
        File file = new File(Constant.FILE_PATH, fileName);
        if (file.exists()) {
            //找到了文件,代表已经下载过,则获取其长度
            downloadLength = file.length();
        }
        //之前下载过,需要重新来一个文件
        int i = 1;
        while (downloadLength >= contentLength) {
            int dotIndex = fileName.lastIndexOf(".");
            String fileNameOther;
            if (dotIndex == -1) {
                fileNameOther = fileName + "(" + i + ")";
            } else {
                fileNameOther = fileName.substring(0, dotIndex)
                        + "(" + i + ")" + fileName.substring(dotIndex);
            }
            File newFile = new File(Constant.FILE_PATH, fileNameOther);
            file = newFile;
            downloadLength = newFile.length();
            i++;
        }
        //设置改变过的文件名/大小
        downloadInfo.setProgress(downloadLength);
        downloadInfo.setFileName(file.getName());
        return downloadInfo;
    }

    private class DownloadSubscribe implements ObservableOnSubscribe<DownloadInfo> {
        private DownloadInfo downloadInfo;

        public DownloadSubscribe(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
        }

        @Override
        public void subscribe(ObservableEmitter<DownloadInfo> e) throws Exception {
            String url = downloadInfo.getUrl();
            long downloadLength = downloadInfo.getProgress();//已经下载好的长度
            long contentLength = downloadInfo.getTotal();//文件的总长度
            //初始进度信息
            e.onNext(downloadInfo);
            Request request = new Request.Builder()
                    //确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
                    .addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength)
                    .url(url)
                    .build();
            Call call = mClient.newCall(request);
            downCalls.put(url, call);//把这个添加到call里,方便取消

            Response response = call.execute();
            File file = new File(Constant.FILE_PATH, downloadInfo.getFileName());
            InputStream is = null;
            FileOutputStream fileOutputStream = null;
            try {
                is = response.body().byteStream();
                fileOutputStream = new FileOutputStream(file, true);
                byte[] buffer = new byte[2048];//缓冲数组2kB
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                    downloadLength += len;
                    downloadInfo.setProgress(downloadLength);
                    e.onNext(downloadInfo);
                }
                fileOutputStream.flush();
                downCalls.remove(url);
                downNext();
            } finally {
                //关闭IO流
                DownloadIO.closeAll(is, fileOutputStream);

            }
            e.onComplete();//完成
        }
    }



    /**
     * 获取下载长度
     *
     * @param downloadUrl
     * @return
     */
    private long getContentLength(String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }

}
