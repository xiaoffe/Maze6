package com.example.administrator.maze;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
//for git test
public class MainActivity extends Activity {
    private int inttestt=0;
    private static int FOUND_PATH = 0x111;
    private static int NOT_FOUND_PATH= 0x112;
    private Maze maze;
    private Button setBarr;
    private Button setBegin;
    private Button setEnd;
    private Button caculator;
    private Button setDrawPath;
    private Button setErase;

    ProgressDialog dialog;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == FOUND_PATH){

            }
            if(msg.what == NOT_FOUND_PATH){
                setBarr.setClickable(true);
                setBegin.setClickable(true);
                setEnd.setClickable(true);
                showMessage(MainActivity.this, "计算结果", "没有路径");
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        maze = (Maze)findViewById(R.id.view);
        maze.getBackground().setAlpha(50);
        maze.setRows(50);
        maze.setCols(50);
        maze.setAllCells();
        initView();
        initEvent();
    }

    private void initView(){
        setBarr = (Button)findViewById(R.id.button1);
        setBegin = (Button)findViewById(R.id.button2);
        setEnd = (Button)findViewById(R.id.button3);
        caculator = (Button)findViewById(R.id.button4);
        setDrawPath = (Button)findViewById(R.id.button0);
        setErase = (Button)findViewById(R.id.button5);
    }
    private void initEvent(){
        setDrawPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maze.clearFindPath();
                maze.enableSetDrawPath();
                Toast.makeText(getBaseContext(), "请描绘障碍", Toast.LENGTH_LONG).show();
            }
        });
        setErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maze.clearFindPath();
                maze.enableSetErase();
                Toast.makeText(getBaseContext(), "请描绘障碍", Toast.LENGTH_LONG).show();
            }
        });
        setBarr.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                maze.clearFindPath();
                maze.enableSetBarr();
                Toast.makeText(getBaseContext(), "请设置障碍", Toast.LENGTH_LONG).show();
            }
        });
        setBegin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                maze.clearFindPath();
                maze.enableSetBegin();
                Toast.makeText(getBaseContext(), "请设置起点", Toast.LENGTH_LONG).show();
            }
        });
        setEnd.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                maze.clearFindPath();
                maze.enableSetEnd();
                Toast.makeText(getBaseContext(), "请设置终点", Toast.LENGTH_LONG).show();
            }
        });
        caculator.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                disableAll();

                if(maze.isBeginNull()){
                    showMessage(MainActivity.this, "aaa", "请设置好起点");
                    return;
                }
                if(maze.isEndNull()){
                    showMessage(MainActivity.this, "bbb", "请设置好终点");
                    return;
                }
                setDrawPath.setClickable(false);
                setErase.setClickable(false);
                setBarr.setClickable(false);
                setBegin.setClickable(false);
                setEnd.setClickable(false);
                maze.disableTouchEvent();
                maze.clearData();
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        do{
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }while(!maze.hasCaculated());
                        if(maze.findPath()){
                            mHandler.sendEmptyMessage(FOUND_PATH);
                            return;
                        }else{
                            mHandler.sendEmptyMessage(NOT_FOUND_PATH);
                        }
                    }
                }).start();
                maze.initialRootAndPathes();
                maze.searchMinPath();
            }
        });
    }
    private void showMessage(Context contxt, String title, String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        setPositiveButton(builder);
        setNegativeButton(builder);
        builder.create().show();
    }
    private AlertDialog.Builder setPositiveButton(AlertDialog.Builder builder){
        return builder.setPositiveButton("aaa", null);
    }
    private AlertDialog.Builder setNegativeButton(AlertDialog.Builder builder){
        return builder.setNegativeButton("bbb", null);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.custommaze, menu);
        return true;
    }

    private void disableAll(){
        setBarr.setClickable(false);
        setBegin.setClickable(false);
        setEnd.setClickable(false);
        caculator.setClickable(false);
        setDrawPath.setClickable(false);
        setErase.setClickable(false);
        myInterface.ffff();
    }
    MyInterface myInterface = new MyInterface() {
        @Override
        public void ffff() {

        }
    };
    public interface MyInterface{
        void ffff();
    }
}
