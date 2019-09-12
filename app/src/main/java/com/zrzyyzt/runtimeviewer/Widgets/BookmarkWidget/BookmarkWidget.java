package com.zrzyyzt.runtimeviewer.Widgets.BookmarkWidget;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.Bookmark;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.zrzyyzt.runtimeviewer.BMOD.MapModule.BaseWidget.BaseWidget;
import com.zrzyyzt.runtimeviewer.R;
import com.zrzyyzt.runtimeviewer.Utils.FileUtils;
import com.zrzyyzt.runtimeviewer.Widgets.BookmarkWidget.Adapter.BookmarkListviewAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gisluq.lib.Util.ToastUtils;


public class BookmarkWidget extends BaseWidget {

    private static final String TAG = "BookmarkWidget";
    public View bookmarkView;

    public ListView bookmarkListView = null;
    public List<Bookmark> bookmarkList = null;

    private Context context;
    private BookmarkListviewAdapter bookmarkviewAdapter = null;


    @Override
    public void active() {
        super.active();
        super.showWidget(bookmarkView);
    }

    @Override
    public void inactive() {
        super.inactive();
    }

    @Override
    public void create() {
        context = super.context;
        LayoutInflater mLayoutInflater = LayoutInflater.from(super.context);
        bookmarkView = mLayoutInflater.inflate(R.layout.widget_view_bookmark,null);

        JSONArray jsonArray = loadBookmarkListConfig();

        try{
            bookmarkList = getBookmarkList(jsonArray);
        }catch (JSONException ex){
            ex.printStackTrace();
        }
        initView();
    }

    /*
     *Json2Bookmark
     */
    private List<Bookmark> getBookmarkList(JSONArray jsonArray) throws JSONException {
        List<Bookmark> result = null;
        int num = jsonArray.length();
        if(num>0) {
            result = new ArrayList<>();
            for (int i = 0; i < num; i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String title = "";
                double x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                int wkid=0;

                //name
                try {
                    title = obj.getString("name");
                } catch (Exception e) {

                }

                //extent
                try{
                    JSONObject extent = obj.getJSONObject("extent");
                    try {
                        x1 = extent.getDouble("xmin");
                    } catch (Exception e) {

                    }
                    try {
                        y1 = extent.getDouble("ymin");
                    } catch (Exception e) {

                    }

                    try {
                        x2 = extent.getDouble("xmax");
                    } catch (Exception e) {

                    }
                    try {
                        y2 = extent.getDouble("ymax");
                    } catch (Exception e) {

                    }
                    try{
                        JSONObject sr = extent.getJSONObject("spatialReference");
                        try {
                            wkid = sr.getInt("wkid");
                        }catch (Exception e){

                        }
                    }catch (Exception ex){

                    }

                }catch (Exception e){

                }
                Log.d(TAG, "getBookmarkList: "+ title+","+x1+","+y1+","+x2+","+y2+","+wkid);
                Envelope envelope = new Envelope(x1, y1, x2, y2, SpatialReference.create(wkid));
                Viewpoint viewpoint = new Viewpoint(envelope);
//                bookmark.setViewpoint(viewpoint);
//                bookmark.setName(tilte);
                result.add(new Bookmark(title,viewpoint));
            }
        }
        return result;
    }

    /*
     *加载bookmark的json配置文件
     */
    private JSONArray loadBookmarkListConfig() {
        String path = getJSONPath();

        String json = FileUtils.openTxt(path, "UTF-8");

        JSONArray jsonArrayBookmark = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            jsonArrayBookmark = jsonObject.getJSONArray("bookmarks");
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return jsonArrayBookmark;
    }


    private String getJSONPath() {
        String path = projectPath+ File.separator+"Json"+File.separator + "bookmarks.json";
        return path;
    }

    private void initView() {
        Log.i(TAG, "initView: " + bookmarkList);
        this.bookmarkListView = (ListView)bookmarkView.findViewById(R.id.widget_view_bookmark_bookmarkListview);
        bookmarkviewAdapter = new BookmarkListviewAdapter(context, bookmarkList, super.mapView);
        this.bookmarkListView.setAdapter(bookmarkviewAdapter);

        TextView btnAdd = (TextView) bookmarkView.findViewById(R.id.widget_view_bookmark_btn_add);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtils.showShort(context,"add");
            }
        });

        TextView btnClear = (TextView) bookmarkView.findViewById(R.id.widget_view_bookmark_btn_clear);

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtils.showShort(context,"clear");
            }
        });
    }
}
