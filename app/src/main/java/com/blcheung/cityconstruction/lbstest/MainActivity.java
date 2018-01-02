package com.blcheung.cityconstruction.lbstest;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.blcheung.cityconstruction.lbstest.Util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public LocationClient mLocationClient;
    private FloatingActionButton fbtnStart;
    private MapView mapView;
    private BaiduMap baiduMap;
    private final String TAG = this.getClass().getSimpleName();
    //    private TextView tvPosition;
    private final int REQUEST_LBS = 1;
    private final int REQUEST_RESULT_GPS = 0;
    private List<String> permissionList;
    private double latitude;
    private double longitude;
    private boolean isOpenGps;
    private boolean isFirstLocate = true;
//    private boolean isNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全局Context
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        // 检测GPS时候打开
        if (!checkGPSIsOpen()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("检测到你的设备未打开GPS,可能导致定位失败或定位不准确.")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, REQUEST_RESULT_GPS);
                        }
                    });
            builder.create().show();
        }

//        tvPosition = findViewById(R.id.tv_position);
        fbtnStart = findViewById(R.id.fbtn_myLoc);
        mapView = findViewById(R.id.baiduMapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // 开始定位
        fbtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permissionList = new ArrayList<>();
                Log.d(TAG, "onClick: onClick");
                // 运行时权限
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(Manifest.permission.READ_PHONE_STATE);
                }
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                if (!permissionList.isEmpty()) {
                    String[] permissions = permissionList.toArray(new String[permissionList.size()]);
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_LBS);
                } else {
                    requestLocation();
                    if (mLocationClient.isStarted() && mLocationClient != null) {
                        navigatToMe(latitude, longitude);
                    }
                }
            }
        });
    }

    /**
     * 定位到我
     * @param latitude
     * @param longitude
     */
    private void navigatToMe(double latitude, double longitude) {
            LatLng latLng = new LatLng(latitude, longitude);
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.setMapStatus(mapStatusUpdate);
            mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
            baiduMap.animateMapStatus(mapStatusUpdate);
            isFirstLocate = false;
//            isNavigated = true;
        MyLocationData locationData = new MyLocationData.Builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
        baiduMap.setMyLocationData(locationData);
    }

//    private void zoomToMe() {
//        if (isNavigated) {
//            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.zoomTo(16f);
//            baiduMap.animateMapStatus(mapStatusUpdate);
//        }
//    }

    private Boolean checkGPSIsOpen() {
        // 检测设备是否打开了GPS
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        isOpenGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isOpenGps;
    }

    /**
     * 权限检测,有一个拒绝则返回false
     *
     * @param grantResults
     * @return
     */
    private Boolean verifyPermissions(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop();
        }
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LBS:
                if (grantResults.length > 0) {
                    if (!verifyPermissions(grantResults)) {
                        AskForPermission("You need to access all permission",
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Toast.makeText(this, "拒绝权限将导致程序某些功能无法使用",
                                Toast.LENGTH_SHORT).show();
                    }
                    requestLocation();
                } else {
                    Toast.makeText(MainActivity.this, "程序发生未知错误",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    /**
     * 开始定位
     */
    private void requestLocation() {
        initLocation();
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.start();
        } else {
            mLocationClient.start();
        }
    }

    /**
     * 实时更新位置 & 相关定位设置
     */
    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setScanSpan(5000);
        option.setLocationNotify(false);
        mLocationClient.setLocOption(option);
    }


    /**
     * 询问权限方法
     */
    private void AskForPermission(String content, final String action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setTitle(title)
                .setMessage(content)
                .setNegativeButton("CANCLE", null)
                .setPositiveButton("SETTING", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(action);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                });
        builder.create().show();
    }

    class MyLocationListener extends BDAbstractLocationListener {

        /**
         * 当获取到位置信息就会回调这个监听器
         */
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation || bdLocation.getLocType() ==
                    BDLocation.TypeNetWorkLocation
                    ) {
                latitude = bdLocation.getLatitude();
                longitude = bdLocation.getLongitude();
                StringBuilder currentPosition = new StringBuilder();
                currentPosition.append("纬度: ").append(latitude).append("\n");
                currentPosition.append("经线: ").append(longitude).append("\n");
                currentPosition.append("国家: ").append(bdLocation.getCountry()).append("\n");
                currentPosition.append("省: ").append(bdLocation.getProvince()).append("\n");
                currentPosition.append("市: ").append(bdLocation.getCity()).append("\n");
                currentPosition.append("区: ").append(bdLocation.getDistrict()).append("\n");
                currentPosition.append("街道: ").append(bdLocation.getStreet()).append("\n");
                currentPosition.append("返回码: ").append(bdLocation.getLocType()).append("\n");
                currentPosition.append("定位方式: ");
                if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                    currentPosition.append("GPS");
                } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                    currentPosition.append("网络");
                }
//            tvPosition.setText(currentPosition);
                ToastUtil.showToast(MainActivity.this, currentPosition);
//                zoomToMe();
            }
        }
    }
}