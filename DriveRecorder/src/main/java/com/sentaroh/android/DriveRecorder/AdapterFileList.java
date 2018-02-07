package com.sentaroh.android.DriveRecorder;

import java.util.ArrayList;

import com.sentaroh.android.Utilities.NotifyEvent;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AdapterFileList extends ArrayAdapter<FileListItem> {
	private Context c;
	private int id;
	private ArrayList<FileListItem>items;
	
	public AdapterFileList(Context context, 
			int textViewResourceId, ArrayList<FileListItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items=objects;
	};
	
	@Override
	final public int getCount() {
		return items.size();
	}

	private boolean isShowCheckBox=false;
	public void setShowCheckBox(boolean p) {isShowCheckBox=p;}
	public boolean isShowCheckBox() {return isShowCheckBox;}

	private NotifyEvent mNotifyCheckBoxEvent=null;
	public void setNotifyCheckBoxEventHandler(NotifyEvent ntfy) {mNotifyCheckBoxEvent=ntfy;}
	
	public int getItemSelectedCount() {
		int result=0;
		for(int i=0;i<items.size();i++) if (items.get(i).isChecked) result++;
		return result;
	};

	@Override
	final public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
            holder.tv_itemname= (TextView) v.findViewById(R.id.file_list_item_name);
            holder.tv_file_size= (TextView) v.findViewById(R.id.file_list_item_size);
            holder.tv_duration= (TextView) v.findViewById(R.id.file_list_item_duration);
            holder.iv_thumnail=(ImageView) v.findViewById(R.id.file_list_thumnail);
            holder.cb_sel=(CheckBox) v.findViewById(R.id.file_list_select);
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final FileListItem o = items.get(position);
    	holder.tv_itemname.setText(o.file_name);
    	if (holder.tv_duration!=null) holder.tv_duration.setText(o.duration);
    	holder.tv_file_size.setText(o.file_size);
    	holder.iv_thumnail.setImageBitmap(o.thumbnail);

    	if (isShowCheckBox) holder.cb_sel.setVisibility(CheckBox.VISIBLE);
    	else holder.cb_sel.setVisibility(CheckBox.INVISIBLE);
   		holder.cb_sel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				o.isChecked=isChecked;
				notifyDataSetChanged();
				if (mNotifyCheckBoxEvent!=null && isShowCheckBox) 
					mNotifyCheckBoxEvent.notifyToListener(true, null);
			}
		});
   		holder.cb_sel.setChecked(o.isChecked);
    	
        return v;
	};

	class ViewHolder {
		TextView tv_itemname, tv_file_size, tv_duration;
		ImageView iv_thumnail;
		CheckBox cb_sel;
	};
}

class FileListItem {
	public boolean isChecked=false;
	public boolean archive_folder=false;
	public String file_name="";
	public String duration="00:00";
	public String file_size="";
	public Bitmap thumbnail=null;
}