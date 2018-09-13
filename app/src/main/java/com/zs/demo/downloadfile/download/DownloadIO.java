package com.zs.demo.downloadfile.download;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by zs
 * Date：2018年 09月 12日
 * Time：14:26
 * —————————————————————————————————————
 * About:
 * —————————————————————————————————————
 */
public class DownloadIO {
    public static void closeAll(Closeable... closeables){
        if(closeables == null){
            return;
        }
        for (Closeable closeable : closeables) {
            if(closeable!=null){
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
