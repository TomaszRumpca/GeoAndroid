package pl.pg.eti.msu.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by bzajcev on 2015-10-11.
 */
public class DrawView extends ImageView {

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

}
