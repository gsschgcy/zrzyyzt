package com.zrzyyzt.runtimeviewer.Base;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.zrzyyzt.runtimeviewer.Common.ToolBarHelper;
import com.zrzyyzt.runtimeviewer.R;

/**
 * Activity基类
 * 用于控制状态栏显示内容
 * Created by gis-luq on 2018/4/10.
 */
public class BaseActivity extends AppCompatActivity {

    private ToolBarHelper mToolBarHelper ;
    public Toolbar toolbar ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {

        mToolBarHelper = new ToolBarHelper(this,layoutResID) ;
        toolbar = mToolBarHelper.getToolBar() ;

        //设置菜单图标
//        toolbar.setNavigationIcon(R.mipmap.ic_menu);
        toolbar.setOverflowIcon(getResources().getDrawable(R.mipmap.ic_menu_white));

        //设置返回按钮图标
        toolbar.setNavigationIcon(getResources().getDrawable(R.mipmap.home));

        //设置为横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(mToolBarHelper.getContentView());
        /*把 activity_toolbar 设置到Activity 中*/
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        /*自定义的一些操作*/
        onCreateCustomToolBar(toolbar) ;
    }

    public void onCreateCustomToolBar(Toolbar toolbar){
        toolbar.setContentInsetsRelative(0,0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home){
//             //状态栏返回按钮
////            finish();
//
//            return true ;
//        }
        return super.onOptionsItemSelected(item);
    }

}
