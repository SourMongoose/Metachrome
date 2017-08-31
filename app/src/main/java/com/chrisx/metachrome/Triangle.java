package com.chrisx.metachrome;

import android.graphics.Color;

public class Triangle {
    private int color, newColor;
    private float animation, animationSpeed;
    private int flipDir; // 0\, 1/, 2|

    Triangle() {}

    int getColor() {
        return color;
    }

    void setColor(int color) {
        this.color = color;
    }
    void setNewColor(int color, float speed /*default 0.17*/, int dir) {
        this.animation = 0;
        this.newColor = color;
        this.animationSpeed = speed;
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
    }
}
