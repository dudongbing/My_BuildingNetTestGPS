package com.example.db_du.my_baiduditu;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.view.WindowManager;
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

import static android.telephony.TelephonyManager.NETWORK_TYPE_UMTS;
import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity
        implements LocationListener,DialogInterface.OnClickListener {

    static final int MIN_TIME=2000;//GPS定位，位置更新条件：X毫秒
    static final float MIN_DIST=5;//GPS定位，位置更新条件：X米
    LocationManager mgr;
    boolean isGpsEnabled;//GPS定位是否可用
    boolean isNetworkEnabled;//网络定位是否可用
    String gpsprovider;//GPS定位提供者

    private MapView mMapView = null;
//    public LocationClient mLocationClient = null;
    private TextView positionText,buildinginfo1,currentsignal;//显示网络信息、位置信息
 //   private MyLocationListener myListener = new MyLocationListener();
    private BaiduMap baiduMap;
    private  boolean isFirstLocate=true;//是否第一次定位
    private Button upfloor,downfloor,savedata;//楼内测试的三个按钮


    private double lastlati,lastlongi,lastaccuracy;//纬度、经度、精度
    private boolean isTestMode=false;//是否测试模式
    private boolean isBuildingtest=false;//是否楼层测试
    private boolean filesaved=true;//测试文件是否保存
    private String TestLocat="小区",BuildingNum="1号楼",BuildingEle="1单元";//测试地点、楼号、单元
    private int BuildingFloor=11;//楼层
    private double buildinglati,buildinglongi;//存储当前楼内测试的经纬度
    Toast tos;
    //数据文件变量
    File SDCard;//SD卡路径
    //定义文件名称
    File file;//保存数据的文件
    FileOutputStream outStream;//保存数据的输出流

//网络测试相关变量
    SignalStrengthListener MyListener;
    TelephonyManager tm;
    String[] networktype={"UNKNOWN","GPRS","EDGE","UMTS","CDMA","EVDO_0","EVDO_A","1xRTT","HSDPA","HSUPA",
            "HSPA","IDEN","EVDO_B","LTE","EHRPD","HSPA+","NETWORK_TYPE_GSM","NETWORK_TYPE_TD_SCDMA","NETWORK_TYPE_IWLAN"};
    String operator;//运营商网络
    int netType;//网络类型
    int LTETAC,eNodebid,LTECI,RSRP,SINR;//LTE网络数据
    int lastSINR;//上一个采集周期的SINR值
    int GSMLAC,GSMCI,RXL,BER;//GSM网络数据
    int WCDMALAC,RNC,WCDMACI,RSCP,ECIO;//WCDMA网络数据
    Boolean  isGetCellInfo=true;//获取网络信息是否成功
    Boolean isSINR10X;//采集的SINR是否10倍数值

    String NetInfo="",LocaInfo="",signalInfo="";//当前网络信息、当前位置信息、信号信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//设置屏幕不休眠

        //SINR值是否10倍实际的数值
        SharedPreferences myPref=getPreferences(MODE_PRIVATE);
        isSINR10X=myPref.getBoolean("SINR10X",false);

    }

/*    //屏蔽返回键
    @Override
    public boolean onKeyDown(int keyCode,KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_BACK)
            return true;//不执行父类点击事件
        return super.onKeyDown(keyCode, event);//继续执行父类其他点击事件
    }
    */

    //确认是否退出程序？
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("是否退出程序？")
                .setCancelable(false)
                .setPositiveButton("是",this)
                .setNegativeButton("否",this)
                .show();
