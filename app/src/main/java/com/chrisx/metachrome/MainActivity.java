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

    private int level, score, turns, shuffles;
    private float width, turnHeight, paletteY, paletteR;
    private boolean shufflePressed;
    private Triangle[][] pyramid;
    private static final int ROWS = 8;
    private static final int MARGIN = 30;

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
                            //background
                            canvas.drawColor(Color.BLACK);

                            if (menu.equals("start")) {
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
                            } else if (menu.equals("game")) {
                                if (!paused) {
                                    for (int r = 0; r < ROWS; r++) {
                                        for (int c = 0; c < r*2+1; c++) {
                                            eqTri((float)(w()/2+(r-c)*width/Math.sqrt(3)), MARGIN+width/2+r*width,
                                                    width/2, 1-c%2, pyramid[r][c].getColor());
                                        }
                                    }

                                    for (int c = 0; c < ROWS*2-1; c++) {
                                        eqTri((float)(w()/2+(ROWS-1-c)*width/Math.sqrt(3)), MARGIN+width/2+ROWS*width,
                                                width/2, c%2, pyramid[ROWS-1][c].getColor());
                                    }
                                    Paint p = new Paint();
                                    p.setShader(new LinearGradient(0, MARGIN+width*ROWS, 0, MARGIN+width*(ROWS+1),
                                            Color.argb(100,0,0,0), Color.BLACK, Shader.TileMode.CLAMP));
                                    canvas.drawRect(0, MARGIN+width*ROWS, w(), MARGIN+width*(ROWS+1)+2, p);

                                    Paint levelText = newPaint(Color.WHITE);
                                    levelText.setTextSize(convert854(50));
                                    canvas.drawText("level", MARGIN, MARGIN+convert854(40), levelText);
                                    Paint scoreText = new Paint(levelText);
                                    scoreText.setTextAlign(Paint.Align.RIGHT);
                                    canvas.drawText("score", w()-MARGIN, MARGIN+convert854(40), scoreText);
                                    levelText.setTextSize(convert854(35));
                                    canvas.drawText(level+"", MARGIN, MARGIN+convert854(90), levelText);
                                    scoreText.setTextSize(convert854(35));
                                    canvas.drawText(score+"", w()-MARGIN, MARGIN+convert854(90), scoreText);

                                    Paint shuffleButton = new Paint(Paint.ANTI_ALIAS_FLAG);
                                    if (shuffles > 0) {
                                        if (shufflePressed) {
                                            shuffleButton.setShader(new LinearGradient(0, turnHeight - convert854(40),
                                                    0, turnHeight + convert854(15), Color.rgb(255, 140, 0),
                                                    Color.rgb(255, 200, 150), Shader.TileMode.CLAMP));
                                        } else {
                                            shuffleButton.setShader(new LinearGradient(0, turnHeight - convert854(40),
                                                    0, turnHeight + convert854(15), Color.rgb(255, 200, 150),
                                                    Color.rgb(255, 140, 0), Shader.TileMode.CLAMP));
                                        }
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
                menu = "game";
                level = 1;
                score = 0;
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
            if (shuffles > 0 && X > w()/2+convert854(MARGIN) && X < w()-convert854(MARGIN)
                    && Y > MARGIN*2+width*(ROWS+1)-convert854(5) && Y < MARGIN*2+width*(ROWS+1)+convert854(50)) {
                if (action == MotionEvent.ACTION_DOWN) {
                    shufflePressed = true;
                } else if (action == MotionEvent.ACTION_UP) {
                    if (downX > w()/2+convert854(MARGIN) && downX < w()-convert854(MARGIN)
                            && downY > MARGIN*2+width*(ROWS+1)-convert854(5) && downY < MARGIN*2+width*(ROWS+1)+convert854(50)) {
                        shufflePressed = false;
                        for (int r = 0; r < ROWS; r++)
                            for (int c = 0; c < r * 2 + 1; c++)
                                pyramid[r][c].setRandomColor();
                        shuffles--;
                    }
                }
            } else {
                shufflePressed = false;
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

    private void eqTri(float x, float y, float r, int dir, int color) {
        if (dir > 0) triangle(x, y-r, (float)(x-r*2/Math.sqrt(3)), y+r, (float)(x+r*2/Math.sqrt(3)), y+r, color);
        else triangle(x, y+r, (float)(x-r*2/Math.sqrt(3)), y-r, (float)(x+r*2/Math.sqrt(3)), y-r, color);
    }

    private void randomizeColors() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < r*2+1; c++)
                pyramid[r][c].setRandomColor();
    }

    private double toRad(double deg) {
        return Math.PI/180*deg;
    }

    private void drawPalette(float x, float y, float r) {
        int[] colors = {Color.rgb(243,52,85),   //red
                Color.rgb(145,20,250),          //purple
                //Color.rgb(255,140,0),         //orange
                Color.rgb(75,200,30),           //green
                Color.rgb(50,230,250),          //blue
                Color.rgb(240,220,20)};         //yellow
        for (int i = 0; i < colors.length; i++) {
            double angle = toRad(64) + toRad(72)*i;
            float tmp = 2*r*r - 2*r*r*(float)Math.cos(toRad(52));
            float dst = (float)(Math.sqrt(r*r + tmp - 2*r*Math.sqrt(tmp)*Math.cos(toRad(4))));
            triangle(x+r*(float)(Math.cos(angle)), y-r*(float)(Math.sin(angle)),
                    x+r*(float)(Math.cos(angle+toRad(52))), y-r*(float)(Math.sin(angle+toRad(52))),
                    x+dst*(float)(Math.cos(angle+toRad(26))), y-dst*(float)(Math.sin(angle+toRad(26))),
                    colors[i]);
        }
    }
}
