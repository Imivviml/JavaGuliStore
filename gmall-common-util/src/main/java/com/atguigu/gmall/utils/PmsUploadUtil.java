/**
 * FileName: PmsUploadUtil
 * Author:   #include
 * Date:     2019/12/15 23:25
 * Description:
 */
package com.atguigu.gmall.utils;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 */
public class PmsUploadUtil {

    public static String uploadImage(MultipartFile multipartFile){

        String url = "http://192.168.112.135";

        //配置fdfs的全局链接地址
        String tracker = PmsUploadUtil.class.getResource("/tracker.conf").getPath();

        try {
            ClientGlobal.init(tracker);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();

        //获得一个trackerServer实例
        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //通过tracker获得一个Storage链接客服端
        StorageClient storageClient = new StorageClient(trackerServer, null);


        try {
            //获得文件的二进制流
            byte[] bytes = multipartFile.getBytes();

            //获得文件的后缀名
            String filename = multipartFile.getOriginalFilename();//获得文件的全名称  a.jpg

            int i = filename.lastIndexOf(".");//获取最后的一个 . 的位置

            String extName = filename.substring(i + 1);//截取最后一个点以后的信息, 获取后缀名

            String[] uploadInfos = storageClient.upload_file(bytes, extName, null);

            for (String uploadInfo : uploadInfos) {
                url += "/" + uploadInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        return url;

    }
}
