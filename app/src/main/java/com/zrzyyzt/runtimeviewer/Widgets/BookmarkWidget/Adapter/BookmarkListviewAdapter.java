package com.zrzyyzt.runtimeviewer.Widgets.BookmarkWidget.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.esri.arcgisruntime.mapping.Bookmark;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.zrzyyzt.runtimeviewer.R;

import java.util.List;

public class BookmarkListviewAdapter extends BaseAdapter {

    private static final String TAG = "BookmarkviewAdapter";

    public class AdapterHolderBookemark{//列表绑定项
        public View itemView;
        public Button btnMore;
        public TextView title;
//        public CheckBox cbxLayer;//图层是否选中
    }

    private Context context;
    private MapView mapView;
    private List<Bookmark> bookmarkList = null;

    public BookmarkListviewAdapter(Context context,List<Bookmark> bookmarkList, MapView mapView){
        this.context = context;
        this.mapView = mapView;
        this.bookmarkList = bookmarkList;
    }

    /**
     * 刷新数据
     */
    public void refreshData(){
        notifyDataSetChanged();//刷新数据
    }

    @Override
    public int getCount() {
        return bookmarkList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView: position " + position);
        final int index = bookmarkList.size()-position-1;
        if (index<0) return convertView;//为空

        AdapterHolderBookemark holder = new AdapterHolderBookemark();

        convertView = LayoutInflater.from(context).inflate(R.layout.widget_view_bookmark_item, null);
        holder.itemView = convertView.findViewById(R.id.widget_view_bookmark_item_view);
        holder.title = convertView.findViewById(R.id.widget_view_bookmark_item_title);
        holder.btnMore = convertView.findViewById(R.id.widget_view_bookmark_btnmore);

        String title = bookmarkList.get(index).getName();
        Log.i(TAG, "getView: title " + title);
        holder.title.setText(title);

        final Bookmark bookmark = bookmarkList.get(index);

        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = v.getId();
                if(i==R.id.widget_view_bookmark_item_title){
                    Viewpoint vp = bookmark.getViewpoint();
                    mapView.setViewpointAsync(vp);
                }
            }
        });

        return convertView;
    }
}
