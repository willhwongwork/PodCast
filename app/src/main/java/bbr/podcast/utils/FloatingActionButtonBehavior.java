package bbr.podcast.utils;

import android.content.Context;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import bbr.podcast.R;

/**
 * Created by Me on 5/20/2017.
 */

public class FloatingActionButtonBehavior extends FloatingActionButton.Behavior {
    private static final String LOG_TAG = FloatingActionButtonBehavior.class.getSimpleName();

    public FloatingActionButtonBehavior(Context context, AttributeSet attrs) {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        CardView cardView = (CardView) parent.findViewById(R.id.controls_container);
        Log.d(LOG_TAG, "layoutDependsOn " + (dependency == cardView));
        return dependency == cardView;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        CardView cardView = (CardView) parent.findViewById(R.id.controls_container);
        Log.d(LOG_TAG, "dependency is cardView " + (dependency == cardView));
        Log.d(LOG_TAG, "dependency.getTranslationY " + dependency.getTranslationY() + " dependency.getHeight " + dependency.getHeight());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(LOG_TAG, "dependency " + dependency.getAccessibilityClassName());
        }
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        Log.d(LOG_TAG, "translationY " + translationY);

        if (dependency == cardView){
            child.setTranslationY(translationY);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
                               View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed);

        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            child.hide();
        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            child.show();
        }
    }
}