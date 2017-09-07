package com.chrisx.metachrome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class Triangle {
    private int color, newColor;
    private float animation, animationSpeed;
    private int flipDir; // 0\, 1/, 2|

    Triangle() {
        this.animationSpeed = 0.1666f;
        this.animation = 1.001f;
    }

    int getColor() {
        return color;
    }
    int getNewColor() {
        return newColor;
    }
    float getAnimation() {
        return animation;
    }

    void setColor(int color) {
        this.color = color;
        this.newColor = color;
    }
    void setNewColor(int color, int dir) {
        this.newColor = color;
        this.flipDir = dir;
    }
    void setRandomColor() {
        int[] colors = {Color.rgb(243,52,85),   //red
                Color.rgb(145,20,250),          //purple
                //Color.rgb(255,140,0),         //orange
                Color.rgb(75,200,30),           //green
                Color.rgb(50,230,250),          //blue
                Color.rgb(240,220,20)};         //yellow
        this.color = colors[(int)(Math.random()*colors.length)];
        this.newColor = this.color;
    }
    void setAnimation(float animation) {
        this.animation = animation;
    }

    void update() {
        if (animation < 1) {
            animation += animationSpeed;
            if (animation > 1) {
                color = newColor;
            }
        }
    }

    private void triangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();

        c.drawPath(path, p);
    }

    private void shortTriangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
        float[] base = mdpt(x1, y1, x2, y2, 0.5f);
        float[] pt = mdpt(base[0], base[1], x3, y3, animation/*(float)Math.sin(animation*Math.PI/2)*/);
        triangle(c, x1, y1, x2, y2, pt[0], pt[1], newColor);
    }

    private float[] mdpt(float x1, float y1, float x2, float y2, float frac) {
        float[] output = {x1+(x2-x1)*frac, y1+(y2-y1)*frac};
        return output;
    }

    void draw(Canvas c, float x, float y, float r, int dir) {
        float x1, y1, x2, y2, x3, y3;
        if (dir > 0) {
            x1 = x;
            y1 = y - r;
            x2 = (float) (x - r * 2 / Math.sqrt(3));
            y2 = y + r;
            x3 = (float) (x + r * 2 / Math.sqrt(3));
            y3 = y + r;
        } else {
            x1 = x;
            y1 = y + r;
            x2 = (float) (x + r * 2 / Math.sqrt(3));
            y2 = y - r;
            x3 = (float) (x - r * 2 / Math.sqrt(3));
            y3 = y - r;
        }

        //triangle
        if (animation >= 1) triangle(c, x1, y1, x2, y2, x3, y3, color);

        //flip effect
        if (animation < 1 && animation >= 0) {
            if (flipDir == 0) shortTriangle(c, x1, y1, x2, y2, x3, y3);
            else if (flipDir == 1) shortTriangle(c, x1, y1, x3, y3, x2, y2);
            else shortTriangle(c, x2, y2, x3, y3, x1, y1);
        }
    }

    void drawBase(Canvas c, float x, float y, float r, int dir) {
        float temp = animation;
        animation = 1;
        draw(c, x, y, r, dir);
        animation = temp;
        draw(c, x, y, r, dir);
    }
}
