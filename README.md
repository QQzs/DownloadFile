# DownloadFile
Android  OkHttp 下载多个文件 断点下载
## 介绍
demo的主要逻辑是，利用okhttp 和 RxJava 在子线程中下载文件，通关观察者模式监听下载的进度，再回调到主线程中，然后利用EventBus 通知页面刷新，更新进度。
## 效果图
![download.gif](https://upload-images.jianshu.io/upload_images/3183047-18efb4a2a30a86f2.gif?imageMogr2/auto-orient/strip)
### step1 导入依赖库
```Java
// OKHttp RxJava
    implementation 'com.squareup.okhttp3:okhttp:3.6.0'
    implementation 'io.reactivex.rxjava2:rxjava:2.1.3'
    implementation 'io.reactivex.rxjava2:rxandroid:2.0.1'
    // eventbus
    implementation 'org.greenrobot:eventbus:3.0.0'
    implementation 'com.android.support:recyclerview-v7:27.1.1'
```
### step 2  定义下载bean 
```Java
/**
 * Created by zs
 * Date：2018年 09月 12日
 * Time：13:50
 * —————————————————————————————————————
 * About: 下载管理
 * —————————————————————————————————————
 */
public class DownloadInfo {

    /**
     * 下载状态
     */
    public static final String DOWNLOAD = "download";
    public static final String DOWNLOAD_PAUSE = "pause";
    public static final String DOWNLOAD_CANCEL = "cancel";
    public static final String DOWNLOAD_OVER = "over";
    public static final String DOWNLOAD_ERROR = "error";

    public static final long TOTAL_ERROR = -1;//获取进度失败

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

```
### step 3  下载管理类 也是主要的内容  （DownloadManager）
```Java
/**
 * Created by zs
 * Date：2018年 09月 12日
 * Time：13:56
 * —————————————————————————————————————
 * About: 下载管理
 * —————————————————————————————————————
 */
public class DownloadManager {
    
    private static final AtomicReference<DownloadManager> INSTANCE = new AtomicReference<>();
    private OkHttpClient mClient;
    private HashMap<String, Call> downCalls; //用来存放各个下载的请求

    public static DownloadManager getInstance() {
        for (; ; ) {
            DownloadManager current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new DownloadManager();
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    private DownloadManager() {
        downCalls = new HashMap<>();
        mClient = new OkHttpClient.Builder().build();
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
     * 开始下载
     *
     * @param url              下载请求的网址
     * @param downLoadObserver 用来回调的接口
     */
    public void download(String url, DownloadObserver downLoadObserver) {
        
        Observable.just(url)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) {
                        return !downCalls.containsKey(s);
                    }
                }) // 过滤 call的map中已经有了,就证明正在下载,则这次不下载
                .flatMap(new Function<String, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(String s) {
                        return Observable.just(createDownInfo(s));
                    }
                }) // 生成 DownloadInfo
                .map(new Function<Object, DownloadInfo>() {
                    @Override
                    public DownloadInfo apply(Object o) {
                        return getRealFileName((DownloadInfo)o);
                    }
                }) // 如果已经下载，重新命名
                .flatMap(new Function<DownloadInfo, ObservableSource<DownloadInfo>>() {
                    @Override
                    public ObservableSource<DownloadInfo> apply(DownloadInfo downloadInfo) {
                        return Observable.create(new DownloadSubscribe(downloadInfo));
                    }
                }) // 下载
                .observeOn(AndroidSchedulers.mainThread()) // 在主线程中回调
                .subscribeOn(Schedulers.io()) //  在子线程中执行
                .subscribe(downLoadObserver); //  添加观察者，监听下载进度
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
```
这里要多分析一下，下载管理类是单例模式这是必须的，里面定义一个HashMap用来存放所有的下载任务，里面的download（）方法，是主要过程
```Java
public void download(String url, DownloadObserver downLoadObserver) {
        
        Observable.just(url)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) {
                        return !downCalls.containsKey(s);
                    }
                }) // 过滤 call的map中已经有了,就证明正在下载,则这次不下载
                .flatMap(new Function<String, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(String s) {
                        return Observable.just(createDownInfo(s));
                    }
                }) // 生成 DownloadInfo
                .map(new Function<Object, DownloadInfo>() {
                    @Override
                    public DownloadInfo apply(Object o) {
                        return getRealFileName((DownloadInfo)o);
                    }
                }) // 如果已经下载，重新命名
                .flatMap(new Function<DownloadInfo, ObservableSource<DownloadInfo>>() {
                    @Override
                    public ObservableSource<DownloadInfo> apply(DownloadInfo downloadInfo) {
                        return Observable.create(new DownloadSubscribe(downloadInfo));
                    }
                }) // 下载
                .observeOn(AndroidSchedulers.mainThread()) // 在主线程中回调
                .subscribeOn(Schedulers.io()) //  在子线程中执行
                .subscribe(downLoadObserver); //  添加观察者，监听下载进度
    }
```
这里用到的是Rxjava，第一步filter，过滤下载，已经下载的url再点击下载是不会另起下载任务的；第二步flatMap，通过url生成Bean类，这个按需求来设计，也可以直接传一个Bean进来也是可以的；第三步map，如果这个文件已经下载了，再次下载重新命名文件，这也是根据需求改变，如果下载过的文件不需要下载，这就可以省了；第四步flatMap，去下载具体去看下载方法；剩下的就是切换线程和添加观察者了。
### step4 观察者
```Java
/**
 * Created by zs
 * Date：2018年 09月 12日
 * Time：13:50
 * —————————————————————————————————————
 * About: 观察者
 * —————————————————————————————————————
 */
public class DownloadObserver implements Observer<DownloadInfo> {

    public Disposable d;//可以用于取消注册的监听者
    public DownloadInfo downloadInfo;

    @Override
    public void onSubscribe(Disposable d) {
        this.d = d;
    }

    @Override
    public void onNext(DownloadInfo value) {
        this.downloadInfo = value;
        downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD);
        EventBus.getDefault().post(downloadInfo);
    }

    @Override
    public void onError(Throwable e) {
        Log.d("My_Log","onError");
        if (DownloadManager.getInstance().getDownloadUrl(downloadInfo.getUrl())){
            DownloadManager.getInstance().pauseDownload(downloadInfo.getUrl());
            downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD_ERROR);
            EventBus.getDefault().post(downloadInfo);
        }else{
            downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD_PAUSE);
            EventBus.getDefault().post(downloadInfo);
        }

    }

    @Override
    public void onComplete() {
        Log.d("My_Log","onComplete");
        if (downloadInfo != null){
            downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD_OVER);
            EventBus.getDefault().post(downloadInfo);
        }
    }
}

```
这里是用到EventBus来通知页面刷新的，当然也可以不用，把订阅者直接写在Activity，但是那样不利于代码的复用，如果多个页面需要进度更新就麻烦了。
### step5 Activity 和 Adapter
页面中是一个RecyclerView，交互逻辑不是很多，说一点，Adapter条目的更新，用的notifyItemChanged(i) 每次更新某一条的进度，而不是notifyDataSetChanged()全部刷新，因为调用全部刷新，刷新的频率很高会导致条目中控件的点击事件不好用没有响应，被拦截了，而更新某一条也会有一个问题是条目刷新时会闪动，解决方案是把RecyclerView的刷新动画去掉，这样就解决了。
```Java
// 取消item刷新的动画
        ((SimpleItemAnimator)recycler_view.getItemAnimator()).setSupportsChangeAnimations(false);
```
```Java
/**
 * Created by zs
 * Date：2018年 09月 11日
 * Time：18:06
 * —————————————————————————————————————
 * About:
 * —————————————————————————————————————
 */
public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.UploadHolder>  {

    private List<DownloadInfo> mdata;

    public DownloadAdapter(List<DownloadInfo> mdata) {
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
                DownloadManager.getInstance().download(info.getUrl(), new DownloadObserver());
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

```
### github地址
https://github.com/QQzs/DownloadFile

### 简书地址
https://www.jianshu.com/p/4bab2e9c577e

参考地址：
https://blog.csdn.net/cfy137000/article/details/54838608
所有内容基本是参考的这篇文章，我只是少有修改，希望给大家有所帮助。
