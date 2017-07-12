package com.example.administrator.maze;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;


/**
 * Created by xiaoffe on 2016/7/29.
 */

// 这个非上帝视角，模拟人走迷宫的程序。最大的不拟人的部分是：只有在选择下一个目标点的时候才看周围
    // 而真正人走迷宫的情况是 边走边看-- xiaoffe170321

    // 算最短路径。。并且不走死胡同版本
public class Maze extends TouchView{

    /**
     * 默认构造函数
     * @param context
     */
    public Maze(Context context){
        super(context);
        init();
    }
    /**
     * 该构造方法在静态引入XML文件中是必须的
     * @param context
     * @param paramAttributeSet
     */
    public Maze(Context context,AttributeSet paramAttributeSet){
        super(context,paramAttributeSet);
        init();
    }
    /**
     * 就算两点间的距离
     */
    Paint p = new Paint();
    private int rows = 0;
    private int cols = 0;
    private float screenWidth = 0;
    private float screenHeight = 0;
    private float wStep = 0;
    private float hStep = 0;

    private Cell begin;
    private Cell end;
    private Cell[][] allCells;
    private Set<Cell> noBarrierCells = new HashSet<Cell>();
    private Set<Cell> barrierCells = new HashSet<Cell>();

    private Tree root;
    private Tree treeRef;
    private PriorityQueue<Path> pathes;
//    private Deque<Path> pathes;
//    private PriorityQueue<Path> tempQueue = new PriorityQueue<>();
    public Set<Cell> deathArea = new HashSet<Cell>();

    private boolean pathFound = false;
    private boolean caculated = false;

    private Handler handler = new Handler();
    private Cell cellPressed;
    private ShortPressed oneShortPressed;
    private BeginPressed oneBeginPressed;
    private EndPressed oneEndPressed;
    private boolean screenPressed = false;
    private double paddWidth = 0;
    private double paddHeight = 0;
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        p.setAlpha(150);
        p.setColor(Color.GRAY);
        canvas.drawLine((float)(screenWidth*.05), (float)(screenHeight*.05),
                (float)(screenWidth*.05), (float)(screenHeight*.95), p);
        canvas.drawLine((float)(screenWidth*.95), (float)(screenHeight*.05),
                (float)(screenWidth*.95), (float)(screenHeight*.95), p);
        canvas.drawLine((float)((screenWidth*.05)), (float)(screenHeight*.05),
                (float)(screenWidth*.95), (float)(screenHeight*.05), p);
        canvas.drawLine((float)((screenWidth*.05)), (float)(screenHeight*.95),
                (float)(screenWidth*.95), (float)(screenHeight*.95), p);

        p.setAntiAlias(true);
        for(Cell c : barrierCells){
            canvas.drawOval((float)(c.getCenterX() - wStep/2), (float)(c.getCenterY() - hStep/2)
                    , (float)(c.getCenterX() + wStep/2), (float)(c.getCenterY() + hStep/2), p);
        }
        p.setColor(Color.RED);
//        p.setAlpha(50);
        // 下面一段画出死胡同的这一段代码可以引起  java.util.ConcurrentModificationException
        // 一般是遍历的时候，又删减的某项。产生的报错。
        // 目前猜测是因为 在下面遍历deathArea的时候，有对其做的add的操作。
        // 所以也引起此Exception。（猜测20170330）
//        for(Cell c : deathArea){
//            canvas.drawOval((float)(c.getCenterX() - wStep/2), (float)(c.getCenterY() - hStep/2)
//                    , (float)(c.getCenterX() + wStep/2), (float)(c.getCenterY() + hStep/2), p);
//        }
        if(begin != null && end != null && treeRef != null){
            Tree tree = treeRef;
            while(tree.parent != null){
                p.setColor(Color.BLACK);
                p.setAlpha(150);
                if(!tree.cell.isSameCell(end)){
                    canvas.drawOval((float)(tree.cell.getCenterX() - wStep/8), (float)(tree.cell.getCenterY() - hStep/8)
                            , (float)(tree.cell.getCenterX() + wStep/8), (float)(tree.cell.getCenterY() + hStep/8), p);
                }
                canvas.drawLine((int)tree.cell.getCenterX(),(int)tree.cell.getCenterY(),
                        (int)tree.parent.cell.getCenterX(),(int)tree.parent.cell.getCenterY(), p);
                tree = tree.parent;
            }
        }

