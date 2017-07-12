package com.example.administrator.maze;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * 继承ImageView 实现了多点触碰的拖动和缩放

 */
public class TouchView extends AppCompatImageView {

    static final int TOP_BLOCK = 30;
    static final int BOTTOM_BLOCK = 640;
    static final int LEFT_BLOCK = 30;
    static final int NONE = 0;
    static final int DRAG = 1;     //拖动中
    static final int ZOOM = 2;     //缩放中
    static final int BIGGER = 3;   //放大ing
    static final int SMALLER = 4;  //缩小ing
    private int mode = NONE;       //当前的事件

    private float beforeLenght;   //两触点距离
    private float afterLenght;    //两触点距离
    private float scale = 0.04f;  //缩放的比例 X Y方向都是这个值 越大缩放的越快

    /*处理拖动 变量 */
    private int start_x;
    private int start_y;
    private int stop_x ;
    private int stop_y ;

    Paint p = new Paint();

    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
    }

    /**
     * 默认构造函数
     * @param context
     */
    public TouchView(Context context){
        super(context);
    }
    /**
     * 该构造方法在静态引入XML文件中是必须的
     * @param context
     * @param paramAttributeSet
     */
    public TouchView(Context context,AttributeSet paramAttributeSet){
        super(context,paramAttributeSet);
    }
    /**
     * 就算两点间的距离
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (int)Math.sqrt(x * x + y * y);
    }

    /**
     * 处理触碰..
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
              //..............................
                mode = DRAG;
                stop_x = (int) event.getRawX();
                stop_y = (int) event.getRawY();
                start_x = (int) event.getX();
                start_y = stop_y - this.getTop();
//                Log.d("position", "getRawX()" + stop_x);
//                Log.d("position", "getX()" + start_x);
//                Log.d("position", "getRawY()" + stop_y);
//                Log.d("position", "getY" + (int) event.getY());
//                Log.d("position", "Width" + this.getWidth());
//                Log.d("position", "Height" + this.getHeight());
//                Log.d("position", "Top" + this.getTop());
//                Log.d("position", "Bottom" + this.getBottom());
//                Log.d("position", "Left" + this.getLeft());
//                Log.d("position", "Right" + this.getRight());
//                Log.d("position", "=======================================");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (spacing(event) > 10f) {
                    mode = ZOOM;
                    beforeLenght = spacing(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                /*处理拖动*/
                if (mode == DRAG) {
                    stop_x = (int) event.getRawX();
                    stop_y = (int) event.getRawY();

                    if(stop_y - start_y + this.getHeight() <= TOP_BLOCK){
                        this.setFrame(stop_x - start_x, TOP_BLOCK - this.getHeight(),
                                stop_x + this.getWidth() - start_x, TOP_BLOCK);
                    }else if(stop_y - start_y >= BOTTOM_BLOCK){
                        this.setFrame(stop_x - start_x, BOTTOM_BLOCK,
                                stop_x + this.getWidth() - start_x, BOTTOM_BLOCK + this.getHeight());
                    }else{
                        this.setFrame(stop_x - start_x, stop_y - start_y,
                                stop_x + this.getWidth() - start_x, stop_y - start_y + this.getHeight());
                    }
                }
                /*处理缩放*/
                else if (mode == ZOOM) {
                    if(spacing(event)>10f)
                    {
                        afterLenght = spacing(event);
                        float gapLenght = afterLenght - beforeLenght;
                        if(gapLenght == 0) {
                            break;
                        }
                        else if(Math.abs(gapLenght)>5f)
                        {
                            if(gapLenght>0) {
                                this.setScale(scale,BIGGER);
                            }else {
                                this.setScale(scale,SMALLER);
                            }
                            beforeLenght = afterLenght;
                        }
                    }
                }
                break;
        }
        //注意了，如果将下面的false改为true。结果会屏蔽掉setBarr、setEnd等 ??
        // 貌似是 onTouchListener 的 onTouch方法返回true时，会屏蔽 onTouchEvent的onTouch
        return true;
    }
    /**
     * 实现处理缩放
     */
    private void setScale(float temp,int flag) {
        if(flag==BIGGER) {
            if(this.getWidth() >= 2000 || this.getHeight() >= 2000)
                return;
            this.setFrame(stop_x - start_x - 5, stop_y - start_y - 5,
                    stop_x + this.getWidth() - start_x + 5, stop_y - start_y + this.getHeight()  +5);
        }else if(flag==SMALLER){
            if(this.getWidth() <= 50 || this.getHeight() <= 50)
                return;
            if(stop_y - start_y + this.getHeight() - 5 <= TOP_BLOCK){
                return;
            }else if(stop_y - start_y + 5 >= BOTTOM_BLOCK){
                return;
            }
            if(stop_x - start_x + getWidth() - 5 <= LEFT_BLOCK){
                return;
            }
            this.setFrame(stop_x - start_x + 5, stop_y - start_y + 5,
                    stop_x + this.getWidth() - start_x - 5, stop_y - start_y + this.getHeight()  -5);
        }
    }
}
