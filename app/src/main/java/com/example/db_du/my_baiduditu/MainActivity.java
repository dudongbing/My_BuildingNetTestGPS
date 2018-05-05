package com.example.db_du.my_baiduditu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity
        implements LocationListener{

    static final int MIN_TIME=5000;
    static final float MIN_DIST=5;
    LocationManager mgr;
    boolean isGpsEnabled;
    boolean isNetworkEnabled;
    String gpsprovider;

    private MapView mMapView = null;
//    public LocationClient mLocationClient = null;
    private TextView positionText,buildinginfo1,currentsignal;//显示网络信息、位置信息
 //   private MyLocationListener myListener = new MyLocationListener();
    private BaiduMap baiduMap;
    private  boolean isFirstLocate=true;
    private Button upfloor,downfloor,savedata;


    private double lastlati,lastlongi,lastaccuracy;//纬度、经度、精度
    private boolean isTestMode=false;//是否测试模式
    private boolean isBuildingtest=false;//是否楼层测试
    private boolean filesaved=true;//测试文件是否保存
    private String TestLocat="",BuildingNum="",BuildingEle="";//测试地点、楼号、单元
    private int BuildingFloor=0;//楼层
    private double buildinglati,buildinglongi;
    Toast tos;
    //数据文件变量
    File SDCard;
    //定义文件名称
    File file;
    FileOutputStream outStream;

//网络测试相关变量
    SignalStrengthListener MyListener;
    TelephonyManager tm;
    String[] networktype={"UNKNOWN","GPRS","EDGE","UMTS","CDMA","EVDO_0","EVDO_A","1xRTT","HSDPA","HSUPA",
            "HSPA","IDEN","EVDO_B","LTE","EHRPD","HSPA+","NETWORK_TYPE_GSM","NETWORK_TYPE_TD_SCDMA","NETWORK_TYPE_IWLAN"};
    String operator;//运营商网络
    int netType;//网络类型
    int LTETAC,eNodebid,LTECI,RSRP,SINR;//LTE网络数据
    int GSMLAC,GSMCI,RXL,BER;//GSM网络数据
    int WCDMALAC,RNC,WCDMACI,RSCP,ECIO;//WCDMA网络数据

    String NetInfo="",LocaInfo="",signalInfo="";//当前网络信息、当前位置信息、信号信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*百度定位
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);
*/
        mgr=(LocationManager)getSystemService(LOCATION_SERVICE);//GPS服务

        //网络测试设置
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        MyListener=new SignalStrengthListener();
        tm.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        baiduMap=mMapView.getMap();

        baiduMap.setMyLocationEnabled(true);//百度定位

        positionText=(TextView)findViewById(R.id.position_text_view);
        currentsignal=(TextView)findViewById(R.id.currentsignal);
        buildinginfo1=(TextView)findViewById(R.id.buildinginfo);

        upfloor=(Button)findViewById(R.id.upfloor);
        downfloor=(Button)findViewById(R.id.downfloor) ;
        savedata=(Button)findViewById(R.id.savedata) ;
        tos=Toast.makeText(this,"",Toast.LENGTH_SHORT);

        buildinginfo1.setVisibility(View.GONE);
        upfloor.setVisibility(View.GONE);
        downfloor.setVisibility(View.GONE);
        savedata.setVisibility(View.GONE);

        //检查权限
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.
                permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.
                permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.
                permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions=permissionList.toArray(new String[permissionList.size()]);
//            Log.d("baidu",permissions.toString());
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }
 /*       else{
            requestLocation();//百度定位
        }*/
    }
