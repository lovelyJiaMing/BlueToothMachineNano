package com.example.administrator.bluetoothmachine;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {

    public class DeviceAllInfo {
        int connectstatus = 0;
        BluetoothDevice device;
    }
    //
    TextView m_tvStart;
    TextView m_tvDiscover;
    TextView m_tvConnect;
    TextView m_tvDisCon;
    TextView m_tvBound;
    //
    TextView m_tvDownloadcall;
    TextView m_tvdeletecall;
    TextView m_tvclearcall;
    TextView m_tvchangeadmcall;
    //
    TextView m_tvTitle;
    ListView m_devlistview;
    TextView m_tvSign;
    //
    BluetoothAdapter mBluetoothAdapter;
    //
    private List<DeviceAllInfo> m_listDevice = new ArrayList<>();
    DevInfoAdapter adapter;
    //选中索引
    int m_nSelectedIndex = -1;
    BluetoothSocket mSocket = null;
    //
    ReceiveDataThread rcvThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_tvStart = (TextView) findViewById(R.id.start);
        m_tvDiscover = (TextView) findViewById(R.id.discover);
        m_tvConnect = (TextView) findViewById(R.id.connectdev);
        m_tvDisCon = (TextView) findViewById(R.id.disconnectdev);
        m_tvDownloadcall = (TextView) findViewById(R.id.downloadfilecall);
        m_tvTitle = (TextView) findViewById(R.id.title);
        m_devlistview = (ListView) findViewById(R.id.listviewdev);
        m_tvSign = (TextView) findViewById(R.id.sign);
        m_tvSign.setVisibility(View.INVISIBLE);
        m_tvBound = (TextView) findViewById(R.id.bounddev);
        m_tvdeletecall = (TextView) findViewById(R.id.deletefilecall);
        m_tvclearcall = (TextView) findViewById(R.id.clearmemcall);
        m_tvchangeadmcall = (TextView) findViewById(R.id.changeadmin);

        m_tvStart.setTextColor(Color.WHITE);
        m_tvDiscover.setTextColor(Color.WHITE);
        m_tvConnect.setTextColor(Color.WHITE);
        m_tvDisCon.setTextColor(Color.WHITE);
        m_tvDownloadcall.setTextColor(Color.WHITE);
        m_tvdeletecall.setTextColor(Color.WHITE);
        m_tvclearcall.setTextColor(Color.WHITE);
        m_tvchangeadmcall.setTextColor(Color.WHITE);
        m_tvBound.setTextColor(Color.WHITE);
        m_tvTitle.setBackgroundColor(0x50000000);

        //获取蓝牙状态以便于显示
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //
        adapter = new DevInfoAdapter(this);
        m_devlistview.setAdapter(adapter);
        //重要广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        //
        if (!mBluetoothAdapter.isEnabled()) {
            m_tvStart.setText("启动蓝牙");
        } else {
            m_tvStart.setText("关闭蓝牙");
        }
        //
        setListener();
    }

    private void setListener() {
        m_tvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                    Toast.makeText(MainActivity.this, "蓝牙已经开启", Toast.LENGTH_SHORT).show();
                    m_tvStart.setText("关闭蓝牙");
                } else {
                    mBluetoothAdapter.disable();
                    Toast.makeText(MainActivity.this, "蓝牙已经关闭", Toast.LENGTH_SHORT).show();
                    m_tvStart.setText("启动蓝牙");
                    //清空已有设备避免重复出现
                    m_listDevice.clear();
                    adapter.setData(m_listDevice);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        m_tvDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "请先开启蓝牙！", Toast.LENGTH_SHORT).show();
                    return;
                }
                m_listDevice.clear();
                mBluetoothAdapter.startDiscovery();
                m_tvSign.setVisibility(View.VISIBLE);
            }
        });

        m_tvConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_nSelectedIndex == -1 || m_listDevice.get(m_nSelectedIndex).device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(MainActivity.this, "请先开启蓝牙或者先从列表中选择一项进行绑定！", Toast.LENGTH_SHORT).show();
                    return;
                }

                m_tvTitle.setText("设备连接中");
                //这里需要开启线程来连接，否则阻塞
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connect();
                    }
                }).start();
            }
        });

        m_tvDisCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        m_tvBound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_nSelectedIndex == -1) {
                    Toast.makeText(MainActivity.this, "请先开启蓝牙或者先从列表中选择一项！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //设备配对！
                Method createBondMethod = null;
                try {
                    createBondMethod = BluetoothDevice.class.getMethod("createBond");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                try {
                    createBondMethod.invoke(m_listDevice.get(m_nSelectedIndex).device);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });

        //单项选中listview
        m_devlistview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                m_nSelectedIndex = position;
                Toast.makeText(MainActivity.this, "已选中设备：" + m_listDevice.get(position).device.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        m_tvDownloadcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_nSelectedIndex == -1 || mSocket == null) {
                    Toast.makeText(MainActivity.this, "请选择一个设备连接！", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("确认")
                        .setMessage("确定下载文件吗？")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                downloadFile();
                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        m_tvdeletecall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_nSelectedIndex == -1 || mSocket == null) {
                    Toast.makeText(MainActivity.this, "请选择一个设备连接！", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("确认")
                        .setMessage("确定删除文件吗？")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteDev();
                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        m_tvclearcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_nSelectedIndex == -1 || mSocket == null) {
                    Toast.makeText(MainActivity.this, "请选择一个设备连接！", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("确认")
                        .setMessage("确定清空用户吗？")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clearAllUser();
                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        m_tvchangeadmcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_nSelectedIndex == -1 || mSocket == null) {
                    Toast.makeText(MainActivity.this, "请选择一个设备连接！", Toast.LENGTH_SHORT).show();
                    return;
                }
                rechageadmin();
            }
        });
    }

    private void rechageadmin() {
        LayoutInflater factory = LayoutInflater.from(this);
        View textEntryView = factory.inflate(R.layout.rechangeaccount, null);
        final EditText mname_edit = (EditText) textEntryView
                .findViewById(R.id.rename_edit);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("修改账户")
                .setView(textEntryView)
                .setNegativeButton("取消",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        })
                .setPositiveButton("确定",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if (!mname_edit.getText().toString().equals("")) {
                                    String sNew = mname_edit.getText().toString();
                                    //
                                    String msg = m_listDevice.get(m_nSelectedIndex).device.getName() + " change\r\n" + sNew + "\r\n";
                                    executeNetCall(msg);
                                }
                            }
                        }).show();
    }

    //清空用户存储空间
    private void clearAllUser() {
        String msg = m_listDevice.get(m_nSelectedIndex).device.getName() + " flag\r\n";
        executeNetCall(msg);
    }

    //删除某个
    private void deleteDev() {
        String msg = m_listDevice.get(m_nSelectedIndex).device.getName() + " del\r\n";
        executeNetCall(msg);
    }

    //下载文件
    private void downloadFile() {
        m_tvTitle.setText("文件下载中");
        String msg = m_listDevice.get(m_nSelectedIndex).device.getName() + " file\r\n";
        executeNetCall(msg);
    }

    private void executeNetCall(String sMsg) {
        try {
            OutputStream mmOutStream = mSocket.getOutputStream();
            mmOutStream.write(sMsg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                m_tvSign.setVisibility(View.INVISIBLE);
                //构造设备信息
                DeviceAllInfo infot = new DeviceAllInfo();
                infot.connectstatus = 0;
                infot.device = device;
                m_listDevice.add(infot);
                adapter.setData(m_listDevice);
                adapter.notifyDataSetChanged();
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                Log.i("BOND_STATE_CHANGED", device.getBondState() + "");
                adapter.notifyDataSetChanged();
            }
        }
    };

    //连接方法
    private void connect() {
        final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        UUID uuid = UUID.fromString(SPP_UUID);
        try {
            mSocket = m_listDevice.get(m_nSelectedIndex).device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message msg = new Message();
        msg.what = 30;
        mHandler.sendMessage(msg);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    public void downloadFileDone() {
        m_tvTitle.setText("电源控制列表控制台(必须选中设备列表一项)");
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 30:
                    //连接成功后开启接收线程
                    rcvThread = new ReceiveDataThread(MainActivity.this, mSocket, m_listDevice.get(m_nSelectedIndex).device.getName());
                    rcvThread.start();
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    downloadFileDone();
                    adapter.setConnect(m_nSelectedIndex);
                    break;
            }
        }
    };
}
