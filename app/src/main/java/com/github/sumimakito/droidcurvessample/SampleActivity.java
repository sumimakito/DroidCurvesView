/*
 *  Copyright (c) 2014-2016 Sumi Makito
 *  https://github.com/sumimakito
 *  Please read the license before using or modifying.
 */

package com.github.sumimakito.droidcurvessample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.github.sumimakito.droidcurves.CurvesView;
import com.github.sumimakito.droidcurvessample.R;

public class SampleActivity extends Activity {
    private CurvesView curvesView;
    private TextView textView1, textView2, textView3, textView4, textView5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        curvesView = (CurvesView) findViewById(R.id.curvesView);
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);

        curvesView.setCurvesChangedListener(new CurvesView.CurvesChangedListener() {
            @Override
            public void onCallback(int controlKnotID, float newValue) {
                switch (controlKnotID) {
                    case 0:
                        textView1.setText(Math.round(newValue * 100) + "%");
                        break;
                    case 1:
                        textView2.setText(Math.round(newValue * 100) + "%");
                        break;
                    case 2:
                        textView3.setText(Math.round(newValue * 100) + "%");
                        break;
                    case 3:
                        textView4.setText(Math.round(newValue * 100) + "%");
                        break;
                    case 4:
                        textView5.setText(Math.round(newValue * 100) + "%");
                        break;
                }
            }
        });
    }
}