//GPS回调方法
    @Override
    public void onLocationChanged(Location location) {
        gpsprovider=location.getProvider();
        lastlati=location.getLatitude();
        lastlongi=location.getLongitude();
        lastaccuracy=location.getAccuracy();


        navigateTo();



        LocaInfo=String.format("经度：%.5f   纬度：%.5f   精度:%.2f米",lastlongi,lastlati,lastaccuracy);
        positionText.setText(LocaInfo+"\n"+NetInfo);

        if(isTestMode&&(!isBuildingtest)){
//            positionText.setText(NetInfo+LocaInfo+"\n测试模式");

            String RRR="",SEB="",TACLAC="",ENRNC="",CI="",LATI="",LONGI="";

            try{
                //定义文件输出流
                outStream=new FileOutputStream(file,true);

                //获取当前时间
                SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");
                Date curDate =  new Date(System.currentTimeMillis());
                String   str = formatter.format(curDate);

                if(netType== TelephonyManager.NETWORK_TYPE_LTE){
                    RRR=RSRP+"";
                    SEB=SINR+"";
                    TACLAC=LTETAC+"";
                    ENRNC=eNodebid+"";
                    CI=LTECI+"";
                    LATI=lastlati+"";
                    LONGI=lastlongi+"";
                }
                else if(netType== TelephonyManager.NETWORK_TYPE_GPRS||netType== TelephonyManager.NETWORK_TYPE_EDGE
                        ||netType== TelephonyManager.NETWORK_TYPE_GSM) {
                    RRR=RXL+"";
                    SEB=BER+"";
                    TACLAC=GSMLAC+"";
                    ENRNC="";
                    CI=GSMCI+"";
                    LATI=lastlati+"";
                    LONGI=lastlongi+"";
                }
                else{
                    RRR=RSCP+"";
                    SEB=ECIO+"";
                    TACLAC=WCDMALAC+"";
                    ENRNC=RNC+"";
                    CI=WCDMACI+"";
                    LATI=lastlati+"";
                    LONGI=lastlongi+"";
                }

                String datatitle=str+",,,,,"+networktype[netType]+","+LONGI+","+LATI+","+RRR+","+SEB+","+TACLAC+","+ENRNC+","+CI+"\n";
                try{
                    //写入文件
                    outStream.write(datatitle.getBytes("gb2312"));
                    outStream.close();
                }catch(IOException  e){
                    e.printStackTrace();
                }
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }
        }

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
//GPS回调方法


    //网络信号强度监听及处理
    private class SignalStrengthListener extends PhoneStateListener {
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            StringBuilder currentnet=new StringBuilder();//网络信息字符串

            operator = tm.getNetworkOperator();
            netType = tm.getNetworkType();

            currentnet.append(operator+"  ").append(networktype[netType]);
            String testStr="";
            if (isTestMode) testStr="    测试模式";
            else   testStr="";

            Boolean  getCard1=false;//是否取得第一张卡的网络信息--本程序只获取第一张卡的网络信息

            try {
                //用getAllCellLocation获取基站信息
                List<CellInfo> mcellInfos = tm.getAllCellInfo();
                if (mcellInfos.size() != 0) {
                    for (CellInfo mci : mcellInfos) {
                        if (mci instanceof CellInfoWcdma) {

                            if (mci.isRegistered()&&(getCard1==false)) {//是否已注册小区,且未取得第一张卡的网络信息
                                CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma) mci).getCellSignalStrength();
                                WCDMALAC = ((CellInfoWcdma) mci).getCellIdentity().getLac();
                                RNC = ((CellInfoWcdma) mci).getCellIdentity().getCid() / 65536;
                                WCDMACI = ((CellInfoWcdma) mci).getCellIdentity().getCid() % 65536;
                                RSCP = cellSignalStrengthWcdma.getDbm();
                                ECIO = 999;
                                currentnet.append("  LAC:" + WCDMALAC).append("  RNC:" + RNC).append("  CI:" + WCDMACI);
                                signalInfo = " RSCP:" + RSCP + "        " + testStr;
                                getCard1=true;//已取得第一张卡的网络信息
                            }
                        } else if (mci instanceof CellInfoGsm) {
                            if  (mci.isRegistered()&&(getCard1==false)) {//是否已注册小区
                                CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) mci).getCellSignalStrength();
                                GSMLAC =  ((CellInfoGsm) mci).getCellIdentity().getLac();
                                GSMCI =  ((CellInfoGsm) mci).getCellIdentity().getCid();
                                RXL = cellSignalStrengthGsm.getDbm();
                                BER = 999;
                                currentnet.append("  LAC:" + GSMLAC).append("  CI:" + GSMCI);
                                signalInfo = " RXL:" + RXL + "        " + testStr;
                                getCard1=true;
                            }
                        } else {
                            if (mci instanceof CellInfoLte) {
      //                          CellInfoLte cellInfoLte = (CellInfoLte) tm.getAllCellInfo().get(0);//获得第一个小区的信息
                                if  (mci.isRegistered()&&(getCard1==false)){//是否已注册小区
                                    CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte)mci).getCellSignalStrength();
                                    LTETAC = ((CellInfoLte)mci).getCellIdentity().getTac();
                                    eNodebid = ((CellInfoLte)mci).getCellIdentity().getCi() / 256;
                                    LTECI = ((CellInfoLte)mci).getCellIdentity().getCi() % 256;
                                    RSRP = cellSignalStrengthLte.getDbm();
                                    SINR = 999;
                                    ;
                                    currentnet.append("  TAC:" + LTETAC).append("  eNodeBID:" + eNodebid).append("  CI:" + LTECI);
                                    signalInfo = " RSRP:" + RSRP + "        " + testStr;
                                    getCard1=true;
                                }

                            }
                        }
