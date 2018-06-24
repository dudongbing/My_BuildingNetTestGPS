package com.example.db_du.my_baiduditu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class buildinginfo extends AppCompatActivity {

    EditText xiaoqu,louhao,danyuan,louceng;
    String xq,lh,dy,lc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buildinginfo);

        //从首选项中读取保存的小区信息
        SharedPreferences myPref=getPreferences(MODE_PRIVATE);
        xq=myPref.getString("xiaoqu","小区");
        lh=myPref.getString("louhao","1号楼");
        dy=myPref.getString("danyuan","1单元");
        lc=myPref.getString("louceng","11");

        Intent it=getIntent();
        xiaoqu=(EditText)findViewById(R.id.xiaoqu);
        louhao=(EditText)findViewById(R.id.louhao);
        danyuan=(EditText)findViewById(R.id.danyuan);
        louceng=(EditText)findViewById(R.id.louceng);

        xiaoqu.setText(xq);
        louhao.setText(lh);
        danyuan.setText(dy);
        louceng.setText(lc);


    }

    public  void onclick(View  v){
        xq=xiaoqu.getText().toString();
        lh=louhao.getText().toString();
        dy=danyuan.getText().toString();
        lc=louceng.getText().toString();

        //将小区信息写入首选项
        SharedPreferences.Editor editor=getPreferences(MODE_PRIVATE).edit();
        editor.putString("xiaoqu",xq);
        editor.putString("louhao",lh);
        editor.putString("danyuan",dy);
        editor.putString("louceng",lc);
        editor.commit();

//        if((xq!=null)&&(lh!=null)&&(dy!=null)&&(lc!=null)){
            Intent it2=new Intent();
            it2.putExtra("xiaoqu",xq);
            it2.putExtra("louhao",lh);
            it2.putExtra("danyuan",dy);
            it2.putExtra("louceng",lc);
            setResult(RESULT_OK,it2);
            finish();
//        }
    }
    //拦截返回键
    @Override
    public void onBackPressed() {

    }
}
