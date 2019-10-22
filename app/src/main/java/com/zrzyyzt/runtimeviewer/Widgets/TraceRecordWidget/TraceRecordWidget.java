package com.zrzyyzt.runtimeviewer.Widgets.TraceRecordWidget;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.AngularUnit;
import com.esri.arcgisruntime.geometry.AngularUnitId;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeodeticDistanceResult;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.zrzyyzt.runtimeviewer.BMOD.MapModule.BaseWidget.BaseWidget;
import com.zrzyyzt.runtimeviewer.Permission.PermissionsUtils;
import com.zrzyyzt.runtimeviewer.R;

public class TraceRecordWidget extends BaseWidget {

    private static final String TAG = "TraceRecordWidget";
    public View traceRecordView = null;//
    private Context context;
    private GraphicsOverlay graphicsLayer = null;
    private PointCollection pointCollection = null;
    private Point lastPoint = null;
    private LocationDisplay locationDisplay;

    @Override
    public void create() {
        this.context = super.context;
        LayoutInflater mLayoutInflater = LayoutInflater.from(super.context);
        traceRecordView = mLayoutInflater.inflate(R.layout.widget_view_trace_record,null);


        View btnStart =  traceRecordView.findViewById(R.id.widget_view_trace_record_start);
        View btnPause =  traceRecordView.findViewById(R.id.widget_view_trace_record_pause);
        View btnStop =  traceRecordView.findViewById(R.id.widget_view_trace_record_stop);
        ListView listViewTraceRecord =  traceRecordView.findViewById(R.id.widget_view_trace_record_list);

        btnStart.setOnClickListener(clickListener);
        btnPause.setOnClickListener(clickListener);
        btnStop.setOnClickListener(clickListener);

        PermissionsUtils.PermissionsChecker(context);

        graphicsLayer = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicsLayer);


        locationDisplay = mapView.getLocationDisplay();
        locationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
            @Override
            public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                LocationDataSource.Location location = locationChangedEvent.getLocation();
                Point position = location.getPosition();
                if(pointCollection==null){
                    pointCollection = new PointCollection(position.getSpatialReference());
                }
                if(lastPoint == null){
                    lastPoint = position;
                }else{
                    GeodeticDistanceResult geodeticDistanceResult = GeometryEngine.distanceGeodetic(position, lastPoint,
                            new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);
                    double distance = geodeticDistanceResult.getDistance();

                    String msg ="x:" + position.getX()+", y:" + position.getY() + ", wkid:" + position.getSpatialReference().getWkid()
                            + ", size:" + pointCollection.size()
//                            + ", graphics size:" + graphicsLayer.getGraphics().size()
                            + ", distance:" + distance;
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    if(distance < 5)
                        return;
                }

                pointCollection.add(position);
                lastPoint = position;

                SimpleMarkerSymbol ptSym = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 10);
                Graphic graphic = new Graphic(position, ptSym);

                graphicsLayer.getGraphics().add(graphic);
            }
        });



    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.widget_view_trace_record_start:
                    Log.d(TAG, "onClick: strat");
                    locationDisplay.startAsync();
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                    break;
                case R.id.widget_view_trace_record_pause:
                    Log.d(TAG, "onClick: pause");
                    if(locationDisplay.isStarted()){
                        locationDisplay.stop();
                    }
                    break;
                case R.id.widget_view_trace_record_stop:
                    Log.d(TAG, "onClick: stop");
                    lastPoint = null;
                    pointCollection = null;
                    graphicsLayer.getGraphics().clear();
                    break;
            }
        }
    } ;

    @Override
    public void active() {
        super.active();
        super.showWidget(traceRecordView);
    }

    @Override
    public void inactive() {
        super.inactive();
    }
}
