package net.leolink.android.ninegagtvanimation;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by leolink on 6/8/14.
 */
public class Util {
    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
}
