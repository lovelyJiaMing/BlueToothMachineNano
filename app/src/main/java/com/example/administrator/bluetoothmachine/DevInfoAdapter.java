package com.example.administrator.bluetoothmachine;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

//设备显示列表适配器
class DevInfoAdapter extends BaseAdapter {

    private List<MainActivity.DeviceAllInfo> m_listInfo = new ArrayList<>();
    private Context mContext;

    public DevInfoAdapter(Context context) {
        this.mContext = context;
    }

    public void setData(List<MainActivity.DeviceAllInfo> list) {
        m_listInfo.clear();
        m_listInfo.addAll(list);
    }

    @Override
    public int getCount() {
        return m_listInfo.size();
    }

    @Override
    public Object getItem(int position) {
        return m_listInfo.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int nIndex = position;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.devinfoitem, null);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.devname1);
        TextView tvAdd = (TextView) convertView.findViewById(R.id.devaddress1);
        TextView tvBound = (TextView) convertView.findViewById(R.id.devbound1);
        TextView tvcon = (TextView) convertView.findViewById(R.id.devconnect1);

        tvName.setText(m_listInfo.get(nIndex).device.getName());
        tvAdd.setText(m_listInfo.get(nIndex).device.getAddress());
        if (m_listInfo.get(nIndex).device.getBondState() == BluetoothDevice.BOND_NONE)
            tvBound.setText("未配对");
        else if (m_listInfo.get(nIndex).device.getBondState() == BluetoothDevice.BOND_BONDED)
            tvBound.setText("已配对");
        else if (m_listInfo.get(nIndex).device.getBondState() == BluetoothDevice.BOND_BONDING)
            tvBound.setText("配对中");
        tvcon.setText(m_listInfo.get(nIndex).connectstatus == 1 ? "已连接" : "未连接");
        //
        if (m_listInfo.get(nIndex).connectstatus == 1) {
            convertView.setBackgroundColor(0xff7fff00);
        } else
            convertView.setBackgroundColor(0x6fffffff);
        return convertView;

    }

    public void setConnect(int nIndex) {
        for (int i = 0; i < m_listInfo.size(); ++i) {
            if (i == nIndex)
                m_listInfo.get(nIndex).connectstatus = 1;
            else
                m_listInfo.get(i).connectstatus = 0;
        }
        notifyDataSetChanged();
    }
}
