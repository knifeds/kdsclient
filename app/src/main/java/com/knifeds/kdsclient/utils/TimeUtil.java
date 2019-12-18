package com.knifeds.kdsclient.utils;

import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    private static final String TAG = "TimeUtil";

    /**
     *  Is action time due
     * @param serverdate action time
     * @return
     */
    public static boolean nowUpdate(String serverdate) {
        try{
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date serverDate = df.parse(serverdate);
            Date nowDate = new Date();
            nowDate.setTime(System.currentTimeMillis());
            if (serverDate.getTime()<=nowDate.getTime()){
                return true;
            }
        }catch (Exception e){
        }
        return false;
    }

    /**
     *  Is time between start & end time
     * @param startdate
     * @param endDate
     * @return
     */
    public static boolean betweenSchedulerDate(String startdate,String endDate) {
        if(TextUtils.isEmpty(startdate)||TextUtils.isEmpty(endDate)){
            return false;
        }
        try{
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startd = df.parse(startdate);
            Date endd = df.parse(endDate);
            Date nowDate = new Date();
            nowDate.setTime(System.currentTimeMillis());
            if (startd.getTime() <= nowDate.getTime()
                    && nowDate.getTime() <= endd.getTime()){
                return true;
            }
        }catch (Exception e){
        }
        return false;
    }
    /**
     *  Is it before scheduled date
     * @param date action time
     * @return
     */
    public static boolean beforeSchedulerDate(String date) {
        if(TextUtils.isEmpty(date)){
            return false;
        }
        try{
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date scheduledDate = df.parse(date);
            Date nowDate = new Date();
            nowDate.setTime(System.currentTimeMillis());
            if (nowDate.getTime() <= scheduledDate.getTime()){
                return true;
            }
//            String mFormatNowDate = df.format(nowDate);
//            if(java.sql.Date.valueOf(date).before(java.sql.Date.valueOf(mFormatNowDate))){//date大于mFormatNowDate
//                return false;
//            }else{
//                return true;
//            }
        }catch (Exception e){
        }
        return false;
    }
    /**
     *  Is it before scheduled time
     * @param endTime action time
     * @return
     */
    public static boolean beforeSchedulerEndTime(String endTime) {
        if(TextUtils.isEmpty(endTime)){
            endTime = "23:59";
        }
        if (endTime.equals("00:00")) {
            return true;
        }
        try{
            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
            SimpleDateFormat df1 = new SimpleDateFormat("HHmm");
            Date mEndTime = df.parse(endTime);
            int mFormatEndTime = Integer.parseInt(df1.format(mEndTime));
            Date nowDate = new Date();
            nowDate.setTime(System.currentTimeMillis());
            int mFormatNowDate = Integer.parseInt(df1.format(nowDate));
            if (mFormatNowDate <= mFormatEndTime){
                return true;
            }else{
                return false;
            }
//            if (nowDate.getTime() <= mDate.getTime()){
//                return true;
//            }
//            if(java.sql.Date.valueOf(endTime).before(java.sql.Date.valueOf(mFormatNowDate))){//date大于mFormatNowDate
//                return false;
//            }else{
//                return true;
//            }
        }catch (Exception e){
            return false;
        }
    }
    /**
     *  Is it before action time in ms
     * @param date action time
     * @return
     */
    public static long beforeSchedulerTimeMill(String date) {
        if(TextUtils.isEmpty(date)){
            return 0;
        }
        try{
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date mDate = df.parse(date);
            Date nowDate = new Date();
            nowDate.setTime(System.currentTimeMillis());
            return mDate.getTime() - nowDate.getTime();
        }catch (Exception e){
            Log.d(TAG, "beforeSchedulerTimeMill: Error checking data: " + date);
            e.printStackTrace();
        }
        return 0;
    }
    /**
     *  Is it after the action time
     * @param date action time
     * @return
     */
    public static boolean afterSchedulerDate(String date) {
        if(TextUtils.isEmpty(date)){
            return false;
        }
        try{
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
//            Date mDate = df.parse(date);
            Date nowDate = new Date();
            nowDate.setTime(System.currentTimeMillis());
            String mFormatNowDate = df.format(nowDate);
//            if (nowDate.getTime() > mDate.getTime()){
//                return true;
//            }
            if((java.sql.Date.valueOf(date).after(java.sql.Date.valueOf(mFormatNowDate)))
                    ||(java.sql.Date.valueOf(date).equals(java.sql.Date.valueOf(mFormatNowDate)))){//date大于mFormatNowDate
                return false;
            }else{
                return true;
            }
        }catch (Exception e){
        }
        return false;
    }
    /**
     *  Is the date passed
     * @param date String e.g. "2020-05-22 10:29:02"
     * @return
     */
    public static boolean afterDate(String date) {
        if(TextUtils.isEmpty(date)){
            return false;
        }
        try{
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date nowDate = new Date();
            nowDate.setTime(System.currentTimeMillis());
            String mFormatNowDate = df.format(nowDate);
            if((java.sql.Date.valueOf(date).after(java.sql.Date.valueOf(mFormatNowDate)))
                    ||(java.sql.Date.valueOf(date).equals(java.sql.Date.valueOf(mFormatNowDate)))){//date大于mFormatNowDate
                return false;
            }else{
                return true;
            }
        }catch (Exception e){
        }
        return false;
    }
}