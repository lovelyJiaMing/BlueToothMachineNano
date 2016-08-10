package com.example.administrator.bluetoothmachine;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by gaoyx on 2016/4/4.
 */
public class ReceiveDataThread extends Thread {
    private final InputStream myInStream;
    StringBuffer m_sbRcvAll;
    String sDevname;
    Context mContext;

    public ReceiveDataThread(Context context, BluetoothSocket socket, String sDevName) {
        this.sDevname = sDevName;
        this.mContext = context;

        InputStream tmpIn = null;
        m_sbRcvAll = new StringBuffer();
        //从连接的socket里获取InputStream和OutputStream
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
        }
        myInStream = tmpIn;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        // 已经连接上以后持续从通道中监听输入流的情况
        while (true) {
            try {
                //  从通道的输入流InputStream中读取数据到buffer数组中
                int nallcount = myInStream.available();
                int readCount = 0; // 已经成功读取的字节的个数
                while (readCount < nallcount) {
                    readCount += myInStream.read(buffer, readCount, nallcount - readCount);
                }
                //为了下载文件需要
                if (readCount == nallcount && readCount > 0) {
                    char[] tChars = new char[readCount];
                    for (int i = 0; i < readCount; i++) {
                        tChars[i] = (char) buffer[i];
                    }
                    m_sbRcvAll.append(tChars);
                }

                //各种命令返回
                while (true) {
                    String sFileAll = m_sbRcvAll.toString();
                    if (sFileAll.contains("single file end")) {
                        int n1 = sFileAll.indexOf("filename\r\n");//开始解析字符串，首先找到filename字符串然后得到文件名称
                        String filename = sFileAll.substring(n1 + 10, n1 + 10 + 10);
                        int n2 = sFileAll.indexOf("single file end");
                        String filecontent = sFileAll.substring(n1 + 10 + 10 + 2, n2 - 2);
                        createFileAndDir(filename, filecontent);
                        //生成一个后就删除已经用来生成的字符串,已便于多文件
                        m_sbRcvAll.delete(0, n2 + 15 + 2);
                    } else {
                        //肯定最后会生一个fileend，所以可以不用处理
                        break;
                    }
                }

                if (m_sbRcvAll.toString().contains("del end")) {
                    Message msg = new Message();
                    msg.what = 2;
                    mHandlerInfo.sendMessage(msg);
                    m_sbRcvAll.setLength(0);
                }

                if (m_sbRcvAll.toString().contains("flag end")) {
                    Message msg = new Message();
                    msg.what = 3;
                    mHandlerInfo.sendMessage(msg);
                    m_sbRcvAll.setLength(0);
                }

                if (m_sbRcvAll.toString().contains("Change ok")) {
                    Message msg = new Message();
                    msg.what = 4;
                    mHandlerInfo.sendMessage(msg);
                    m_sbRcvAll.setLength(0);
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    private void createFileAndDir(String sFilename, String sFileContent) {
        //
        String sDir = FileCreateUtil.makeDir("/acpowercontrol/" + sDevname + "/", true, mContext);
        File saveFile = new File(sDir, sFilename);
//        if (saveFile.exists()) {
//            Message msg = new Message();
//            msg.what = 23;
//            mHandlerInfo.sendMessage(msg);
//            return;
//        }
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(saveFile);
            outStream.write(sFileContent.getBytes());
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Uri data = Uri.fromFile(new File(saveFile.getAbsolutePath()));
        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, data);
        mContext.sendBroadcast(localIntent);

        Message msg = new Message();
        msg.what = 1;
        msg.obj = sDir;
        mHandlerInfo.sendMessage(msg);
    }

    //处理线程成功提示
    Handler mHandlerInfo = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://1代表存储文件成功
                    Toast.makeText(mContext, "文件已存储在：" + (String) msg.obj, Toast.LENGTH_SHORT).show();
                    ((MainActivity) mContext).downloadFileDone();
                    break;
                case 2://删除文件成功
                    Toast.makeText(mContext, "文件已成功删除", Toast.LENGTH_SHORT).show();
                    break;
                case 3://清除存储空间成功
                    Toast.makeText(mContext, "清除用户成功", Toast.LENGTH_SHORT).show();
                    break;
                case 4://更改用户账户成功
                    Toast.makeText(mContext, "更改用户账户成功", Toast.LENGTH_SHORT).show();
                    break;
                case 23://文件已经存在
                    Toast.makeText(mContext, "文件已存在", Toast.LENGTH_SHORT).show();
                    break;
                case 24://下位机没有文件
                    Toast.makeText(mContext, "下位机没有文件", Toast.LENGTH_SHORT).show();
                    ((MainActivity) mContext).downloadFileDone();
                    break;
            }
            super.handleMessage(msg);
        }
    };

}
