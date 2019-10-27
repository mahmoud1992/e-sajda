package com.issc.isscaudiowidget;

import java.util.ArrayList;
import java.util.HashMap;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {
	
	private boolean D = false;
	private static final String TAG = "RecordTest";

	private RecorderMain recorderMain;
	private ArrayList<HashMap<String, Object>> mAppList;
	private LayoutInflater mInflater;
	private Context mContext;
	private String[] keyString;
	private int[] valueViewID;
	
	private ItemView itemView;

	private class ItemView {
		TextView ItemName;
		TextView ItemInfo;
		ImageButton ItemButton;
	}

	public MyAdapter(RecorderMain c, ArrayList<HashMap<String, Object>> appList, int resource, String[] from, int[] to) {
		mAppList = appList;
		mContext = c;
		this.recorderMain = c;
		mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		keyString = new String[from.length];
		valueViewID = new int[to.length];
		System.arraycopy(from, 0, keyString, 0, from.length);
		System.arraycopy(to, 0, valueViewID, 0, to.length);
	}
	
	public void updateList(ArrayList<HashMap<String, Object>> appList) {
		mAppList = appList;
	}

	@Override
		public int getCount() {
		// TODO Auto-generated method stub
		//return 0;
		return mAppList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		//return null;
		return mAppList.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		//return 0;
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		//return null;
		
		if (convertView != null) {
			itemView = (ItemView) convertView.getTag();
		} else {
			convertView = mInflater.inflate(R.layout.mylistview, null);
			itemView = new ItemView();
			itemView.ItemName = (TextView)convertView.findViewById(valueViewID[0]);
			itemView.ItemInfo = (TextView)convertView.findViewById(valueViewID[1]);
			itemView.ItemButton = (ImageButton)convertView.findViewById(valueViewID[2]);
			convertView.setTag(itemView);
		}
		
		HashMap<String, Object> appInfo = mAppList.get(position);
		if (appInfo != null) {
			String name = (String) appInfo.get(keyString[0]);
			String info = (String) appInfo.get(keyString[1]);
			itemView.ItemName.setText(name);
			itemView.ItemInfo.setText(info);
			itemView.ItemButton.setOnClickListener(new ItemButton_Click(this.recorderMain, position));
		}
		
		return convertView;
	}
	
	class ItemButton_Click implements OnClickListener {
		private int position;
		private RecorderMain recorderMain;
		
		ItemButton_Click(RecorderMain context, int pos) {
			position = pos;
			this.recorderMain = context;
		}
		
		@Override
		public void onClick(View v) {
			int vid = v.getId();
			if (vid == itemView.ItemButton.getId())
				if (D) Log.v(TAG,String.valueOf(position) );
			this.recorderMain.myDialog(String.valueOf(mAppList.get(position).get("ItemName")));
		}
	}
}