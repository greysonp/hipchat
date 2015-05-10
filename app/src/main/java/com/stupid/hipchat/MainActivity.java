package com.stupid.hipchat;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;


public class MainActivity extends ActionBarActivity {

    SlidingUpPanelLayout mSlidingLayout;
    ImageView mCheatArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mSlidingLayout = (SlidingUpPanelLayout) findViewById(R.id.main_container);
        mCheatArrow = (ImageView) findViewById(R.id.main_cheatsheet_arrow);

        mSlidingLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                mCheatArrow.setRotation(180 * v);
            }

            @Override
            public void onPanelCollapsed(View view) {
            }

            @Override
            public void onPanelExpanded(View view) {
            }

            @Override
            public void onPanelAnchored(View view) {

            }

            @Override
            public void onPanelHidden(View view) {

            }
        });
    }
}
