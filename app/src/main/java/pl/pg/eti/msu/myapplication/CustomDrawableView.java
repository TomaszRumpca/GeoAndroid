package pl.pg.eti.msu.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by bzajcev on 2015-10-13.
 */
public class CustomDrawableView extends ImageView {

    private ShapeDrawable mDrawable;


    public CustomDrawableView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CustomDrawableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomDrawableView(Context context) {
        super(context);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawable == null) return;
        else
            mDrawable.draw(canvas);
    }

    public void pushDrawable(ShapeDrawable mDrawable) {
        this.mDrawable = mDrawable;
    }

}