//        moveTaskToBack(false);拦截返回键实现后台运行
    }


    //GPS回调方法
    @Override
    public void onLocationChanged(Location location) {
        gpsprovider=location.getProvider();//GPS数据提供者，程序中未使用
        lastlati=location.getLatitude();
        lastlongi=location.getLongitude();
        lastaccuracy=location.getAccuracy();

        navigateTo();//显示当前位置的地图

        LocaInfo=String.format("经度：%.5f   纬度：%.5f   精度:%.2f米",lastlongi,lastlati,lastaccuracy);
        positionText.setText(LocaInfo+"\n"+NetInfo);//在主界面中显示位置信息

        if(isTestMode&&(!isBuildingtest)){
            savetestdata();//保存测试数据
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

    //推出程序对话框事件处理
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which==DialogInterface.BUTTON_POSITIVE){
            System.exit(0);
        }
    }

    //网络信号强度监听及处理
    private class SignalStrengthListener extends PhoneStateListener {
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            StringBuilder currentnet=new StringBuilder();//保存网络信息字符串

            operator = tm.getNetworkOperator();//运营商信息
            netType = tm.getNetworkType();//网络类型

            String testStr;
            if (isTestMode) testStr="    测试模式";
            else   testStr="";

            Boolean  getCard1=false;//是否取得第一张卡的网络信息--本程序只获取第一张卡的网络信息
            isGetCellInfo=true;

            //由于getallcellinfo无法获取ECIO\BER\SINR，先设置一个较大的值，如其他方法有效，再用正确的值取代
            ECIO = 999;
            BER = 999;
            SINR = 999;

            GsmCellLocation gLocation = (GsmCellLocation) tm.getCellLocation();//取得基站小区信息
            //网络类型常量
            //GSM-------NETWORK_TYPE_GPRS;NETWORK_TYPE_EDGE;NETWORK_TYPE_GSM
            //CDMA------NETWORK_TYPE_CDMA;NETWORK_TYPE_1xRTT;NETWORK_TYPE_IDEN;
            //CDMA2000--NETWORK_TYPE_EVDO_0;NETWORK_TYPE_EVDO_A;NETWORK_TYPE_EVDO_B;NETWORK_TYPE_EHRPD;
            //WCDMA-----NETWORK_TYPE_UMTS;NETWORK_TYPE_HSDPA;NETWORK_TYPE_HSUPA;NETWORK_TYPE_HSPA;NETWORK_TYPE_HSPAP;
            //IWLAN-----NETWORK_TYPE_IWLAN
            //TDSCDMA---NETWORK_TYPE_TD_SCDMA
            //LTE-------NETWORK_TYPE_LTE
            if (netType == TelephonyManager.NETWORK_TYPE_LTE) {//LTE计算enodebid和ci
                try {
                    LTETAC = gLocation.getLac();
                    eNodebid = gLocation.getCid() / 256;
                    LTECI = gLocation.getCid() % 256;
                    currentnet.append(operator+"  ").append(networktype[netType]);
                    currentnet.append("  TAC:" + LTETAC).append("  eNodeBID:" + eNodebid).append("  CI:" + LTECI);
                } catch (Exception e) {
                    isGetCellInfo=false;
                    makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
                ;
            } else if (netType == TelephonyManager.NETWORK_TYPE_GPRS || netType == TelephonyManager.NETWORK_TYPE_EDGE
                    || netType == TelephonyManager.NETWORK_TYPE_GSM) {
                GSMLAC = gLocation.getLac();
                GSMCI = gLocation.getCid();
                currentnet.append(operator+"  ").append(networktype[netType]);
                currentnet.append("  LAC:" + GSMLAC).append("  CI:" + GSMCI);
            } else if (netType == TelephonyManager.NETWORK_TYPE_UMTS || netType == TelephonyManager.NETWORK_TYPE_HSDPA
                    || netType == TelephonyManager.NETWORK_TYPE_HSUPA || netType == TelephonyManager.NETWORK_TYPE_HSPA
                    || netType == TelephonyManager.NETWORK_TYPE_HSPAP) {
                try {
                    WCDMALAC = gLocation.getLac();
                    RNC = gLocation.getCid() / 65536;
                    WCDMACI = gLocation.getCid() % 65536;
                    currentnet.append(operator+"  ").append(networktype[netType]);
                    currentnet.append("  LAC:" + WCDMALAC).append("  RNC:" + RNC).append("  CI:" + WCDMACI);
                } catch (Exception e) {
                    isGetCellInfo=false;
                    makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }

            }else{
                currentnet.append("UNKOWN");
            }

            try {
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
                    if(RSRP!=0){//有些手机偶尔采集不到有效数据，放弃掉无效数据
                        SINR = lte_sinr;
                        lastSINR=SINR;
                    }
                    else{
                        SINR=lastSINR;
                    }
                    //如果SINR大于100，而且isSINR为false，将首选项SINR10X设为true
                    if(SINR>100 && isSINR10X==false){
                        isSINR10X=true;
                        SharedPreferences.Editor editor=getPreferences(MODE_PRIVATE).edit();
                        editor.putBoolean("SINR10X" ,true);
                        editor.commit();
                    }

                    if(isSINR10X){
                        SINR=SINR/10;
                    }
                    signalInfo = " RSRP:" + RSRP + "  SINR:" + SINR + testStr;
                } else if (netType == TelephonyManager.NETWORK_TYPE_GPRS || netType == TelephonyManager.NETWORK_TYPE_EDGE
                        || netType == TelephonyManager.NETWORK_TYPE_GSM) {
                    int gsm_dbm = signalStrength.getGsmSignalStrength();
                    int gsm_ber = signalStrength.getGsmBitErrorRate();

                    RXL = gsm_dbm;
                    BER = gsm_ber;
                    if(BER<0 ||BER >7){
                        BER=999;
                        signalInfo = " RXL:" + RXL + "  BER: - "  + testStr;
                    }
                    else{
                        signalInfo = " RXL:" + RXL + "  BER:" + BER + testStr;
                    }
                    signalInfo = " RXL:" + RXL + "  BER:" + BER + testStr;
                } else if (netType == TelephonyManager.NETWORK_TYPE_UMTS || netType == TelephonyManager.NETWORK_TYPE_HSDPA
                        || netType == TelephonyManager.NETWORK_TYPE_HSUPA || netType == TelephonyManager.NETWORK_TYPE_HSPA
                        || netType == TelephonyManager.NETWORK_TYPE_HSPAP) {
                    int wcdma_rscp = (Integer) signalStrength.getClass().getMethod("getWcdmaRscp").invoke(signalStrength);
                    int wcdma_ecio = (Integer) signalStrength.getClass().getMethod("getWcdmaEcio").invoke(signalStrength);
                    RSCP = wcdma_rscp;
                    ECIO = wcdma_ecio;
                    signalInfo = " RSCP:" + RSCP + "  ECIO:" + ECIO + testStr;
                } else {
                    signalInfo = "";
                }
            }catch(Exception  e){
                isGetCellInfo=false;
                makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
//            if(isGetCellInfo==false) {
                try {
                    //用getAllCellLocation获取基站信息
                    List<CellInfo> mcellInfos = tm.getAllCellInfo();//获取全部小区信息
                    if (mcellInfos.size() != 0) {//如果已取得小区信息
                        for (CellInfo mci : mcellInfos) {//依次处理小区信息
                            if (mci instanceof CellInfoWcdma) {//处理WCDMA小区信息

                                if (mci.isRegistered() && (getCard1 == false)) {//是否已注册小区,且未取得第一张卡的网络信息：只处理第一张卡的注册小区信息
                                    CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma) mci).getCellSignalStrength();
                                    WCDMALAC = ((CellInfoWcdma) mci).getCellIdentity().getLac();
                                    RNC = ((CellInfoWcdma) mci).getCellIdentity().getCid() / 65536;
                                    WCDMACI = ((CellInfoWcdma) mci).getCellIdentity().getCid() % 65536;
                                    int t_dbm=cellSignalStrengthWcdma.getDbm();
                                    if (t_dbm<0) {
                                        RSCP = t_dbm;
                                    }
//                                    ECIO = 999;
                                    if (netType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                                        netType = NETWORK_TYPE_UMTS;
                                    if(currentnet==null) {//如果前面的步骤未得到网络信息，添加相关信息
                                        currentnet.append(operator + "  ").append(networktype[netType]);
                                        currentnet.append("  LAC:" + WCDMALAC).append("  RNC:" + RNC).append("  CI:" + WCDMACI);
                                    }
                                    if (ECIO==999){
                                        signalInfo = " RSCP:" + RSCP + "  ECIO: - "   + testStr;
                                    }
                                    else {
                                        signalInfo = " RSCP:" + RSCP + "  ECIO:" + ECIO + testStr;
                                    }
                                    getCard1 = true;//已取得第一张卡的网络信息
                                }
                            } else if (mci instanceof CellInfoGsm) {
                                if (mci.isRegistered() && (getCard1 == false)) {//是否已注册小区
                                    CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) mci).getCellSignalStrength();
                                    GSMLAC = ((CellInfoGsm) mci).getCellIdentity().getLac();
                                    GSMCI = ((CellInfoGsm) mci).getCellIdentity().getCid();
                                    int t_dbm=cellSignalStrengthGsm.getDbm();
                                    if (t_dbm<0) {
                                        RXL = t_dbm;
                                    }
//                                    BER = 999;
                                    if (netType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                                        netType = TelephonyManager.NETWORK_TYPE_GSM;
                                    if(currentnet==null) {
                                        currentnet.append(operator + "  ").append(networktype[netType]);
                                        currentnet.append("  LAC:" + GSMLAC).append("  CI:" + GSMCI);
                                    }
                                    if(BER==999) {
                                        signalInfo = " RXL:" + RXL + "  BER: - "  + testStr;
                                    }
                                    else {
                                        signalInfo = " RXL:" + RXL + "  BER:" + BER + testStr;
                                    }
                                    getCard1 = true;
                                }
                            } else {
                                if (mci instanceof CellInfoLte) {
                                    //                          CellInfoLte cellInfoLte = (CellInfoLte) tm.getAllCellInfo().get(0);//获得第一个小区的信息
                                    if (mci.isRegistered() && (getCard1 == false)) {//是否已注册小区
                                        CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) mci).getCellSignalStrength();
                                        LTETAC = ((CellInfoLte) mci).getCellIdentity().getTac();
                                        eNodebid = ((CellInfoLte) mci).getCellIdentity().getCi() / 256;
                                        LTECI = ((CellInfoLte) mci).getCellIdentity().getCi() % 256;
                                        int t_dbm= cellSignalStrengthLte.getDbm();
                                        if (t_dbm<0) {
                                            RSRP = t_dbm;
                                        }
//                                        SINR = 999;
                                        if (netType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                                            netType = TelephonyManager.NETWORK_TYPE_LTE;
                                        if(currentnet==null) {
                                            currentnet.append(operator + "  ").append(networktype[netType]);
                                            currentnet.append("  TAC:" + LTETAC).append("  eNodeBID:" + eNodebid).append("  CI:" + LTECI);
                                        }
                                        if(SINR==999) {
                                            signalInfo = " RSRP:" + RSRP + "  SINR: - "  + testStr;
                                        }
                                        else{
                                            signalInfo = " RSRP:" + RSRP + "  SINR:" + SINR + testStr;
                                        }
                                        getCard1 = true;
                                    }

                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
 //           }//endif
            NetInfo=currentnet.toString();
            positionText.setText(LocaInfo+"\n"+NetInfo);
            currentsignal.setText(signalInfo);
        }
    }

    private  void navigateTo() {

        // 将GPS设备采集的原始GPS坐标转换成百度坐标，百度地图API
        CoordinateConverter converter  = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);

        // sourceLatLng待转换坐标
        LatLng ll = new LatLng(lastlati, lastlongi);//待转换的坐标
        converter.coord(ll);
        LatLng desLatLng = converter.convert();

        if (isFirstLocate) {//如果是第一次定位，就显示当前位置的地图
            baiduMap.setMapStatus(MapStatusUpdateFactory. newLatLng(desLatLng));
            baiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(19f));

            isFirstLocate=false;
        }

        //显示位置图标
        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.latitude(desLatLng.latitude);
        locationBuilder.longitude(desLatLng.longitude);
        MyLocationData locationData=locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }



    @Override
    //申请权限事件响应程序
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[]grantResults){
        switch(requestCode){
            case 1:
                if( grantResults.length>0){
                for(int result:grantResults){
                    if (result!=PackageManager.PERMISSION_GRANTED){
                        makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                }else{
                    makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
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

    //GPS定位使能程序
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
    //菜单事件响应程序
    public boolean onOptionsItemSelected(MenuItem  item){
        switch(item.getItemId()){
            case R.id.test://开始测试菜单
                if(!isTestMode) {//第一次点击测试菜单
                    isTestMode = true;
                    //打开要写入的测试文件，写入标题行
                    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))  {//SD卡可用
                        //获取SDCARD目录
                        SDCard=Environment.getExternalStorageDirectory().getAbsoluteFile();//
                        //定义文件名称

                        File textsDir = new File(SDCard+File.separator + "Download");//文件保存在SD卡的Downloads目录
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
                            file.createNewFile();//打开文件
                        }catch(IOException e ){
                            e.printStackTrace();
                        }

                        try{
                            //定义文件输出流
                            outStream=new FileOutputStream(file);
                            //定义文件第一行标题内容
                            String datatitle="测试时间,地点,楼号,单元,楼层,网络,经度,纬度,RSRP/RSCP/RXL,SINR/ECIO/BER,TAC/LAC,eNodeBID/RNC,CI\n";

                            try{
                                //写入文件
                                outStream.write(datatitle.getBytes("gb2312"));//按照gb2312编码写入
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
            case R.id.buildingtest://楼层测试菜单
                if(isTestMode) {
                    if (!isBuildingtest) {//第一次点击楼层测试
                        isBuildingtest = true;
                        item.setChecked(true);//勾选菜单
                        buildinginfo1.setVisibility(View.VISIBLE);
                        upfloor.setVisibility(View.VISIBLE);
                        downfloor.setVisibility(View.VISIBLE);
                        savedata.setVisibility(View.VISIBLE);
                        //将当前经纬度设为大楼经纬度
                        buildinglati=lastlati;
                        buildinglongi=lastlongi;
                        //Toast.makeText(this, "开启楼层测试", Toast.LENGTH_SHORT).show();
                        //进入楼宇信息设置界面
                        Intent it=new Intent(this,buildinginfo.class);
                        startActivityForResult(it,100);
                    } else {//再次点击楼层测试，关闭楼层测试功能
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
            case R.id.testend://结束测试菜单
                if(isTestMode&&(!isBuildingtest)) {
                    isTestMode = false;
                    makeText(this, "关闭测试模式", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.upload://数据上传菜单
                try {
                    if (!isTestMode) {
                        //打开上传数据界面
                        Intent intent=new Intent(this,FileShare.class);
                        startActivity(intent);
                    } else {
                        makeText(this, "请先结束测试", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }catch (Exception e){
                    Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
                }
            case R.id.about://关于菜单
                Intent aboutit=new Intent(this,about.class);
                startActivity(aboutit);
                break;
            case R.id.exit://退出程序菜单
                System.exit(0);
                break;

            default:
        }
        return true;
    }

    //获得楼层测试相关建筑物信息
    protected  void onActivityResult(int requestCode,int resultCode,Intent it){
        if(resultCode==RESULT_OK){
            //确认建筑物信息后，初始化楼层测试的相关数据
            MenuItem mmi=(MenuItem)findViewById(R.id.buildingtest) ;
            TestLocat=it.getStringExtra("xiaoqu");
            BuildingNum=it.getStringExtra("louhao");
            BuildingEle=it.getStringExtra("danyuan");
            BuildingFloor=Integer.parseInt(it.getStringExtra("louceng"));

            displaytestdata();//显示小区和建筑物信息
        }
    }
    //测试上一层楼,最高40层
    public void upfloor(View  v){
            BuildingFloor++;
            if (BuildingFloor > 40) BuildingFloor = 40;
            if(BuildingFloor == 0) BuildingFloor = 1;//处理楼层为0的情况
            displaytestdata();//显示小区和建筑物信息
    }

    //测试下一层楼，最低-5层
    public void downfloor(View  v){
        BuildingFloor--;
        if (BuildingFloor<-5) BuildingFloor=-5;
        if(BuildingFloor == 0) BuildingFloor = -1;//处理楼层为0的情况
        displaytestdata();//显示小区和建筑物信息
    }

    //保存楼层测试数据
    public void savedata(View  v){
        savetestdata();
        tos.setText("已保存");
        tos.show();//提示已记录测试数据

    }
    //显示楼层测试相关信息
    public void displaytestdata(){
        if(BuildingFloor == 0) BuildingFloor = 1;//处理楼层为0的情况
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

            //保存LTE数据
            if(netType== TelephonyManager.NETWORK_TYPE_LTE){
                RRR=RSRP+"";
                SEB=SINR+"";
                if (SINR==999) SEB="";
                TACLAC=LTETAC+"";
                ENRNC=eNodebid+"";
                CI=LTECI+"";
                LATI=lastlati+"";
                LONGI=lastlongi+"";
            }
            //保存数据
            else if(netType== TelephonyManager.NETWORK_TYPE_GPRS||netType== TelephonyManager.NETWORK_TYPE_EDGE
                    ||netType== TelephonyManager.NETWORK_TYPE_GSM) {
                RRR=RXL+"";
                SEB=BER+"";
                if (BER==999) SEB="";
                TACLAC=GSMLAC+"";
                ENRNC="";
                CI=GSMCI+"";
                LATI=lastlati+"";
                LONGI=lastlongi+"";
            }
            //保存WCDMA数据
            else if (netType == TelephonyManager.NETWORK_TYPE_UMTS || netType == TelephonyManager.NETWORK_TYPE_HSDPA
                    || netType == TelephonyManager.NETWORK_TYPE_HSUPA || netType == TelephonyManager.NETWORK_TYPE_HSPA
                    || netType == TelephonyManager.NETWORK_TYPE_HSPAP) {
                RRR=RSCP+"";
                SEB=ECIO+"";
                if (ECIO==999) SEB="";
                TACLAC=WCDMALAC+"";
                ENRNC=RNC+"";
                CI=WCDMACI+"";
                LATI=lastlati+"";
                LONGI=lastlongi+"";
            }else{
                RRR="";
                SEB="";
                TACLAC="";
                ENRNC="";
                CI="";
                LATI=lastlati+"";
                LONGI=lastlongi+"";
            }
            //如果是楼层测试，数据保存时增加楼层偏移量
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


