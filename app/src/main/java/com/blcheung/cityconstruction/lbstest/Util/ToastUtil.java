package com.blcheung.cityconstruction.lbstest.Util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by BLCheung.
 * Date:2018年1月2日,0002 17:12
 */

public class ToastUtil {
    private static Toast toast;

    public static void showToast(Context context, CharSequence content) {
        if (toast == null) {
            toast = Toast.makeText(context, content, Toast.LENGTH_SHORT);
        } else {
            toast.setText(content);
        }
        toast.show();
    }
}
