package com.zrzyyzt.runtimeviewer.Widgets.StatisticsWidget;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.StatisticDefinition;
import com.esri.arcgisruntime.data.StatisticRecord;
import com.esri.arcgisruntime.data.StatisticType;
import com.esri.arcgisruntime.data.StatisticsQueryParameters;
import com.esri.arcgisruntime.data.StatisticsQueryResult;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.zrzyyzt.runtimeviewer.BMOD.MapModule.BaseWidget.BaseWidget;
import com.zrzyyzt.runtimeviewer.R;
import com.zrzyyzt.runtimeviewer.Widgets.StatisticsWidget.Adapter.StFieldSpinnerAdapter;
import com.zrzyyzt.runtimeviewer.Widgets.StatisticsWidget.Adapter.StLayerSpinnerAdapter;
import com.zrzyyzt.runtimeviewer.Widgets.StatisticsWidget.Adapter.StTypeSpinnerAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;

public class StatisticsWidget extends BaseWidget {

    public View mWidgetView = null;//

    public RelativeLayout viewContent;

    private View chartView=null;

    private View tableView=null;

    /**
     * 组件面板打开时，执行的操作
     * 当点击widget按钮是, WidgetManager将会调用这个方法，面板打开后的代码逻辑.
     * 面板关闭将会调用 "inactive" 方法
     */
    @Override
    public void active() {

        super.active();//默认需要调用，以保证切换到其他widget时，本widget可以正确执行inactive()方法并关闭
        super.showWidget(mWidgetView);//加载UI并显示

        //super.showMessageBox(super.name);//显示组件名称

        //super.mapView.getMap().getBasemap().getBaseLayers();
        //super.mapView.getMap().getOperationalLayers();

        //super.showCenterView();
        //super.showCollectPointBtn();
    }

    /**
     * widget组件的初始化操作，包括设置view内容，逻辑等
     * 该方法在应用程序加载完成后执行
     */
    @Override
    public void create() {
        //LayoutInflater mLayoutInflater = LayoutInflater.from(super.context);
        //设置widget组件显示内容
        //mWidgetView = mLayoutInflater.inflate(R.layout.widget_view_statistics,null);
        this.context=context;
        initWidgetView();
    }

    /**
     * 组件面板关闭时，执行的操作
     * 面板关闭将会调用 "inactive" 方法
     */
    @Override
    public void inactive(){
        super.inactive();
        returnDefault();
        //super.hideCenterView();
        //super.hideCollectPointBtn();
    }

