package com.thy.svgdoodleview.Utils;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by neavo on 14-3-15.
 */

public class UnitUtil {

	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("00");

	public static synchronized String formatSecond(long value) {
		String result;
		if (value > 60) {
			long minute = value / 60;
			long second = value % 60;

			result = DECIMAL_FORMAT.format(minute) + ":" + DECIMAL_FORMAT.format(second);
		} else {
			result = "00:" + DECIMAL_FORMAT.format(value);
		}

		return result;
	}

	public static synchronized String formatTimeToCurrent(String format, Date date) {
		String result;

		long t = (System.currentTimeMillis() - date.getTime()) / 1000;

		if (t < 3600) { //60分钟内
			int minutes = (int) Math.floor(t / 60);
			minutes = minutes==0 ? 1 : minutes;

			result = minutes+"分钟前";
		} else {
			int hours = (int) Math.floor(t / 3600);
			hours = hours==0 ? 1 : hours;

			if (hours < 24) {
				result = hours+"小时前";
			} else {
				result = formatDate(format, date);
			}
		}

		return result;
	}

	public static synchronized String formatMillisecond(long value) {
		return formatSecond(value / 1000);
	}

	public static synchronized String formatDate(String format, Date date) {
		return new SimpleDateFormat(format).format(date);
	}

	public static synchronized int toPx(Context ctx, float dp) {
		return Math.round(dp * DeviceUtil.getDPI(ctx));
	}

	public static synchronized int toDp(Context ctx, float px) {
		return Math.round((px / DeviceUtil.getDPI(ctx)));
	}

}
