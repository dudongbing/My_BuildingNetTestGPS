package com.example.db_du.my_baiduditu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class buildinginfo extends AppCompatActivity {

    EditText xiaoqu,louhao,danyuan,louceng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buildinginfo);

        Intent it=getIntent();
        xiaoqu=(EditText)findViewById(R.id.xiaoqu);
        louhao=(EditText)findViewById(R.id.louhao);
        danyuan=(EditText)findViewById(R.id.danyuan);
        louceng=(EditText)findViewById(R.id.louceng);

    }

    public  void onclick(View  v){
        String xq,lh,dy,lc;
        xq=xiaoqu.getText().toString();
        lh=louhao.getText().toString();
        dy=danyuan.getText().toString();
        lc=louceng.getText().toString();
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
}
