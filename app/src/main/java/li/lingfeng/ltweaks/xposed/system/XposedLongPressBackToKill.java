package li.lingfeng.ltweaks.xposed.system;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.view.KeyEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PackageUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/12/14.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_keys_long_press_back_to_kill)
public class XposedLongPressBackToKill extends XposedBase {

    private static final String PHONE_WINDOW_MANAGER = "com.android.server.policy.PhoneWindowManager";
    private Context mContext;
    private Handler mHandler;

    private Runnable mKillRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
                vibrator.vibrate(10);

                ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                String packageName = activityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
                PackageUtils.killPackage(mContext, packageName);
            } catch (Throwable e) {
                Logger.e("Failed to kill process, " + e);
            }
        }
    };

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(PHONE_WINDOW_MANAGER, "interceptKeyBeforeDispatching", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mContext == null) {
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                }

                KeyEvent keyEvent = (KeyEvent) param.args[1];
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0) {
                    int action = keyEvent.getAction();
                    if (action == KeyEvent.ACTION_DOWN) {
                        mHandler.postDelayed(mKillRunnable, 1000);
                    } else if (action == KeyEvent.ACTION_UP) {
                        mHandler.removeCallbacks(mKillRunnable);
                    }
                }
            }
        });
    }
}
