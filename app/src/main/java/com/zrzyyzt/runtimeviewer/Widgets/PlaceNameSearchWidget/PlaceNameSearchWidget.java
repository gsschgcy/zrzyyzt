package com.zrzyyzt.runtimeviewer.Widgets.PlaceNameSearchWidget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.zrzyyzt.runtimeviewer.BMOD.MapModule.BaseWidget.BaseWidget;
import com.zrzyyzt.runtimeviewer.GloabApp.MPApplication;
import com.zrzyyzt.runtimeviewer.R;
import com.zrzyyzt.runtimeviewer.Widgets.PlaceNameSearchWidget.Adapter.PoiResultAdapter;
import com.zrzyyzt.runtimeviewer.Widgets.PlaceNameSearchWidget.Entity.PoiBean;
import com.zrzyyzt.runtimeviewer.Widgets.PlaceNameSearchWidget.Thread.GetPoiThread;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlaceNameSearchWidget extends BaseWidget {
    private static final String TAG = "PlaceNameSearchWidget";
    private Context context;


    private  PoiBean poiBean=null;
    public View mWidgetView = null;//

    private MapView mMapView;
    private LocatorTask mLocatorTask;
    private GraphicsOverlay mGraphicsOverlay;
    private GeocodeParameters mAddressGeocodeParameters;
    private PictureMarkerSymbol mPinSourceSymbol;
    private Callout mCallout;
    //public LocationClient mLocationClient = null;
    //private MyLocationListener myListener = new MyLocationListener();
    /**
     * 组件面板打开时，执行的操作
     * 当点击widget按钮是, WidgetManager将会调用这个方法，面板打开后的代码逻辑.
     * 面板关闭将会调用 "inactive" 方法
     */
    @Override
    public void active() {

        super.active();//默认需要调用，以保证切换到其他widget时，本widget可以正确执行inactive()方法并关闭
        super.showWidget(mWidgetView);//加载UI并显示

        super.showMessageBox(super.name);//显示组件名称
    }

    /**
     * widget组件的初始化操作，包括设置view内容，逻辑等
     * 该方法在应用程序加载完成后执行
     */
    @Override
    public void create() {

        context=super.context;
        mMapView=super.mapView;

        LayoutInflater mLayoutInflater = LayoutInflater.from(super.context);
        //设置widget组件显示内容
        mWidgetView = mLayoutInflater.inflate(R.layout.widget_view_placenamesearch,null);

        //final EditText queryContext=mWidgetView.findViewById(R.id.widget_view_placenamesearch_txtQueryInfo);
        //final Button queryBtn=mWidgetView.findViewById(R.id.widget_view_placenamesearch_btnplacenamesearch);
        final ListView resultList=mWidgetView.findViewById(R.id.widget_view_placenamesearch_resultListview);
        final SearchView mAddressSearchView = mWidgetView.findViewById(R.id.widget_view_placenamesearch_addressSearchView);
        mAddressSearchView.setIconified(false);
        mAddressSearchView.setFocusable(false);

        BitmapDrawable pinDrawable = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.placenamesearch_pin);
        try {
            mPinSourceSymbol = PictureMarkerSymbol.createAsync(pinDrawable).get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "Picture Marker Symbol error: " + e.getMessage());
            Toast.makeText(MPApplication.getContext(), "没有加载定位图标！", Toast.LENGTH_LONG).show();
        }
        mPinSourceSymbol.setWidth(19f);
        mPinSourceSymbol.setHeight(72f);

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(context, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                identifyGraphic(motionEvent);
                return true;
            }
        });

        mAddressSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                poiBean=getPoi(s);
                if(poiBean!=null){
                    List<PoiBean.PoisBean> list=poiBean.getPois();
                    PoiResultAdapter poiResultAdapter=new PoiResultAdapter(context,list);
                    resultList.setAdapter(poiResultAdapter);
                }
                else {
                    Toast.makeText(MPApplication.getContext(), "查询关键字不符合查询要求！", Toast.LENGTH_LONG).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

//        queryBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String queryStr=queryContext.getText().toString();
//                if(queryStr==null||queryStr.equals(""))
//                {
//                    return;
//                }else{
//                    poiBean=getPoi(queryContext.getText().toString());
//                    if(poiBean!=null){
//                        List<PoiBean.PoisBean> list=poiBean.getPois();
//                        PoiResultAdapter poiResultAdapter=new PoiResultAdapter(context,list);
//                        resultList.setAdapter(poiResultAdapter);
//                    }
//                    else {
//                        Toast.makeText(MPApplication.getContext(), "查询关键字不符合查询要求！", Toast.LENGTH_LONG).show();
//                    }
//                }
//            }
//        });
        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Object obj=resultList.getItemAtPosition(position);
                if(obj!=null){
                    PoiBean.PoisBean poisBean=(PoiBean.PoisBean)obj;
                    String lonlat=poisBean.getLonlat();
                    String [] strLonlat=lonlat.trim().split(" ");
                    if(strLonlat.length<1)return;
                    Map<String,Object>map=attributeMap(poisBean);
                    Double lon=Double.valueOf(strLonlat[0]);
                    Double lat=Double.valueOf(strLonlat[1]);

                    Point addressPoint=new Point(lon,lat);
                    displaySearchResult(addressPoint,map);
                }
            }
        });

    }

    private void identifyGraphic(MotionEvent motionEvent) {
        // get the screen point
        android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()),
                Math.round(motionEvent.getY()));
        // from the graphics overlay, get graphics near the tapped location
        final ListenableFuture<IdentifyGraphicsOverlayResult> identifyResultsFuture = mMapView
                .identifyGraphicsOverlayAsync(mGraphicsOverlay, screenPoint, 10, false);
        identifyResultsFuture.addDoneListener(new Runnable() {
            @Override public void run() {
                try {
                    IdentifyGraphicsOverlayResult identifyGraphicsOverlayResult = identifyResultsFuture.get();
                    List<Graphic> graphics = identifyGraphicsOverlayResult.getGraphics();
                    // if a graphic has been identified
                    if (graphics.size() > 0) {
                        //get the first graphic identified
                        Graphic identifiedGraphic = graphics.get(0);
                        showCallout(identifiedGraphic);
                    } else {
                        // if no graphic identified
                        mCallout.dismiss();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Identify error: " + e.getMessage());
                }
            }
        });
    }


    private PoiBean getPoi(String keyWord){

            String address="http://api.tianditu.gov.cn/search?";
            String key="1a2360bdad0e682aba76bfb388fcb849";
            String postStr="postStr={\"keyWord\":\""+keyWord+"\",\"level\":\"14\",\"mapBound\":\"91.63,32.351,109.66,42.97\",\"queryType\":\"1\",\"count\":\"20\",\"start\":\"0\"}" ;
            String keyType="&type=query&tk="+key;
            String url=address+postStr+keyType;

            PoiBean temp = null;
            ExecutorService pool = Executors.newCachedThreadPool();
            GetPoiThread getPoiThread = new GetPoiThread(temp,url);
            Future<PoiBean> future  = pool.submit(getPoiThread);
            while(true){
                if(future.isDone()){
                    try{
                        temp= future.get();
                    }catch (Exception e){
                        Log.e(TAG, "PlaceNameSearchWidget: " + e.getMessage());
                    }
                    pool.shutdown();
                    break;
                }
            }
            return temp;
    }

    private Map<String,Object> attributeMap(PoiBean.PoisBean poisBean) {

        Map<String,Object>map=new HashMap<String,Object>();
        map.put("name",poisBean.getName());
        map.put("address",poisBean.getAddress());
        map.put("phone",poisBean.getPhone());
        return map;
    }

    private void displaySearchResult(Point point,Map<String,Object> map) {
        // dismiss any callout
        if (mMapView.getCallout() != null && mMapView.getCallout().isShowing()) {
            mMapView.getCallout().dismiss();
        }
        // clear map of existing graphics
        mMapView.getGraphicsOverlays().clear();
        //mGraphicsOverlay.getGraphics().clear();
        // create graphic object for resulting location
        mGraphicsOverlay=new GraphicsOverlay();

        Point resultPoint = point;
        Graphic resultLocGraphic = new Graphic(resultPoint,map,mPinSourceSymbol);
        // add graphic to location layer
        mGraphicsOverlay.getGraphics().add(resultLocGraphic);
        // zoom map to result over 3 seconds
        mMapView.setViewpointAsync(new Viewpoint(resultPoint.getExtent()), 3);
        // set the graphics overlay to the map
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
        showCallout(resultLocGraphic);
    }

    private void showCallout(final Graphic graphic) {
        // create a TextView for the Callout
        TextView calloutContent = new TextView(MPApplication.getContext());
        calloutContent.setTextColor(Color.BLACK);
        // set the text of the Callout to graphic's attributes
        calloutContent.setText("名称："+graphic.getAttributes().get("name").toString() + "\n"
                + "地址："+graphic.getAttributes().get("address").toString()+"\n"+"电话："+graphic.getAttributes().get("phone"));
        // get Callout
        mCallout = mMapView.getCallout();
        // set Callout options: animateCallout: true, recenterMap: false, animateRecenter: false
        mCallout.setShowOptions(new Callout.ShowOptions(true, false, false));
        mCallout.setContent(calloutContent);
        // set the leader position and show the callout
        // set the leader position and show the callout
        Point calloutLocation = graphic.computeCalloutLocation(graphic.getGeometry().getExtent().getCenter(), mMapView);
        mCallout.setGeoElement(graphic, calloutLocation);
        mCallout.show();
    }

    /**
     * 组件面板关闭时，执行的操作
     * 面板关闭将会调用 "inactive" 方法
     */
    @Override
    public void inactive(){
        super.inactive();
        super.hideCenterView();
        mCallout.dismiss();
        mMapView.getGraphicsOverlays().clear();
        mMapView.refreshDrawableState();
    }
}
