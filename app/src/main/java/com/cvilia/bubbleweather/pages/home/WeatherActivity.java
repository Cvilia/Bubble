package com.cvilia.bubbleweather.pages.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.amap.api.location.AMapLocation;
import com.cvilia.bubbleweather.R;
import com.cvilia.bubbleweather.R2;
import com.cvilia.bubbleweather.adapter.Day7Adapter;
import com.cvilia.bubbleweather.base.BaseActivity;
import com.cvilia.bubbleweather.bean.Day7WeatherBean;
import com.cvilia.bubbleweather.bean.Day7WeatherBean.DataBean;
import com.cvilia.bubbleweather.bean.WeatherInfo;
import com.cvilia.bubbleweather.config.PageUrlConfig;
import com.cvilia.bubbleweather.utils.CopyDb2Local;
import com.scwang.smart.refresh.header.BezierRadarHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

@Route(path = PageUrlConfig.MAIN_PAGE)
public class WeatherActivity extends BaseActivity<HomePagePresenter> implements HomePageContact.View, OnRefreshListener {

    private static final String TAG = WeatherActivity.class.getSimpleName();
    private static final int REQUEST_CODE_SELECT_SITE = 0x1101;

    @BindView(R2.id.mainPageLl)
    LinearLayout mMainPageLl;//首页父布局，用来设置背景

    @BindView(R2.id.locateTv)
    TextView mCityName;

    @BindView(R2.id.tempTv)
    TextView mTempTv;

    @BindView(R2.id.weatherDescTv)
    TextView mWeatherDescTv;

    @BindView(R2.id.tempRangeTv)
    TextView mTempRangeTv;

    @BindView(R2.id.AQITv)
    TextView mAqiTv;

    @BindView(R2.id.updateTimeTv)
    TextView mUpdateTimeTv;

    @BindView(R2.id.refreshL)
    SmartRefreshLayout mRefreshLayout;

    @BindView(R2.id.refreshHeader)
    BezierRadarHeader refreshHeader;

    private static boolean isFirstIn = true;

    @BindView(R2.id.selectSiteTv)
    TextView mSelectSiteTv;//跳转选择位置

    private boolean selectCity = false;//是否是选择过城市
    private String cityCode;


    @BindView(R.id.day7RecyclerView)
    RecyclerView mDay7RecyclerView;

    @BindView(R.id.hourRecyclerView)
    RecyclerView mHourRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onViewCreated() {
        setFullScreen(false);
        super.onViewCreated();
    }

    @Override
    protected void initWidget() {
        mRefreshLayout.setPrimaryColorsId(R.color.bg_2d3093, R.color.app_main);
    }

    @Override
    protected void initWidgetEvent() {
        mRefreshLayout.setEnableLoadMore(false);
        if (isFirstIn) {
            isFirstIn = false;
            mRefreshLayout.autoRefresh();
        }
        mRefreshLayout.setOnRefreshListener(this);
        mSelectSiteTv.setOnClickListener(view -> {
            ARouter.getInstance().build(PageUrlConfig.SELECT_CITY_PAGE).navigation(this,
                    REQUEST_CODE_SELECT_SITE);
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mDay7RecyclerView.setLayoutManager(linearLayoutManager);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SITE && resultCode == RESULT_OK) {
            assert data != null;
            cityCode = data.getStringExtra("cityCode");
            mCityName.setText(data.getStringExtra("cityName"));
            selectCity = true;
//            onRefresh(mRefreshLayout);
            mRefreshLayout.autoRefresh();
        }
    }

    @Override
    protected void initData() {
        CopyDb2Local.copy2localdb(this);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void getIntentData() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected HomePagePresenter getPresenter() {
        return new HomePagePresenter();
    }


    /**
     * 重新加载当日天气信息
     *
     * @param bean
     */
    private void reloadCurrentInfo(Day7WeatherBean bean) {
        DataBean todayInfo = bean.getData().get(0);
        if (todayInfo != null) {
            mTempTv.setText(String.format(getString(R.string.temperature), todayInfo.getTem()));
            mWeatherDescTv.setText(todayInfo.getWea());
            mTempRangeTv.setText(String.format(getString(R.string.temperature_min_max),
                    todayInfo.getTem1(), todayInfo.getTem2()));
            String exchange = getString(R.string.aqi, getAQI(todayInfo.getAir()));
            mAqiTv.setText(Html.fromHtml(exchange));
            mUpdateTimeTv.setText(String.format(getString(R.string.last_update_time),
                    bean.getUpdate_time().substring(11)));
        }
        reloadDay7Weather(bean);
    }

    private String getAQI(String air) {
        String aqi = "未知";
        if (!TextUtils.isEmpty(air)) {
            int intAqi = Integer.parseInt(air);
            if (intAqi <= 50) {
                aqi = "优";
            } else if (intAqi <= 100) {
                aqi = "良";
            } else if (intAqi <= 150) {
                aqi = "轻度污染";
            } else if (intAqi <= 200) {
                aqi = "中度污染";
            } else if (intAqi <= 300) {
                aqi = "重度污染";
            } else {
                aqi = "严重污染";
            }
        }
        return aqi;
    }

    /**
     * 加载7日天气信息
     *
     * @param bean
     */
    private void reloadDay7Weather(Day7WeatherBean bean) {
        List<WeatherInfo> infos = new ArrayList<>();
        if (bean.getData().size() > 0) {
            List<DataBean> datas = bean.getData();
            int index = 0;//用来记录抛出异常的数据
            try {
                //todo 针对不为空的温度应该做类型转换异常判断（抛出并捕获异常，还需要针对温度字符串做正则表达式确认其只含有数字）
                for (int i = 0; i < bean.getData().size(); i++) {
                    index = i;

                    String tem1 = datas.get(i).getTem1().substring(0,
                            datas.get(i).getTem1().length() - 1);
                    String tem2 = datas.get(i).getTem2().substring(0,
                            datas.get(i).getTem2().length() - 1);
                    int max = TextUtils.isEmpty(datas.get(i).getTem1()) ? 10 :
                            Integer.parseInt(tem1);
                    int min = TextUtils.isEmpty(datas.get(i).getTem2()) ? 10 :
                            Integer.parseInt(tem2);
                    WeatherInfo info = new WeatherInfo(max, min);
                    infos.add(info);
                }
            } catch (Exception e) {
                Log.e(TAG, "第" + index + "条数据出错");
            }
        }
        Day7Adapter day7Adapter = new Day7Adapter(bean, infos, this);
        mDay7RecyclerView.setAdapter(day7Adapter);

    }

    @Override
    public void showRequestSuccess(Day7WeatherBean bean) {
        mRefreshLayout.finishRefresh();
        if (bean != null) runOnUiThread(() -> reloadCurrentInfo(bean));
    }

    @Override
    public void showRequestFailed() {

    }

    @Override
    public void locateSuccess(AMapLocation location) {
        mPresenter.requestWeatherInfo(location.getDistrict());
        mCityName.setText(location.getDistrict());
    }

    @Override
    public void locateFailed() {
        mRefreshLayout.finishRefresh();
        mPresenter.requestWeatherInfo("北京市");
        mCityName.setText("北京市");
    }

    @Override
    public void loading() {

    }

    @Override
    public void dismissLoading() {

    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        if (!selectCity) {
            mPresenter.startLocate(this);
        }
        if (selectCity) {
            mPresenter.requestWeatherInfo(cityCode);
        }
    }
}