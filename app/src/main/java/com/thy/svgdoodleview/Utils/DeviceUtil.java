package com.thy.svgdoodleview.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

public class DeviceUtil {

	public static synchronized int getCoreNum() {
		int result = 1;

		try {
			File[] files = new File("/sys/devices/system/cpu/").listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return Pattern.matches("cpu[0-9]", pathname.getName());
				}
			});

			result = files.length;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static synchronized long getCoreMaxFreq() {
		long freq = 0;

		try {
			String[] args = {
					"/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"
			};

			Process process = new ProcessBuilder(args).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			freq = Long.parseLong(reader.readLine()) / 1000;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return freq;
	}

	public static synchronized boolean isLowProfile() {
		return getCoreNum() < 4 || getCoreMaxFreq() < 1300;
	}

	public static synchronized float getDPI(Context ctx) {
		return ctx.getResources().getDisplayMetrics().density;
	}

	public static synchronized boolean isTablet(Context ctx) {
		return (ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
				>=
				Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	public static synchronized boolean isLandscape(Context ctx) {
		return ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	public static synchronized int getScreenWidth(Context ctx) {
		Point point = new Point();
		WindowManager manager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		manager.getDefaultDisplay().getSize(point);

		return point.x;
	}

	public static synchronized int getScreenHeight(Context ctx) {
		Point point = new Point();
		WindowManager manager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		manager.getDefaultDisplay().getSize(point);

		return point.y;
	}

	public static synchronized boolean hasNavigationBar(Context ctx) {
		boolean hasSoftwareKeys = true;
		WindowManager windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR1){
			Display d = windowManager.getDefaultDisplay();

			DisplayMetrics realDisplayMetrics = new DisplayMetrics();
			d.getRealMetrics(realDisplayMetrics);

			int realHeight = realDisplayMetrics.heightPixels;
			int realWidth = realDisplayMetrics.widthPixels;

			DisplayMetrics displayMetrics = new DisplayMetrics();
			d.getMetrics(displayMetrics);

			int displayHeight = displayMetrics.heightPixels;
			int displayWidth = displayMetrics.widthPixels;

			hasSoftwareKeys =  (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
		}else{
			boolean hasMenuKey = ViewConfiguration.get(ctx).hasPermanentMenuKey();
			boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
			hasSoftwareKeys = !hasMenuKey && !hasBackKey;
		}
		return hasSoftwareKeys;
	}

	public static synchronized int getNavigationBarHeight(Context ctx) {
		int result = 0;

		Resources resources = ctx.getResources();
		int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = resources.getDimensionPixelSize(resourceId);
		}

		return result;
	}

	public static synchronized long getExternalFreeSpace() {
		long result = -1;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
			result = BigDecimal.valueOf(fs.getAvailableBlocks()).multiply(BigDecimal.valueOf(fs.getBlockSize())).longValue();
		}

		return result;
	}

	public static synchronized long getExternalTotalSpace() {
		long result = 0;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
			result = BigDecimal.valueOf(fs.getBlockCount()).multiply(BigDecimal.valueOf(fs.getBlockSize())).longValue();
		}

		return result;
	}

	public static synchronized boolean hasSdCard() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
	}

	public static String getVersionName(Context ctx) {
		PackageInfo pi = getSoftwarePackageInfo(ctx);
		if(pi != null) {
			return pi.versionName;
		}

		return "";
	}

	public static final synchronized PackageInfo getSoftwarePackageInfo(Context context) {
        if(context == null)
            return null;
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(),
                    0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
			return null;
        }
        return packInfo;
    }

	/**
	 * 获取SIM卡IMSI
	 * */
	public static String getSimIMSI(Context context) {
		TelephonyManager tmManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = "";
		if(tmManager != null) {
			imsi = tmManager.getSubscriberId();
		}
		if(imsi == null) imsi = "";
		return imsi;
	}

	public static String getNetWorkType(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network = connManager.getActiveNetworkInfo();
		if(network == null) {
			return null;
		}
		String netTypeName = connManager.getActiveNetworkInfo().getTypeName();
		return netTypeName == null ? "" : netTypeName;
	}

	/**
	 *
	 * @return -1：未知，0：无网，1：wifi，2：2g，3：3g，4：4g
	 * */
	public static int getCurrentNetType(Context context) {
		int type = -1;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info == null) {
			type = 0;
		} else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
			type = 1;
		} else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
			int subType = info.getSubtype();
			if (subType == TelephonyManager.NETWORK_TYPE_CDMA || subType == TelephonyManager.NETWORK_TYPE_GPRS
					|| subType == TelephonyManager.NETWORK_TYPE_EDGE) {
				type = 2;
			} else if (subType == TelephonyManager.NETWORK_TYPE_UMTS || subType == TelephonyManager.NETWORK_TYPE_HSDPA
					|| subType == TelephonyManager.NETWORK_TYPE_EVDO_A || subType == TelephonyManager.NETWORK_TYPE_EVDO_0
					|| subType == TelephonyManager.NETWORK_TYPE_EVDO_B) {
				type = 3;
			} else if (subType == TelephonyManager.NETWORK_TYPE_LTE) {// LTE是3g到4g的过渡，是3.9G的全球标准
				type = 4;
			}
		}
		return type;
	}

	/**
	 * 获取deviceID IMEI
	 * */
	public static String getDevicesId( Context context) {
		if(context == null)
			return null;
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String devicesID = "";
		if(tm != null) {
			devicesID = tm.getDeviceId();
		}
		if(devicesID == null) {
			devicesID = "";
		}
		return devicesID;
	}

	/**
	 * Role:Telecom service providers获取手机服务商信息 <BR>
	 * 需要加入权限<uses-permission
	 * android:name="android.permission.READ_PHONE_STATE"/> <BR>
	 * Date:2012-3-12 <BR>
	 *
	 * @author CODYY)peijiangping
	 */
	public static int getProviders(Context ctx) {
		int provider = 0;
		// 返回唯一的用户ID;就是这张卡的编号神马的
		String IMSI = getSimIMSI(ctx);
		// IMSI号前面3位460是国家，紧接着后面2位00 02是中国移动，01是中国联通，03是中国电信。
		System.out.println(IMSI);
		if (IMSI.startsWith("46000") || IMSI.startsWith("46002")) {
			provider = 1; // 中国移动
		} else if (IMSI.startsWith("46001")) {
			provider = 2; // 中国联通
		} else if (IMSI.startsWith("46003")) {
			provider = 3; // 中国电信
		}
		return provider;
	}

	/**
	 * 获取屏幕分辨率
	 * */
	public static String getResolution(Context ctx) {
		return getScreenWidth(ctx) + "x" + getScreenHeight(ctx);
	}

	/**
	 * 获取设备名称
	 * */
	public static String getDeviceName(){
		return new Build().MODEL;
	}

	/**
	 * 获取系统版本
	 *
	 * */
	public static String getSysVersion() {
		return Build.VERSION.RELEASE;
	}

}