//                        signal1.setText(txvdbm);
                    }
                }

                //网络类型常量
                //GSM-------NETWORK_TYPE_GPRS;NETWORK_TYPE_EDGE;NETWORK_TYPE_GSM
                //CDMA------NETWORK_TYPE_CDMA;NETWORK_TYPE_1xRTT;NETWORK_TYPE_IDEN;
                //CDMA2000--NETWORK_TYPE_EVDO_0;NETWORK_TYPE_EVDO_A;NETWORK_TYPE_EVDO_B;NETWORK_TYPE_EHRPD;
                //WCDMA-----NETWORK_TYPE_UMTS;NETWORK_TYPE_HSDPA;NETWORK_TYPE_HSUPA;NETWORK_TYPE_HSPA;NETWORK_TYPE_HSPAP;
                //IWLAN-----NETWORK_TYPE_IWLAN
                //TDSCDMA---NETWORK_TYPE_TD_SCDMA
                //LTE-------NETWORK_TYPE_LTE
                else {//如果使用getAllCellInfo方法不可行，用下面的程序
                    GsmCellLocation gLocation = (GsmCellLocation) tm.getCellLocation();//取得基站小区信息

                    if (netType == TelephonyManager.NETWORK_TYPE_LTE) {//LTE计算enodebid和ci
                        try {
                            LTETAC = gLocation.getLac();
                            eNodebid = gLocation.getCid() / 256;
                            LTECI = gLocation.getCid() % 256;
                            currentnet.append("  TAC:" + LTETAC).append("  eNodeBID:" + eNodebid).append("  CI:" + LTECI);
                        } catch (Exception e) {
                            makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                        }
                        ;
                    } else if (netType == TelephonyManager.NETWORK_TYPE_GPRS || netType == TelephonyManager.NETWORK_TYPE_EDGE
                            || netType == TelephonyManager.NETWORK_TYPE_GSM) {
                        GSMLAC = gLocation.getLac();
                        GSMCI = gLocation.getCid();
                        currentnet.append("  LAC:" + GSMLAC).append("  CI:" + GSMCI);
                    } else {
                        try {
                            WCDMALAC = gLocation.getLac();
                            RNC = gLocation.getCid() / 65536;
                            WCDMACI = gLocation.getCid() % 65536;
                            currentnet.append("  LAC:" + WCDMALAC).append("  RNC:" + RNC).append("  CI:" + WCDMACI);
                        } catch (Exception e) {
                            makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                        }
                        ;
                    }


                    if (netType == TelephonyManager.NETWORK_TYPE_LTE) {
                        int lte_Strength = (Integer) signalStrength.getClass().getMethod("getLteSignalStrength").invoke(signalStrength);
                        int lte_rsrp = (Integer) signalStrength.getClass().getMethod("getLteRsrp").invoke(signalStrength);
                        int lte_rsrq = (Integer) signalStrength.getClass().getMethod("getLteRsrq").invoke(signalStrength);
                        int lte_sinr = (Integer) signalStrength.getClass().getMethod("getLteRssnr").invoke(signalStrength);
                        int lte_cqi = (Integer) signalStrength.getClass().getMethod("getLteCqi").invoke(signalStrength);
                        int lte_dbm = (Integer) signalStrength.getClass().getMethod("getLteDbm").invoke(signalStrength);
                        int lte_asulevel = (Integer) signalStrength.getClass().getMethod("getLteAsuLevel").invoke(signalStrength);
                        int lte_level = (Integer) signalStrength.getClass().getMethod("getLteLevel").invoke(signalStrength);

                        RSRP = lte_rsrp;
                        SINR = lte_sinr;
                        signalInfo = " RSRP:" + RSRP + "  SINR:" + SINR + testStr;
                    } else if (netType == TelephonyManager.NETWORK_TYPE_GPRS || netType == TelephonyManager.NETWORK_TYPE_EDGE
                            || netType == TelephonyManager.NETWORK_TYPE_GSM) {
                        int gsm_dbm = signalStrength.getGsmSignalStrength();
                        int gsm_ber = signalStrength.getGsmBitErrorRate();

                        RXL = gsm_dbm;
                        BER = gsm_ber;
                        signalInfo = " RXL:" + RXL + "  BER:" + BER + testStr;
                    } else {
                        int wcdma_rscp = (Integer) signalStrength.getClass().getMethod("getWcdmaRscp").invoke(signalStrength);
                        int wcdma_ecio = (Integer) signalStrength.getClass().getMethod("getWcdmaEcio").invoke(signalStrength);
                        RSCP = wcdma_rscp;
                        ECIO = wcdma_ecio;
                        signalInfo = " RSCP:" + RSCP + "  ECIO:" + ECIO + testStr;
                    }
                }

            } catch (Exception e) {
                makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
            }
            NetInfo=currentnet.toString();
            positionText.setText(LocaInfo+"\n"+NetInfo);
            currentsignal.setText(signalInfo);
        }
    }

    private  void navigateTo() {

        LatLng desLatLng=new LatLng(lastlati, lastlongi);
        // 将GPS设备采集的原始GPS坐标转换成百度坐标

        CoordinateConverter converter  = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);

        // sourceLatLng待转换坐标
        LatLng ll = new LatLng(lastlati, lastlongi);
        converter.coord(ll);
        desLatLng = converter.convert();


        if (isFirstLocate) {


            baiduMap.setMapStatus(MapStatusUpdateFactory. newLatLng(desLatLng));
            baiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(19f));

