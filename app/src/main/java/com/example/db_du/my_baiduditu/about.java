package com.example.db_du.my_baiduditu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class about extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.db_du.my_baiduditu.R.layout.activity_about);

        TextView txv=(TextView)findViewById(com.example.db_du.my_baiduditu.R.id.textView5);
        txv.setText("深度覆盖测试  v1.0\n\n居民小区基站深度覆盖测试工具\n\nQQ交流群：\n\n796247093");

    }

    public  void helpweb(View v){
        Intent helpit=new Intent(Intent.ACTION_VIEW);
        helpit.setData(Uri.parse("http://blog.sina.com.cn/s/blog_6f5c640c0102z8vs.html"));
        startActivity(helpit);
//        finish();
    }


}