        if(begin != null){
            p.setColor(Color.WHITE);
            p.setAlpha(255);

            canvas.drawOval((float)(begin.getCenterX() - wStep/2), (float)(begin.getCenterY() - hStep/2)
                    , (float)(begin.getCenterX() + wStep/2), (float)(begin.getCenterY() + hStep/2), p);
        }

        if(end != null){
            p.setColor(Color.BLACK);
            p.setAlpha(255);

            canvas.drawOval((float)(end.getCenterX() - wStep/2), (float)(end.getCenterY() - hStep/2)
                    , (float)(end.getCenterX() + wStep/2), (float)(end.getCenterY() + hStep/2), p);
        }

        resetSize();
        invalidate();
    }
    private void init(){
        resetSize();
    }
    public void setRows(int rows){
        this.rows = rows;
    }
    public void setCols(int cols){
        this.cols = cols;
    }

    public void resetSize(){
        if(rows == 0 || cols == 0)
            return;
        screenWidth = getWidth();
        screenHeight = getHeight();
//        Log.d("look", "最初screenWidth" + screenWidth);
//        Log.d("look", "最初screenHeight" + screenHeight);
        wStep = (float) (screenWidth*.9 / (rows > 1 ? cols + 0.5 : cols));
        hStep = (float) (screenHeight*.9 / (1 + (cols - 1)*Math.sqrt(3)/2));
//        Log.d("look", "最初wStep" + wStep);
//        Log.d("look", "最初hStep" + hStep);
        if(wStep >= hStep && Math.abs(wStep - hStep) > 1){
            screenWidth /= wStep/hStep;
        }else{
            screenHeight /= hStep/wStep;
        }
//        Log.d("look", "调整后的wStep" + wStep);
//        Log.d("look", "调整后的hStep" + hStep);
        int l = getLeft();
        int t = getTop();
        setFrame(l, t, l + (int)screenWidth, t + (int)screenHeight);
//        Log.d("look", "调整后的screenWidth" + screenWidth);
//        Log.d("look", "调整后的screenHeight" + screenHeight);
        paddWidth = screenWidth*.05;
        paddHeight = screenHeight*.05;
    }
    public void setAllCells(){
        allCells = new Cell[rows][cols];
        for(int m = 0; m < rows; m++)
            for(int n = 0; n < cols; n++){
                allCells[m][n] = new Cell(m, n);
                noBarrierCells.add(allCells[m][n]);
            }
    }
    public Cell getCell(int m, int n){ // allCells没有值之前这个方法是不安全的。。
        if(m < 0 || m >= cols || n < 0 || n >= rows){
            return null;
        }
        return allCells[m][n];
    }

    public int twoCellDistance(Cell one, Cell oth){
        Direction direction = getDirectionTo(one, oth);
        int distance = 0;
        switch(direction){
            case d0 :
                distance = oth.getNumY() - one.getNumY();
                break;
            case d0between60:
                distance = (oth.getNumY() - one.getNumY()) + (oth.getNumX() - one.getNumX());
                break;
            case d60:
                distance = oth.getNumX() - one.getNumX();
                break;
            case d60between120:
                distance = oth.getNumX() - one.getNumX();
                break;
            case d120:
                distance = oth.getNumX() - one.getNumX();
                break;
            case d120between180:
                distance = one.getNumY() - oth.getNumY();
                break;
            case d180:
                distance = one.getNumY() - oth.getNumY();
                break;
            case d180between240:
                distance = (one.getNumX() - oth.getNumX()) + (one.getNumY() - oth.getNumY());
                break;
            case d240:
                distance = (one.getNumX() - oth.getNumX());
                break;
            case d240between300:
                distance = (one.getNumX() - oth.getNumX());
                break;
            case d300:
                distance = (one.getNumX() - oth.getNumX());
                break;
            case d300between360:
                distance = oth.getNumY() - one.getNumY();
                break;
            default:break;
        }
//        double distance = 0;
//        double x_ = one.getCenterX() - oth.getCenterX();
//        double y_ = one.getCenterY() - oth.getCenterY();
//        distance = Math.sqrt(Math.pow(x_, 2) + Math.pow(y_, 2));
        return distance;

    }
    public void enableSetDrawPath(){
        this.setOnTouchListener(setDrawPath);
    }
    public void enableSetErase(){
        this.setOnTouchListener(setErase);
    }

    public void enableSetBarr(){
        this.setOnTouchListener(setBarr);
    }
    public void enableSetBegin(){this.setOnTouchListener(setBegin);}
    public void enableSetEnd(){
        this.setOnTouchListener(setEnd);
    }
    public void disableTouchEvent(){
        this.setOnTouchListener(new OnTouchListener(){

            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                return true;
            }

        });
    }
    private void enableTouchEvent(){
        this.setOnTouchListener(new OnTouchListener(){

            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                return false;
            }
        });
    }
    public void initialRootAndPathes(){  // 使用之前先判断begin end 有没有初始化。
        root = new Tree(begin);
        treeRef = root;
        int actDistance = 0;
        int estDistance = twoCellDistance(begin, end);
//        pathes = new LinkedList<Path>();
        pathes = new PriorityQueue<Path>();
        pathes.add(new Path(treeRef, actDistance, estDistance));
    }
    public void searchMinPath(){
        new Thread(new SearchPath()).start();
    }
    public boolean isBeginNull(){
        return begin ==null;
    }
    public boolean isEndNull(){
        return end == null;
    }
    public boolean findPath(){
        return pathFound;
    }
    public boolean hasCaculated(){
        return caculated;
    }
    public void clearData(){
        pathFound = false;
        caculated = false;
    }
    public void clearFindPath(){
        pathFound = false;
    }

    class SearchPath implements Runnable{
        public void run(){
            while(!treeRef.cell.isSameCell(end)){
//                Log.d("cacu", "treeRef is " + treeRef.cell);
                ////////////////////////////////////////////////////////////////
                boolean isPassable = false;
                // 获得两组数据  ： sunShines 和 转角数据
                Pair<Set<Cell>, Set<Cell>> pair = new SunShine(treeRef.cell).getSunShineCells();
                Set<Cell> sunShines = pair.first;
                Set<Cell> corners = pair.second;

//                Log.d("xiaofffe", "sun:" + treeRef.cell  );
//                Log.d("xiaofffe", "corners :");
                for(Cell corner : corners){
//                    Log.d("xiaofffe", "" + corner);
                }

                for(Cell cel : sunShines){
                    if(cel != null){
//                        Log.d("cacu", "看看有那些sunshines：：：cel" + cel);
                    }
                }
                if(sunShines.contains(end)){
                    isPassable = true;
                }
                /////////////////////////////////////////////////////////////////
                if(isPassable){
//                    Log.d("cacu", "here1");
                    pathFound = true;
                    Tree treeEnd = new Tree(end);
                    treeRef.child_sunshines.add(treeEnd);
                    treeEnd.parent = treeRef;
                    treeRef = treeEnd;
                    enableTouchEvent();
                    caculated = true;
//                    Log.d("cacu", "here2");
                    return;
                }else{
                    // PriorityQueue用poll（）弹出权重最大还是最小的？由T的Comparable<T>（）决定
//                    Log.d("cacu", "here3");
                    Path pathNow = pathes.poll();

                    Set<Cell> relatives = new HashSet<Cell>();
                    Tree treeRefer = treeRef;
                    while((treeRefer = treeRefer.parent) != null)
                        for(Tree relative : treeRefer.child_sunshines)
                            relatives.add(relative.cell);
                    // 去掉corners在relative里出现的
                    for(Cell cell : relatives)
                        if(corners.contains(cell))
                            corners.remove(cell);
                    // 去掉sunShines在relative里出现的
                    for(Cell cell : relatives){
                        if(sunShines.contains(cell)){
                            sunShines.remove(cell);
                        }
                    }
                    if(!corners.isEmpty()){
                        for (Cell cell : sunShines) {
                            Tree tree = new Tree(cell);
                            treeRef.child_sunshines.add(tree);
                            tree.parent = treeRef;
                            if(corners.contains(cell)){
                                int actDis = pathNow.actDis + twoCellDistance(treeRef.cell, cell);
                                int estDis = twoCellDistance(cell, end);
                                pathes.add(new Path(tree, actDis, estDis));
                            }
                        }
                    }else{
                        // 不加child_sunshines
                        // 树不生长，而且看是否要砍树
                        Tree waiteToDelet = treeRef;
                        Tree waiteToDeletParent = waiteToDelet.parent;
                        do{
                            //下面这一句砍树
                            waiteToDeletParent.child_corners.remove(waiteToDelet);
                            if(waiteToDeletParent.child_corners.isEmpty()){
                                for(Tree tree : waiteToDeletParent.child_sunshines){
                                    deathArea.add(tree.cell);
                                }
                            }
                            waiteToDelet = waiteToDeletParent;
                            waiteToDeletParent = waiteToDeletParent.parent;
                        }while(waiteToDeletParent != null);
                    }

                    // 要清楚corners是sunShine的子集--20170323 （this block is core）
                    // TODO:现在要做一个优化，将sunShine里面去掉在relatives里已经出现的部分的，
                    // TODO:即新的area,如果新的area为空，那就将次拐点标志为不可走。
                    // TODO:并且，当一轮扫射的所有的拐点为不可走时，那同片的area也全部为不可走。再也不纳入到计算中去。
                    // todo: 不过上面这个tudo想做的事情应该是多余。已经在功能上达到这个效果了20170329
                    // todo: no! no! no! 有用的！！ 防止别的路线又找到这里来。
                    // 但是这里有一个启发： 预计算处理 真的很有必要。 就想视觉感官上面一看，一眼就能看出一大片块是死胡同。
//                    if(corners.isEmpty()){
//                        for(Cell cell : sunShines){
//                            if(!deathArea.contains(cell)){
//                                deathArea.add(cell);
//                            }
//                        }
//                    }
//                    Log.d("lookdeath", "deathArea.size" + deathArea.size());

                    // 如果有效的corners为空，那么就一直砍掉上一节的枝叶。一直砍
                    // 压根连加此时候的sunShines都没有意义。
                    // 所以可以理解为corners是预计算。发现没有必要使用分支corners时，压根就不去发展分支
                    // todo: 所以下面逻辑要改， 而且Tree类也要改
//                    for (Cell cell : sunShines) {
//                        if(!relatives.contains(cell)){
//                            Tree tree = new Tree(cell);
//                            treeRef.child_sunshines.add(tree);
//                            tree.parent = treeRef;
//                            if(corners.contains(cell)){
//                                int actDis = pathNow.actDis + twoCellDistance(treeRef.cell, cell);
//                                int estDis = twoCellDistance(cell, end);
//                                pathes.add(new Path(tree, actDis, estDis));
//                            }
//                        }
//                    }

                    if(pathes.isEmpty()){
//                        Log.d("cacu", "here4");
                        enableTouchEvent();
                        caculated = true;
                        return;
                    }else{
//                        Log.d("cacu", "here5");
                        treeRef = pathes.peek().treeRef;
                    }
                }
            }
        }
    }
    private Cell getPressedCell(MotionEvent e){
        int M = 0;
        int N = 0;
        double x = e.getX();
        double y = e.getY();
        if(y <= paddHeight + (1 - Math.sqrt(3)/2)*hStep || y >= screenHeight -  paddHeight || x <= paddWidth || x >= wStep*cols + wStep/2 + paddWidth ){
            return null;
        }else{
            N = (int)((y - paddHeight - (1 - Math.sqrt(3)/2)*hStep)/(hStep*Math.sqrt(3)/2));
            if(N%2 == 0){
                if(x >= wStep*cols + paddWidth){
                    return null;
                }else{
                    M = (int)((x - paddWidth)/wStep);
                    //调整一下二维数组cell在屏幕上的分部。自己的算法是想从左下角开始的
                    //然而屏幕自然分部是从屏幕左上角开始的
                    N = rows - 1 - N;
                    return getCell(M, N);
                }
            }else{
                if(x <= wStep/2 + paddWidth){
                    return null;
                }else{
                    M = (int)((x-paddWidth -wStep/2)/wStep);
                    //调整一下二维数组cell在屏幕上的分部。自己的算法是想从左下角开始的
                    //然而屏幕自然分部是从屏幕左上角开始的
                    N = rows - 1 - N;
                    return getCell(M, N);
                }
            }
        }
    }
    private OnTouchListener setEnd = new OnTouchListener(){
        @Override
        public boolean onTouch(View arg0, MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_DOWN){
                screenPressed = true;
                cellPressed = getPressedCell(e);

                if(cellPressed == null){
                    return false;
                }
                if(cellPressed.isBarrier())
                    return false;
                else{
                    if(begin != null && cellPressed.isSameCell(begin))
                        return false;
                    if(oneEndPressed != null){
                        return false;
                    }
                    oneEndPressed = new EndPressed();
                    handler.postDelayed(oneEndPressed, 100);
                }
            }
            if(e.getAction() == MotionEvent.ACTION_UP){
                screenPressed = false;
            }
            if(e.getAction() == MotionEvent.ACTION_POINTER_DOWN){

            }
            if(e.getAction() == MotionEvent.ACTION_MOVE){

            }
            return false;
        }
    };
    private OnTouchListener setBegin = new OnTouchListener(){
        @Override
        public boolean onTouch(View arg0, MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_DOWN){
                screenPressed = true;

                cellPressed = getPressedCell(e);

                if(cellPressed == null){
                    return false;
                }
                if(cellPressed.isBarrier())
                    return false;
                else{
                    if(end != null && cellPressed.isSameCell(end))
                        return false;
                    if(oneBeginPressed != null){
                        return false;
                    }
                    oneBeginPressed = new BeginPressed();
                    handler.postDelayed(oneBeginPressed, 100);
                }
            }
            if(e.getAction() == MotionEvent.ACTION_UP){
                screenPressed = false;
            }
            if(e.getAction() == MotionEvent.ACTION_POINTER_DOWN){

            }
            if(e.getAction() == MotionEvent.ACTION_MOVE){

            }
            return false;
        }
    };
    private OnTouchListener setBarr = new OnTouchListener(){
        @Override
        public boolean onTouch(View arg0, MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_DOWN){
                screenPressed = true;
                cellPressed = getPressedCell(e);

                if(cellPressed == null){
                    return false;
                }
                if(oneShortPressed != null){
                    return false;
                }
                oneShortPressed = new ShortPressed();
                if(begin != null && cellPressed.isSameCell(begin) || end != null && cellPressed.isSameCell(end)){
                    return false;
                }
                handler.postDelayed(oneShortPressed, 100);
            }
            if(e.getAction() == MotionEvent.ACTION_UP){

                screenPressed = false;
            }
            if(e.getAction() == MotionEvent.ACTION_POINTER_DOWN){

            }
            if(e.getAction() == MotionEvent.ACTION_MOVE){

            }
            return false;
        }
    };
    private OnTouchListener setErase = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_DOWN){
                cellPressed = getPressedCell(e);
                if(cellPressed == null){
                    return true;
                }
                if(cellPressed.equals(end) || cellPressed.equals(begin)){
                    return true;
                }else{
                    cellPressed.setBarrier(false);
                    barrierCells.remove(cellPressed);
                    noBarrierCells.add(cellPressed);
                }
            }
            if(e.getAction() == MotionEvent.ACTION_UP){

            }
            if(e.getAction() == MotionEvent.ACTION_POINTER_DOWN){

            }
            if(e.getAction() == MotionEvent.ACTION_MOVE){
                cellPressed = getPressedCell(e);
                if(cellPressed == null){
                    return true;
                }
                if(cellPressed.equals(end) || cellPressed.equals(begin)){
                    return true;
                }else{
                    cellPressed.setBarrier(false);
                    barrierCells.remove(cellPressed);
                    noBarrierCells.add(cellPressed);
                }
            }
            return true;
        }
    };


    private OnTouchListener setDrawPath = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_DOWN){
                cellPressed = getPressedCell(e);
                if(cellPressed == null){
                    return true;
                }
                if(cellPressed.equals(end) || cellPressed.equals(begin)){
                    return true;
                }else{
                    cellPressed.setBarrier(true);
                    barrierCells.add(cellPressed);
                    noBarrierCells.remove(cellPressed);
                }
            }
            if(e.getAction() == MotionEvent.ACTION_UP){

            }
            if(e.getAction() == MotionEvent.ACTION_POINTER_DOWN){

            }
            if(e.getAction() == MotionEvent.ACTION_MOVE){
                cellPressed = getPressedCell(e);
                if(cellPressed == null){
                    return true;
                }
                if(cellPressed.equals(end) || cellPressed.equals(begin)){
                    return true;
                }else{
                    cellPressed.setBarrier(true);
                    barrierCells.add(cellPressed);
                    noBarrierCells.remove(cellPressed);
                }
            }
            return true;
        }
    };

    private boolean isInMaze(Cell cell){
        if(cell.m < 0 || cell.m >= cols || cell.n < 0 || cell.n >= rows){
            return false;
        }
        return true;
    }
    class SunShine{
        private Cell sun;
        public SunShine(Cell sun){
            this.sun = sun;
        }
        public Pair<Set<Cell>, Set<Cell>> getSunShineCells(){
//            Log.d("cacu", "SUN:......................... " + sun);
            int circle = 1;
            Set<Cell> sunShines = new HashSet<Cell>();
            Set<Cell> corners = new HashSet<Cell>();
            //上一层的障碍集合和非障碍集合
            Set<Cell> barriersInLastCircle = new HashSet<Cell>();
            Set<Cell> noBarriersInLastCircle = new HashSet<Cell>();
            while(circle == 1 || (circle > 1 && !noBarriersInLastCircle.isEmpty())){
                //这一层的障碍集合和非障碍集合
                Set<Cell> barriersInThisCircle = sun.getBarrierCircleCells(circle);
                Set<Cell> noBarriersInThisCircle = sun.getNoBarrierCircleCells(circle);
//                Log.d("cacu", "层数：：" + circle);
//                for(Cell c1 : barriersInThisCircle){
//                    Log.d("cacu", ":障碍：：" + c1);
//                }
//                for(Cell c2 : noBarriersInThisCircle){
//                    Log.d("cacu", "非障碍:：：" + c2);
//                }

                if(noBarriersInThisCircle.isEmpty()){
                    break;
                }
                for(Cell c : barriersInLastCircle){
//                    if(c == null){
//                        Log.d("cacu", "上一层的障碍 == null");
//                    }else{
//                        Log.d("cacu", "上一层的障碍" + c);
//                    }
                    Direction direction = getDirectionTo(sun, c);
                    Set<Cell> shadows = getShadowCells(c, direction);
                    for(Cell shadow: shadows){
//                        Log.d("cacu", "上一层映射到下一层的shadow"+shadow);
                        //考虑到shadow里面会有空的情况
                        if(shadow == null){
                            continue;
                        }
                        if(!barriersInThisCircle.contains(shadow)){
                            barriersInThisCircle.add(shadow);
                        }
                        if(noBarriersInThisCircle.contains(shadow)){
                            noBarriersInThisCircle.remove(shadow);
                            Set<Cell> maybeCorners = shadow.getCircleCells(1);
                            // 理论上下面出现的数量为0或1。不会大于1
                            for(Cell maybe : maybeCorners){
                                if(noBarriersInLastCircle.contains(maybe)){
                                    //下面这个条件很重要
                                    if(c.isBarrier()){
                                        corners.add(maybe);
                                    }
                                }
                            }
                        }
                    }
                }
                barriersInLastCircle = barriersInThisCircle;
                noBarriersInLastCircle = noBarriersInThisCircle;
                sunShines.addAll(noBarriersInThisCircle);
                circle++;
            }

            return new Pair(sunShines, corners);
        }
    }
    class Cell{
        @Override
        public String toString(){
            return "cell("  + getM()  + "," + getN() + ")";
        }
//        m,n 指的是二维数组坐标。而numX，numY指的是XY坐标
        private int numX;
        private int numY;
        public boolean barrier = false;
        public int m ;
        public int n ;
        public Cell(int m, int n){
            this.m = m;
            this.n = n;
            numX = m - n/2;
            numY = n;
        }
        public Cell(int m, int n, boolean barrier){
            this.m = m;
            this.n = n;
            numX = m - n/2;
            numY = n;
            this.barrier = barrier;
        }
        public int getM(){
            return m;
        }
        public int getN(){
            return n;
        }
        public int getNumX(){return numX;}
        public int getNumY(){return numY;}
        public double getCenterX(){
            return m*wStep + (n%2 == 0?wStep/2:wStep) + paddWidth;
        }
        public double getCenterY(){
            return screenHeight - (n*Math.sqrt(3)*hStep/2 + hStep/2 + paddHeight);
        }
        public boolean isBarrier(){
            return barrier;
        }
        private void setBarrier(boolean bool){
            barrier = bool;
        }
        public void switchBarrier(){ barrier = barrier?false:true; }
        public boolean isSameCell(Cell cell){
            return this.m == cell.m && this.n == cell.n;
        }

        private Set<Cell> getCircleCells(int circle){
//            Log.d("cacu", "检测" + this + "的第" + circle + "圈。。。。。。。。。。。。");
            Set<Cell> circleCells = new HashSet<Cell>(); // 没有Cell的default constructor不知道能否编译通过
            int x = this.numX - circle;
            int y = this.numY;
            for(int n = 1; n <= circle; n++){
                y += 1;
                Cell cell = reachCellByXY(x, y);
                if(cell == null){
//                    Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为 null" );
                    continue;
                }
//                Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为" + cell );
                circleCells.add(cell);
            }
            for(int n = 1; n<= circle; n++){
                // 之前这里是 x =+ 1. 我草。这都可以？？ 应该是 x += 1.  这尼玛不报错的
                x += 1;
                Cell cell = reachCellByXY(x, y);
                if(cell == null){
//                    Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为 null" );
                    continue;
                }
//                Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为" + cell );
                circleCells.add(cell);
            }
            for(int n = 1; n<= circle; n++){
                x += 1;
                y -= 1;
                Cell cell = reachCellByXY(x, y);
                if(cell == null){
//                    Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为 null" );
                    continue;
                }
//                Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为" + cell );
                circleCells.add(cell);
            }
            for(int n = 1; n<= circle; n++){
                y -= 1;
                Cell cell = reachCellByXY(x, y);
                if(cell == null){
//                    Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为 null" );
                    continue;
                }
//                Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为" + cell );
                circleCells.add(cell);
            }
            for(int n = 1; n <= circle; n++){
                x -= 1;
                Cell cell = reachCellByXY(x, y);
                if(cell == null){
//                    Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为 null" );
                    continue;
                }
//                Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为" + cell );
                circleCells.add(cell);
            }
            for(int n = 1; n <= circle; n++){
                x -= 1;
                y += 1;
                Cell cell = reachCellByXY(x, y);
                if(cell == null){
//                    Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为 null" );
                    continue;
                }
//                Log.d("cacu", "x:" + x + "y:" + y + "对应的cell为" + cell );
                circleCells.add(cell);
            }
            return circleCells;
        }
        public Set<Cell> getBarrierCircleCells(int circle){
            Set<Cell> cells = getCircleCells(circle);
            for(Cell cell : new HashSet<>(cells)){
                if(!isInMaze(cell)){
                    // 不在地图内，也认为是障碍。考虑到迷宫如果有中空的情况（当然这里没有中空）
                }else{
                    if(!cell.isBarrier() && !deathArea.contains(cell)){
                        cells.remove(cell);
                    }
                }
            }
            return cells;
        }

        public Set<Cell> getNoBarrierCircleCells(int circle){
            Set<Cell> cells = getCircleCells(circle);
            // 当用 for-each 时 有remove操作。一定要记得使用副本来for-each  下面是有问题的20160911
//            for(Cell cell : cells){
//                if(!isInMaze(cell)){
//                    cells.remove(cell);
//                }else{
//                    if(cell.isBarrier()){
//                        cells.remove(cell);
//                    }
//                }
//            }
            for(Cell cell : new HashSet<Cell>(cells)){
                if(!isInMaze(cell)){
                    cells.remove(cell);
                }else{
                    if(cell.isBarrier() || deathArea.contains(cell)){
                        cells.remove(cell);
                    }
                }
            }
            return cells;
        }
    }
    class Tree {
        public Tree parent;
        public Cell cell;
        // 有效sunshines和有效corners（在relative里面没有的）
        public Set<Tree> child_sunshines = new HashSet<Tree>();
        public Set<Tree> child_corners = new HashSet<Tree>();
        public Tree(Cell cell){
            this.cell = cell;
        }
    }
    class Path implements Comparable<Path>{
        private Tree treeRef;  // 用来指定路径最后一点 在树中的位置。。
        private int actDis;
        private int estDis;
        public Path(Tree treeRef, int actDis, int estDis){
            this.treeRef = treeRef;
            this.actDis = actDis;
            this.estDis = estDis;
        }

        @Override
        public int compareTo(Path arg){
            return actDis + estDis > arg.actDis + arg.estDis ? 1 : (actDis + estDis == arg.actDis + arg.estDis ? 0 : -1);
        //  双向列表版本用下面的
        //  return actDis + estDis < arg.actDis + arg.estDis ? 1 : (actDis + estDis == arg.actDis + arg.estDis ? 0 : -1);
        }
    }
    public class ShortPressed implements Runnable{
        @Override
        public void run() {
            if(!screenPressed){
                if(cellPressed.isBarrier()){
                    cellPressed.switchBarrier();
                    barrierCells.remove(cellPressed);
                    noBarrierCells.add(cellPressed);
                }else{
                    cellPressed.switchBarrier();
                    barrierCells.add(cellPressed);
                    noBarrierCells.remove(cellPressed);
                }
            }
            oneShortPressed = null;
        }
    }
    public class BeginPressed implements Runnable{
        @Override
        public void run() {
            if(!screenPressed){
                begin = cellPressed;
            }
            oneBeginPressed = null;
        }
    }
    public class EndPressed implements Runnable{
        @Override
        public void run() {
            if(!screenPressed){
                end = cellPressed;
            }
            oneEndPressed = null;
        }
    }
    private enum Direction{
        d0,
        d0between60,
        d60,
        d60between120,
        d120,
        d120between180,
        d180,
        d180between240,
        d240,
        d240between300,
        d300,
        d300between360,
    }
    private Direction getDirectionTo(Cell one, Cell oth){
        int vectorX = oth.getNumX() - one.getNumX();
        int vectorY = oth.getNumY() - one.getNumY();
        Direction direction = null;
        if(vectorX == 0){
            if(vectorY == 0){
                //
            }else if(vectorY > 0){
                direction = Direction.d0;
            }else if(vectorY < 0){
                direction = Direction.d180;
            }
        }else if(vectorX > 0){
            if(vectorY > 0){
                direction = Direction.d0between60;
            }else if(vectorY == 0){
                direction = Direction.d60;
            }else if(vectorY < 0){
                int absVectorX = Math.abs(vectorX);
                int absVectorY = Math.abs(vectorY);
                if(absVectorX > absVectorY){
                    direction = Direction.d60between120;
                }else if(absVectorX == absVectorY){
                    direction = Direction.d120;
                }else if(absVectorX < absVectorY){
                    direction = Direction.d120between180;
                }
            }
        }else if(vectorX < 0){
            if(vectorY > 0){
                int absVectorX = Math.abs(vectorX);
                int absVectorY = Math.abs(vectorY);
                if(absVectorX < absVectorY){
                    direction = Direction.d300between360;
                }else if(absVectorX == absVectorY){
                    direction = Direction.d300;
                }else if(absVectorX > absVectorY){
                    direction = Direction.d240between300;
                }
            }else if(vectorY == 0){
                direction = Direction.d240;
            }else if(vectorY < 0){
                direction = Direction.d180between240;
            }
        }
//        Log.d("cacu", one + " " + oth + " 的方向" + (Direction)(direction));
        return direction;
    }
    private Set<Cell> getShadowCells(Cell c, Direction d){
//        Log.d("cacu", "ooooooooooooooooooooooooooo  " + c + " " + d);
        Set<Cell> shadowCells = new HashSet<Cell>();
        int numX = c.getNumX();
        int numY = c.getNumY();
        switch (d) {
            case d0 :
                shadowCells.add(reachCellByXY(numX,numY+1));
                shadowCells.add(reachCellByXY(numX-1,numY+1));
                shadowCells.add(reachCellByXY(numX+1,numY));
                break;
            case d0between60:
                shadowCells.add(reachCellByXY(numX,numY+1));
                shadowCells.add(reachCellByXY(numX+1,numY));
                break;
            case d60:
                shadowCells.add(reachCellByXY(numX+1,numY));
                shadowCells.add(reachCellByXY(numX,numY+1));
                shadowCells.add(reachCellByXY(numX+1,numY-1));
                break;
            case d60between120:
                shadowCells.add(reachCellByXY(numX+1,numY));
                shadowCells.add(reachCellByXY(numX+1,numY-1));
                break;
            case d120:
                shadowCells.add(reachCellByXY(numX+1,numY-1));
                shadowCells.add(reachCellByXY(numX+1,numY));
                shadowCells.add(reachCellByXY(numX,numY-1));
                break;
            case d120between180:
                shadowCells.add(reachCellByXY(numX,numY-1));
                shadowCells.add(reachCellByXY(numX+1,numY-1));
                break;
            case d180:
                shadowCells.add(reachCellByXY(numX,numY-1));
                shadowCells.add(reachCellByXY(numX+1,numY-1));
                shadowCells.add(reachCellByXY(numX-1,numY));
                break;
            case d180between240:
                shadowCells.add(reachCellByXY(numX,numY-1));
                shadowCells.add(reachCellByXY(numX-1,numY));
                break;
            case d240:
                shadowCells.add(reachCellByXY(numX-1,numY));
                shadowCells.add(reachCellByXY(numX-1,numY+1));
                shadowCells.add(reachCellByXY(numX,numY-1));
                break;
            case d240between300:
                shadowCells.add(reachCellByXY(numX-1,numY));
                shadowCells.add(reachCellByXY(numX-1,numY+1));
                break;
            case d300:
                shadowCells.add(reachCellByXY(numX-1,numY+1));
                shadowCells.add(reachCellByXY(numX,numY+1));
                shadowCells.add(reachCellByXY(numX-1,numY));
                break;
            case d300between360:
                shadowCells.add(reachCellByXY(numX,numY+1));
                shadowCells.add(reachCellByXY(numX-1,numY+1));
                break;
            default:break;
        }
//        Log.d("cacu", "ooooooooooooooooooooooooooo  retrun shadows" + shadowCells);
        return shadowCells;
    }
    private Cell reachCellByXY(int x, int y){
//        Log.d("cacu", ",,,,,,x:" + x + " y:" + y + " x + y/2: " + (x + y/2) + " y:" + y);
        return getCell(x + y/2, y);
    }

}