/*            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(19f);
            baiduMap.animateMapStatus(update);*/
            isFirstLocate=false;
        }


        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.latitude(desLatLng.latitude);
        locationBuilder.longitude(desLatLng.longitude);
        MyLocationData locationData=locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }

/*
    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(5000);//5秒更新一次位置
        option.setIsNeedAddress(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setCoorType("bd09ll");
        mLocationClient.setLocOption(option);
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[]grantResults){
        switch(requestCode){
            case 1:
                if( grantResults.length>0){
                for(int result:grantResults){
                    if (result!=PackageManager.PERMISSION_GRANTED){
                        makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
//                        finish();
                        return;
                    }
                }
//                requestLocation();//百度定位
                }else{
                    makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    /*
    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation  location){
            if(location.getLocType()==BDLocation.TypeGpsLocation||location.getLocType()==BDLocation.TypeNetWorkLocation){
                navigateTo(location);
            }

            lastlati=location.getLatitude();
            lastlongi=location.getLongitude();
            lastaccuracy=location.getRadius();
            LocaInfo=String.format("经度：%.5f   纬度：%.5f   精度:%.2f米",lastlongi,lastlati,lastaccuracy);
            positionText.setText(NetInfo+LocaInfo);

            if(isTestMode){
                positionText.setText(NetInfo+LocaInfo+"\n测试模式");

                String RRR="",SEB="",TACLAC="",ENRNC="",CI="",LATI="",LONGI="";

                try{
                    //定义文件输出流
                    outStream=new FileOutputStream(file,true);

                    //获取当前时间
                    SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");
                    Date curDate =  new Date(System.currentTimeMillis());
                    String   str = formatter.format(curDate);

                    if(netType== TelephonyManager.NETWORK_TYPE_LTE){
                        RRR=RSRP+"";
                        SEB=SINR+"";
                        TACLAC=LTETAC+"";
                        ENRNC=eNodebid+"";
                        CI=LTECI+"";
                        LATI=lastlati+"";
                        LONGI=lastlongi+"";
                    }
                    else if(netType== TelephonyManager.NETWORK_TYPE_GPRS||netType== TelephonyManager.NETWORK_TYPE_EDGE
                            ||netType== TelephonyManager.NETWORK_TYPE_GSM) {
                        RRR=RXL+"";
                        SEB=BER+"";
                        TACLAC=GSMLAC+"";
                        ENRNC="";
                        CI=GSMCI+"";
                        LATI=lastlati+"";
                        LONGI=lastlongi+"";
                    }
                    else{
                        RRR=RSCP+"";
                        SEB=ECIO+"";
                        TACLAC=WCDMALAC+"";
                        ENRNC=RNC+"";
                        CI=WCDMACI+"";
                        LATI=lastlati+"";
                        LONGI=lastlongi+"";
                    }

                        String datatitle=str+",,,,,"+networktype[netType]+","+LONGI+","+LATI+","+RRR+","+SEB+","+TACLAC+","+ENRNC+","+CI+"\n";
                    try{
                        //写入文件
                        outStream.write(datatitle.getBytes("gb2312"));
                        outStream.close();
                    }catch(IOException  e){
                        e.printStackTrace();
                    }
                }catch(FileNotFoundException e){
                    e.printStackTrace();
                }
            }

        }
    }*/


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
//        mLocationClient.stop();
        baiduMap.setMyLocationEnabled(false);
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
        tm.listen(MyListener,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        enableLocationUpdates(true);//gps
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
        tm.listen(MyListener, PhoneStateListener.LISTEN_NONE);
        enableLocationUpdates(false);//gps
    }

    private void enableLocationUpdates(boolean isTurnon){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
        {
            if(isTurnon){
                isGpsEnabled=mgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetworkEnabled=mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if(!isGpsEnabled&&!isNetworkEnabled){
                    makeText(this,"请确认已启用定位功能!",Toast.LENGTH_LONG).show();
                }
                else{
                    makeText(this,"正在获取定位信息。。。",Toast.LENGTH_LONG).show();
                    if(isGpsEnabled){
                        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME,MIN_DIST,this);

                    }
                    if(isNetworkEnabled)
                        mgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MIN_TIME,MIN_DIST,this);
                }
            }
            else{
                mgr.removeUpdates(this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem  item){
        switch(item.getItemId()){
            case R.id.test:
                if(!isTestMode) {
                    isTestMode = true;
//打开文件，写入标题行
                    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))  {
                        //获取SDCARD目录
                        SDCard=Environment.getExternalStorageDirectory().getAbsoluteFile();//
                        //定义文件名称

                        File textsDir = new File(SDCard+File.separator + "Download");
                        if(!textsDir.exists()){
                            textsDir.mkdir();
                        }
                        //获取当前时间字符串
                        SimpleDateFormat formatter =  new SimpleDateFormat("yyyyMMddHHmmss");
                        Date curDate =  new Date(System.currentTimeMillis());
                        String   str = formatter.format(curDate);

                        file=new File(SDCard,File.separator + "Download"+ File.separator +str+ "test.csv");
                        Toast.makeText(this,"开启测试",Toast.LENGTH_LONG).show();

                        try {
                            file.createNewFile();
                        }catch(IOException e ){
                            e.printStackTrace();
                        }

                        try{
                            //定义文件输出流
                            outStream=new FileOutputStream(file);
                            String datatitle="测试时间,地点,楼号,单元,楼层,网络,经度,纬度,RSRP/RSCP/RXL,SINR/ECIO/BER,TAC/LAC,eNodeBID/RNC,CI\n";

                            try{
                                //写入文件
                                outStream.write(datatitle.getBytes("gb2312"));
                                outStream.close();
                            }catch(IOException  e){
                                e.printStackTrace();
                            }
                        }catch(FileNotFoundException e){
                            e.printStackTrace();
                        }

                    }

                    makeText(this, "启动测试模式", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.buildingtest:
                if(isTestMode) {
                    if (!isBuildingtest) {
                        isBuildingtest = true;
                        item.setChecked(true);
                        buildinginfo1.setVisibility(View.VISIBLE);
                        upfloor.setVisibility(View.VISIBLE);
                        downfloor.setVisibility(View.VISIBLE);
                        savedata.setVisibility(View.VISIBLE);
                        buildinglati=lastlati;
                        buildinglongi=lastlongi;
//                        Toast.makeText(this, "开启楼层测试", Toast.LENGTH_SHORT).show();

                        Intent it=new Intent(this,buildinginfo.class);
                        startActivityForResult(it,100);
                    } else {
                        isBuildingtest = false;
                        item.setChecked(false);
                        buildinginfo1.setVisibility(View.GONE);
                        upfloor.setVisibility(View.GONE);
                        downfloor.setVisibility(View.GONE);
                        savedata.setVisibility(View.GONE);
                        makeText(this, "关闭楼层测试", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    makeText(this, "请先启动测试模式", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.testend:
                if(isTestMode&&(!isBuildingtest)) {
                    isTestMode = false;
                    makeText(this, "关闭测试模式", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.upload:
                try {


                    if (!isTestMode) {
                        Intent intent=new Intent(this,FileShare.class);
                        startActivity(intent);
/*                        SDCard=Environment.getExternalStorageDirectory().getAbsoluteFile();//
                        file=new File(SDCard,File.separator + "Download"+ File.separator + "data.csv");

 //                       String sharefile = SDCard + File.separator + "Download" + File.separator + "data.csv";
//                          Toast.makeText(this,sharefile,Toast.LENGTH_LONG).show();
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
       //                 shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(sharefile)));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

      //                  (Uri.parse(new File("/sdcard/cats.jpg").toString()));
                        shareIntent.setType("application/octet-stream");
                        startActivity(Intent.createChooser(shareIntent, "上传测试数据"));

                        makeText(this, "上传测试数据", Toast.LENGTH_SHORT).show();*/
                    } else {
                        makeText(this, "请先结束测试", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }catch (Exception e){
                    Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
                }

            default:
        }
        return true;
    }
//获得楼层测试相关建筑物信息
    protected  void onActivityResult(int requestCode,int resultCode,Intent it){
        if(resultCode==RESULT_OK){
            TestLocat=it.getStringExtra("xiaoqu");
            BuildingNum=it.getStringExtra("louhao");
            BuildingEle=it.getStringExtra("danyuan");
            BuildingFloor=Integer.parseInt(it.getStringExtra("louceng"));
            displaytestdata();
        }
    }
    //测试上一层楼,最高40层
    public void upfloor(View  v){
            BuildingFloor++;
            if (BuildingFloor > 40) BuildingFloor = 40;
            displaytestdata();
    }

    //测试下一层楼，最低-5层
    public void downfloor(View  v){
        BuildingFloor--;
        if (BuildingFloor<-5) BuildingFloor=-5;
        displaytestdata();
    }

    //保存楼层测试数据
    public void savedata(View  v){
        savetestdata();
        tos.setText("已保存");
        tos.show();//提示已记录测试数据

    }
    //显示楼层测试相关信息
    public void displaytestdata(){

        buildinginfo1.setText("小区名称:"+TestLocat+"\n"+"楼        号:"+BuildingNum+"\n"+"单        元:"+BuildingEle+"\n"+"楼        层:"+BuildingFloor);
    }

    public void savetestdata(){
        String RRR="",SEB="",TACLAC="",ENRNC="",CI="",LATI="",LONGI="",datatitle ;
        try{
            //定义文件输出流
            outStream=new FileOutputStream(file,true);

            //获取当前时间
            SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");
            Date curDate =  new Date(System.currentTimeMillis());
            String   str = formatter.format(curDate);

            if(netType== TelephonyManager.NETWORK_TYPE_LTE){
                RRR=RSRP+"";
                SEB=SINR+"";
                TACLAC=LTETAC+"";
                ENRNC=eNodebid+"";
                CI=LTECI+"";
                LATI=lastlati+"";
                LONGI=lastlongi+"";
            }
            else if(netType== TelephonyManager.NETWORK_TYPE_GPRS||netType== TelephonyManager.NETWORK_TYPE_EDGE
                    ||netType== TelephonyManager.NETWORK_TYPE_GSM) {
                RRR=RXL+"";
                SEB=BER+"";
                TACLAC=GSMLAC+"";
                ENRNC="";
                CI=GSMCI+"";
                LATI=lastlati+"";
                LONGI=lastlongi+"";
            }
            else{
                RRR=RSCP+"";
                SEB=ECIO+"";
                TACLAC=WCDMALAC+"";
                ENRNC=RNC+"";
                CI=WCDMACI+"";
                LATI=lastlati+"";
                LONGI=lastlongi+"";
            }
            if(isBuildingtest){
                double numStep=Double.valueOf(BuildingFloor).floatValue()*0.00002;//每层楼经纬度增加的偏移量
                LATI=buildinglati+numStep+"";
                LONGI=buildinglongi-numStep+"";
                datatitle = str +"," + TestLocat+","+BuildingNum+","+BuildingEle+","+BuildingFloor+"," + networktype[netType] + "," +
                        LONGI + "," + LATI + "," + RRR + "," + SEB + "," + TACLAC + "," + ENRNC + "," + CI + "\n";

            }else {
                datatitle = str + ",,,,," + networktype[netType] + "," +
                        LONGI + "," + LATI + "," + RRR + "," + SEB + "," + TACLAC + "," + ENRNC + "," + CI + "\n";
            }
            try{
                //写入文件
                outStream.write(datatitle.getBytes("gb2312"));
                outStream.close();
            }catch(IOException  e){
                e.printStackTrace();
            }
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }
}