    private void  initWidgetView() {
        mWidgetView= LayoutInflater.from(super.context).inflate(R.layout.widget_view_statistics,null);
        viewContent=mWidgetView.findViewById(R.id.widget_view_statistics_result);
        chartView=mWidgetView.findViewById(R.id.widget_view_statistics_result_chart);
        tableView=mWidgetView.findViewById(R.id.widget_view_statistics_result_table);
        TextView txtBtnChart=mWidgetView.findViewById(R.id.widget_view_statistics_txtBtnChart);
        TextView txtBtnTable=mWidgetView.findViewById(R.id.widget_view_statistics_txtBtnTable);

        final Spinner spinnerLayerList =mWidgetView.findViewById(R.id.widget_view_statistics_spinnerLayer);
        final Spinner spinnerFieldList=mWidgetView.findViewById(R.id.widget_view_statistics_spinnerfield);
        final Spinner spinnerTypeList=mWidgetView.findViewById(R.id.widget_view_statistics_spinnertype);


        StLayerSpinnerAdapter stLayerSpinnerAdapter=new StLayerSpinnerAdapter(context,mapView.getMap().getOperationalLayers());
        spinnerLayerList.setAdapter(stLayerSpinnerAdapter);
        spinnerLayerList.setSelection(0);

        List<String>  typeList=new ArrayList<>();
        typeList.add("数量");
        typeList.add("面积");
        typeList.add("长度");
        StTypeSpinnerAdapter stTypeSpinnerAdapter=new StTypeSpinnerAdapter(context,typeList);
        spinnerTypeList.setAdapter(stTypeSpinnerAdapter);
        spinnerTypeList.setSelection(0);

        final View resultView=LayoutInflater.from(super.context).inflate(R.layout.widget_view_statistics_result,null);
        final PieChartView pieChartView=resultView.findViewById(R.id.widget_view_statistics_piechartview);

        final View listView=LayoutInflater.from(super.context).inflate(R.layout.widget_view_statistics_table,null);
        final ListView tabListView=listView.findViewById(R.id.widget_view_statistics_resultListview);

        final Button btnStatistics=mWidgetView.findViewById(R.id.widget_view_statistics_btnStatistics);

        //final PieChartView pieChartView=(PieChartView)mWidgetView.findViewById(R.id.widget_view_statistics_piechartview);
        //initPieChart(pieChartView);

        final StFieldSpinnerAdapter[] stFieldSpinnerAdapter = new StFieldSpinnerAdapter[1];

        spinnerLayerList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object obj=spinnerLayerList.getSelectedItem();
                if(obj!=null){

                    final ArcGISTiledLayer featureLayer = (ArcGISTiledLayer)obj;
                    ServiceFeatureTable featureTable =new ServiceFeatureTable(featureLayer.getUri()+"/0" );
                    setStFields(featureTable,spinnerFieldList);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(viewContent.getChildAt(0)!=resultView){
                    viewContent.removeAllViews();
                    viewContent.addView(resultView);
                }
                Object object=spinnerLayerList.getSelectedItem();
                if(object!=null){
                    final ArcGISTiledLayer featureLayer=(ArcGISTiledLayer)object;
                    final com.esri.arcgisruntime.data.Field field=(com.esri.arcgisruntime.data.Field)spinnerFieldList.getSelectedItem();
                    final String sataType=(String)spinnerTypeList.getSelectedItem();
                    //textView.setText(field.getName());
                    List<StatisticDefinition> statisticDefinitions=new ArrayList<>();

                    if(sataType=="数量"){
                        statisticDefinitions.add(new StatisticDefinition(field.getName(), StatisticType.COUNT,sataType));
                    }
                    else if(sataType=="面积"){
                        statisticDefinitions.add(new StatisticDefinition("AREA", StatisticType.SUM,sataType));
                    }
                    else {
                        statisticDefinitions.add(new StatisticDefinition("LENGTH", StatisticType.SUM,sataType));
                    }

                    StatisticsQueryParameters queryParameters=new StatisticsQueryParameters(statisticDefinitions);
                    queryParameters.getGroupByFieldNames().add(field.getName());

                    ServiceFeatureTable featureTable=new ServiceFeatureTable(featureLayer.getUri()+"/0");

                    final ListenableFuture<StatisticsQueryResult> queryResultListenableFuture=featureTable.queryStatisticsAsync(queryParameters);
                    queryResultListenableFuture.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                StatisticsQueryResult statisticsQueryResult = queryResultListenableFuture.get();
                                Iterator<StatisticRecord> statisticRecordIterator=statisticsQueryResult.iterator();
                                List<SliceValue> values=new ArrayList<>();
                                ArrayList<String> listTab=new ArrayList<>();

                                while (statisticRecordIterator.hasNext()){
                                    StatisticRecord statisticRecord=statisticRecordIterator.next();
                                    if(statisticRecord.getGroup().isEmpty()){
                                        for (Map.Entry<String,Object> stat:statisticRecord.getStatistics().entrySet()){
                                            String strValue=stat.getKey()+":"+String.format(Locale.CHINESE,"%,.0f",(Double)stat.getValue());
                                        }
                                    }else {
                                        for (Map.Entry<String, Object> group : statisticRecord.getGroup().entrySet()) {
                                            for (Map.Entry<String, Object> stat : statisticRecord.getStatistics().entrySet()) {
                                                double value= (Double) stat.getValue();
                                                values.add(new SliceValue((float)value,randomColor()).setLabel(group.getValue().toString()));
                                                listTab.add(group.getValue().toString()+":"+stat.getValue());
                                            }
                                        }
                                    }
                                }
                                initPieChart(pieChartView,values);
                                if(listTab.size()>0){
                                    ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(context,R.layout.widget_view_statistics_table_item,R.id.widget_view_statistics_table_item_txtName,listTab);
                                    tabListView.setAdapter(arrayAdapter);
                                }

                            }
                            catch (Exception e) {
                                Log.e("统计发生错误","错误原因："+e.getMessage());
                            }
                        }
                    });
                }
            }
        });
        txtBtnChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewContent.getChildAt(0)!=resultView)
                {
                    viewContent.removeAllViews();
                    viewContent.addView(resultView);
                    chartView.setVisibility(View.VISIBLE);
                    tableView.setVisibility(View.GONE);
                }
            }
        });

        txtBtnTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewContent.getChildAt(0)!=listView)
                {
                    viewContent.removeAllViews();
                    viewContent.addView(listView);
                    chartView.setVisibility(View.GONE);
                    tableView.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    private void setStFields(final ServiceFeatureTable serviceFeatureTable,final Spinner fieldList) {
        serviceFeatureTable.loadAsync();
        serviceFeatureTable.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                List<Field> list=serviceFeatureTable.getFields();
                if(list.size()<0){
                    return;
                }
                final StFieldSpinnerAdapter[] stFieldSpinnerAdapter = new StFieldSpinnerAdapter[1];
                stFieldSpinnerAdapter[0] =new StFieldSpinnerAdapter(context,list);
                fieldList.setAdapter(stFieldSpinnerAdapter[0]);
            }
        });
    }

    private int randomColor(){

        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);

        return Color.rgb(r,g,b);
    }

    private void initPieChart(PieChartView pieChartView,List<SliceValue>values){

        PieChartData pieChartData=new PieChartData(values);
        /*****************************饼中文字设置************************************/
        //是否显示文本内容(默认为false)
        pieChartData.setHasLabels(true);
        //是否点击饼模块才显示文本（默认为false,为true时，setHasLabels(true)无效）
//		pieChartData.setHasLabelsOnlyForSelected(true);
        //文本内容是否显示在饼图外侧(默认为false)
        pieChartData.setHasLabelsOutside(false);
        //文本字体大小
        pieChartData.setValueLabelTextSize(10);
        //文本文字颜色
        pieChartData.setValueLabelsTextColor(Color.WHITE);
        //设置文本背景颜色
        pieChartData.setValueLabelBackgroundColor(Color.RED);
        //设置文本背景颜色时，必须设置自动背景为false
        pieChartData.setValueLabelBackgroundAuto(false);
        //设置是否有文字背景
        pieChartData.setValueLabelBackgroundEnabled(false);

        /*****************************中心圆设置************************************/
        //饼图是空心圆环还是实心饼状（默认false,饼状）
        pieChartData.setHasCenterCircle(true);
        //中心圆的颜色（需setHasCenterCircle(true)，因为只有圆环才能看到中心圆）
        pieChartData.setCenterCircleColor(Color.WHITE);
        //中心圆所占饼图比例（0-1）
        pieChartData.setCenterCircleScale(0.2f);

        //饼图各模块的间隔(默认为0)
        pieChartData.setSlicesSpacing(3);

        pieChartView.setPieChartData(pieChartData);
        //整个饼图所占视图比例（0-1）
        pieChartView.setCircleFillRatio(1.0f);
        //饼图是否可以转动（默认为true）
        pieChartView.setChartRotationEnabled(true);

    }

    private void returnDefault(){
        viewContent.removeAllViews();
        viewContent.refreshDrawableState();
        mWidgetView.refreshDrawableState();
    }

}
