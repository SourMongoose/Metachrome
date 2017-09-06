package com.chrisx.metachrome;

/**
 * Organized in order of priority:
 * @TODO everything
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.chrisx.metachrome.R;

import java.util.ArrayDeque;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    private Bitmap bmp;
    private Canvas canvas;
    private LinearLayout ll;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private Typeface merkur, origram, pillpopper;

    private boolean paused = false;
    private long frameCount = 0;

    private String menu = "start";

    private int level, score, turns, shuffles, currentColor;
    private float width, turnHeight, paletteY, paletteR;
    private boolean shufflePressed;

    private Triangle[][] pyramid;
    private Queue<Integer> flipQueue = new ArrayDeque<>();
    private boolean flipped = false, flipping = false;

    private static final int ROWS = 8;
    private static final int MARGIN = 30;

    private static final int[] COLORS = {
            Color.rgb(243,52,85),           //red
            Color.rgb(145,20,250),          //purple
            //Color.rgb(255,140,0),         //orange
            Color.rgb(75,200,30),           //green
            Color.rgb(50,230,250),          //blue
            Color.rgb(240,220,20)};         //yellow

    //frame data
    private static final int FRAMES_PER_SECOND = 60;
    private long nanosecondsPerFrame;
    private long millisecondsPerFrame;

    private float downX, downY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //creates the bitmap
        //note: Star 4.5 is 480x854
        bmp = Bitmap.createBitmap(Resources.getSystem().getDisplayMetrics().widthPixels,
                Resources.getSystem().getDisplayMetrics().heightPixels,
                Bitmap.Config.ARGB_8888);

        //creates canvas
        canvas = new Canvas(bmp);

        ll = (LinearLayout) findViewById(R.id.draw_area);
        ll.setBackgroundDrawable(new BitmapDrawable(bmp));

        //initializes SharedPreferences
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        nanosecondsPerFrame = (long)1e9 / FRAMES_PER_SECOND;
        millisecondsPerFrame = (long)1e3 / FRAMES_PER_SECOND;

        pyramid = new Triangle[ROWS][ROWS*2-1];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < r*2+1; c++) {
                pyramid[r][c] = new Triangle();
            }
        }

        width = (float)((w() - 2*convert854(MARGIN)) / ROWS * Math.sqrt(3) / 2);
        turnHeight = MARGIN*2 + width*(ROWS+1) + convert854(35);
        paletteY = (h() + turnHeight + convert854(15)) / 2;
        paletteR = h() - paletteY - convert854(MARGIN);

        merkur = Typeface.createFromAsset(getAssets(), "fonts/Merkur.otf");
        origram = Typeface.createFromAsset(getAssets(), "fonts/Origram.otf");
        pillpopper = Typeface.createFromAsset(getAssets(), "fonts/PillPopper.ttf");

        canvas.drawColor(Color.BLACK);

        //title screen
        drawTitleMenu();

        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //draw loop
                while (!menu.equals("quit")) {
                    long startTime = System.nanoTime();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (menu.equals("start")) {
                            } else if (menu.equals("game")) {
                                if (!paused) {
                                    //background
                                    canvas.drawRect(-1, turnHeight-convert854(35), w()+1, h()+1, newPaint(Color.BLACK));
                                    //canvas.drawColor(Color.BLACK);

                                    if (flipping) {
                                        //pyramid
                                        flipping = false;
                                        for (int r = 0; r < ROWS; r++) {
                                            for (int c = 0; c < r * 2 + 1; c++) {
                                                pyramid[r][c].update();
                                                if (pyramid[r][c].getAnimation() < 1)
                                                    flipping = true;
                                                if (pyramid[r][c].getAnimation() > 0 && pyramid[r][c].getAnimation() < 1)
                                                    pyramid[r][c].draw(canvas, (float)(w()/2 + (r-c) * width/Math.sqrt(3)),
                                                            MARGIN + width/2 + r*width, width / 2, 1 - c % 2);
                                            }
                                        }

                                        //reflection
                                        for (int c = 0; c < ROWS * 2 - 1; c++) {
                                            pyramid[ROWS - 1][c].draw(canvas, (float)(w()/2+(ROWS-1-c)*width/Math.sqrt(3)),
                                                    MARGIN + width/2 + ROWS*width, width / 2, c % 2);
                                        }
                                        Paint p = new Paint();
                                        p.setShader(new LinearGradient(0, MARGIN + width * ROWS, 0, MARGIN + width * (ROWS + 1),
                                                Color.argb(100, 0, 0, 0), Color.BLACK, Shader.TileMode.CLAMP));
                                        canvas.drawRect(0, MARGIN + width * ROWS, w(), MARGIN + width * (ROWS + 1) + 2, p);
                                    }

                                    //shuffle button
                                    Paint shuffleButton = new Paint(Paint.ANTI_ALIAS_FLAG);
                                    if (shuffles > 0 && !flipped) {
                                        if (shufflePressed)
                                            shuffleButton.setShader(new LinearGradient(0, turnHeight - convert854(40),
                                                    0, turnHeight + convert854(15), Color.rgb(255, 140, 0),
                                                    Color.rgb(255, 200, 150), Shader.TileMode.CLAMP));
                                        else
                                            shuffleButton.setShader(new LinearGradient(0, turnHeight - convert854(40),
                                                    0, turnHeight + convert854(15), Color.rgb(255, 200, 150),
                                                    Color.rgb(255, 140, 0), Shader.TileMode.CLAMP));
                                    } else {
                                        shuffleButton.setShader(new LinearGradient(0, turnHeight - convert854(40),
                                                0, turnHeight + convert854(15), Color.rgb(200, 200, 200),
                                                Color.rgb(140, 140, 140), Shader.TileMode.CLAMP));
                                    }
                                    canvas.drawRoundRect(new RectF(w()/2+convert854(MARGIN), turnHeight-convert854(40),
                                            w()-convert854(MARGIN), turnHeight+convert854(15)), convert854(15),
                                            convert854(15), shuffleButton);
                                    Paint shuffle = newPaint(Color.WHITE);
                                    shuffle.setTextSize(convert854(35));
                                    shuffle.setTextAlign(Paint.Align.CENTER);
                                    canvas.drawText("shuffle", w()*3/4, turnHeight, shuffle);
                                    canvas.drawText("turns: "+turns, w()/4, turnHeight, shuffle);

                                    //palette
                                    drawPalette(w()/2, paletteY, paletteR);
                                }
                            }

                            //update canvas
                            ll.invalidate();
                        }
                    });

                    frameCount++;

                    //wait until frame is done
                    while (System.nanoTime() - startTime < nanosecondsPerFrame);
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    //handles touch events
    public boolean onTouchEvent(MotionEvent event) {
        float X = event.getX();
        float Y = event.getY();
        int action = event.getAction();

        if (menu.equals("start")) {
            if (action == MotionEvent.ACTION_DOWN) {
                canvas.drawColor(Color.BLACK);
                level = 1;
                score = 0;
                drawLevel();
                drawScore();
                menu = "game";
                turns = 30;
                shuffles = 3;
                randomizeColors();
            }
        } else if (menu.equals("game")) {
            if (action == MotionEvent.ACTION_DOWN) {
                downX = X;
                downY = Y;
            }

            //shuffle
            if (shuffles > 0 && !flipped && X > w()/2+convert854(MARGIN) && X < w()-convert854(MARGIN)
                    && Y > MARGIN*2+width*(ROWS+1)-convert854(5) && Y < MARGIN*2+width*(ROWS+1)+convert854(50)) {
                if (action == MotionEvent.ACTION_DOWN) {
                    shufflePressed = true;
                } else if (action == MotionEvent.ACTION_UP) {
                    if (downX > w()/2+convert854(MARGIN) && downX < w()-convert854(MARGIN)
                            && downY > MARGIN*2+width*(ROWS+1)-convert854(5) && downY < MARGIN*2+width*(ROWS+1)+convert854(50)) {
                        shufflePressed = false;

                        //randomize pyramid colors
                        randomizeColors();

                        shuffles--;
                    }
                }
            } else {
                shufflePressed = false;
            }

            //palette
            if (!flipping && Math.pow(X-w()/2, 2) + Math.pow(Y-paletteY, 2) < Math.pow(paletteR, 2)) {
                if (action == MotionEvent.ACTION_DOWN) {
                    double angle = (Math.atan2(paletteY-Y, X-w()/2) + 2*Math.PI) % (2*Math.PI);
                    for (int i = 0; i < COLORS.length; i++) {
                        if (currentColor == COLORS[i]) continue;

                        double mnAngle = toRad(54) + toRad(72)*i;
                        double mxAngle = toRad(126) + toRad(72)*i;
                        double mnAngle2 = mnAngle - 2*Math.PI;
                        double mxAngle2 = mxAngle - 2*Math.PI;
                        if (angle > mnAngle && angle < mxAngle || angle > mnAngle2 && angle < mxAngle2) {
                            flipQueue.add(0);
                            pyramid[0][0].setNewColor(COLORS[i], 0);
                            pyramid[0][0].setAnimation(0);
                            flip(COLORS[i]);
                            score = (level - 1)*100 + calculateScore();
                            drawScore();
                            currentColor = COLORS[i];
                            flipping = true;
                            flipped = true;
                            turns--;
                            break;
                        }
                    }
                }
            }
        }

        return true;
    }

    //shorthand for w() and h()
    private float w() {
        return canvas.getWidth();
    }
    private float h() {
        return canvas.getHeight();
    }

    //creates an instance of Paint set to a given color
    private Paint newPaint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTypeface(pillpopper);

        return p;
    }

    private float convert854(float f) {
        return h() / (854 / f);
    }

    private void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        Paint p = newPaint(color);
        p.setStyle(Paint.Style.FILL);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();

        canvas.drawPath(path, p);
    }
    private void triangle(float x1, float y1, float x2, float y2, float x3, float y3, Paint p) {
        p.setStyle(Paint.Style.FILL);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();

        canvas.drawPath(path, p);
    }

    private void randomizeColors() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < r*2+1; c++)
                pyramid[r][c].setRandomColor();
        currentColor = pyramid[0][0].getColor();
        drawPyramid();
    }

    private double toRad(double deg) {
        return Math.PI/180*deg;
    }

    private void drawTitleMenu() {
        Paint title = newPaint(Color.WHITE);
        title.setTextAlign(Paint.Align.CENTER);
        title.setTextSize(convert854(120));
        canvas.drawText("Meta", w()/2, h()/3, title);
        canvas.drawText("Chrome", w()/2, h()/3+convert854(100), title);

        Paint start = newPaint(Color.rgb(170,170,170));
        start.setTextAlign(Paint.Align.CENTER);
        start.setTextSize(convert854(70));
        canvas.drawText("tap to", w()/2, h()*2/3, start);
        canvas.drawText("start", w()/2, h()*2/3+convert854(60), start);
    }

    private void drawLevel() {
        triangle(w()/2, 0, 0, 0, 0, MARGIN+ROWS*width, Color.BLACK);

        Paint levelText = newPaint(Color.WHITE);
        levelText.setTextSize(convert854(50));
        canvas.drawText("level", MARGIN, MARGIN+convert854(40), levelText);
        levelText.setTextSize(convert854(35));
        canvas.drawText(level+"", MARGIN, MARGIN+convert854(90), levelText);
    }

    private void drawScore() {
        triangle(w()/2, 0, w(), 0, w(), MARGIN+ROWS*width, Color.BLACK);

        Paint scoreText = newPaint(Color.WHITE);
        scoreText.setTextSize(convert854(50));
        scoreText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("score", w()-MARGIN, MARGIN+convert854(40), scoreText);
        scoreText.setTextSize(convert854(35));
        canvas.drawText(score+"", w()-MARGIN, MARGIN+convert854(90), scoreText);
    }

    private void drawPyramid() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < r * 2 + 1; c++)
                pyramid[r][c].draw(canvas, (float) (w() / 2 + (r - c) * width / Math.sqrt(3)),
                        MARGIN + width / 2 + r * width, width / 2, 1 - c % 2);
    }

    private void drawPalette(float x, float y, float r) {
        for (int i = 0; i < COLORS.length; i++) {
            double angle = toRad(64) + toRad(72)*i;
            float tmp = 2*r*r - 2*r*r*(float)Math.cos(toRad(52));
            float dst = (float)(Math.sqrt(r*r + tmp - 2*r*Math.sqrt(tmp)*Math.cos(toRad(4))));
            triangle(x+r*(float)(Math.cos(angle)), y-r*(float)(Math.sin(angle)),
                    x+r*(float)(Math.cos(angle+toRad(52))), y-r*(float)(Math.sin(angle+toRad(52))),
                    x+dst*(float)(Math.cos(angle+toRad(26))), y-dst*(float)(Math.sin(angle+toRad(26))),
                    COLORS[i]);

            if (currentColor == COLORS[i]) {
                Shader shader = new RadialGradient(x, y, r, Color.argb(0,255,255,255), Color.argb(210,255,255,255), Shader.TileMode.CLAMP);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setShader(shader);
                triangle(x+r*(float)(Math.cos(angle)), y-r*(float)(Math.sin(angle)),
                        x+r*(float)(Math.cos(angle+toRad(52))), y-r*(float)(Math.sin(angle+toRad(52))),
                        x+dst*(float)(Math.cos(angle+toRad(26))), y-dst*(float)(Math.sin(angle+toRad(26))), p);
            }
        }
    }

    private void flip(int color) {
        while (!flipQueue.isEmpty()) {
            int top = flipQueue.remove();
            int r = top / 100, c = top % 100;

            if (c % 2 == 0) {
                if (r < ROWS-1 && pyramid[r+1][c+1].getColor() == pyramid[r][c].getColor()
                        && pyramid[r+1][c+1].getAnimation() >= 1) {
                    pyramid[r+1][c+1].setAnimation(pyramid[r][c].getAnimation()-1);
                    pyramid[r+1][c+1].setNewColor(color, 2);
                    flipQueue.add((r+1)*100 + c+1);
                }
                if (c > 0 && pyramid[r][c-1].getColor() == pyramid[r][c].getColor()
                        && pyramid[r][c-1].getAnimation() >= 1) {
                    pyramid[r][c-1].setAnimation(pyramid[r][c].getAnimation()-1);
                    pyramid[r][c-1].setNewColor(color, 1);
                    flipQueue.add(r*100 + c-1);
                }
                if (c < 2*r && pyramid[r][c+1].getColor() == pyramid[r][c].getColor()
                        && pyramid[r][c+1].getAnimation() >= 1) {
                    pyramid[r][c+1].setAnimation(pyramid[r][c].getAnimation()-1);
                    pyramid[r][c+1].setNewColor(color, 0);
                    flipQueue.add(r*100 + c+1);
                }
            } else {
                if (pyramid[r-1][c-1].getColor() == pyramid[r][c].getColor()
                        && pyramid[r-1][c-1].getAnimation() >= 1) {
                    pyramid[r-1][c-1].setAnimation(pyramid[r][c].getAnimation()-1);
                    pyramid[r-1][c-1].setNewColor(color, 2);
                    flipQueue.add((r-1)*100 + c-1);
                }
                if (pyramid[r][c-1].getColor() == pyramid[r][c].getColor()
                        && pyramid[r][c-1].getAnimation() >= 1) {
                    pyramid[r][c-1].setAnimation(pyramid[r][c].getAnimation()-1);
                    pyramid[r][c-1].setNewColor(color, 0);
                    flipQueue.add(r*100 + c-1);
                }
                if (pyramid[r][c+1].getColor() == pyramid[r][c].getColor()
                        && pyramid[r][c+1].getAnimation() >= 1) {
                    pyramid[r][c+1].setAnimation(pyramid[r][c].getAnimation()-1);
                    pyramid[r][c+1].setNewColor(color, 1);
                    flipQueue.add(r*100 + c+1);
                }
            }
        }
    }

    private int calculateScore() {
        boolean[][] visited = new boolean[ROWS][2*ROWS-1];

        flipQueue.add(0);
        visited[0][0] = true;

        while (!flipQueue.isEmpty()) {
            int top = flipQueue.remove();
            int r = top / 100, c = top % 100;

            if (c % 2 == 0) {
                if (r < ROWS-1 && pyramid[r+1][c+1].getNewColor() == pyramid[r][c].getNewColor() && !visited[r+1][c+1]) {
                    visited[r+1][c+1] = true;
                    flipQueue.add((r+1)*100 + c+1);
                }
                if (c > 0 && pyramid[r][c-1].getNewColor() == pyramid[r][c].getNewColor() && !visited[r][c-1]) {
                    visited[r][c-1] = true;
                    flipQueue.add(r*100 + c-1);
                }
                if (c < 2*r && pyramid[r][c+1].getNewColor() == pyramid[r][c].getNewColor() && !visited[r][c+1]) {
                    visited[r][c+1] = true;
                    flipQueue.add(r*100 + c+1);
                }
            } else {
                if (pyramid[r-1][c-1].getNewColor() == pyramid[r][c].getNewColor() && !visited[r-1][c-1]) {
                    visited[r-1][c-1] = true;
                    flipQueue.add((r-1)*100 + c-1);
                }
                if (pyramid[r][c-1].getNewColor() == pyramid[r][c].getNewColor() && !visited[r][c-1]) {
                    visited[r][c-1] = true;
                    flipQueue.add(r*100 + c-1);
                }
                if (pyramid[r][c+1].getNewColor() == pyramid[r][c].getNewColor() && !visited[r][c+1]) {
                    visited[r][c+1] = true;
                    flipQueue.add(r*100 + c+1);
                }
            }
        }

        int nFlipped = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < 2*r+1; c++)
                if (visited[r][c]) nFlipped++;

        return 100*nFlipped/ROWS/ROWS;
    }
}
