package com.knifeds.kdsclient.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Schedule {
    @Expose
    public String StartDate;
    @Expose
    public String EndDate;
    @Expose
    public List<Playlist> PlayList;
    @Expose
    public int Type;

    public Schedule(){

    }

    public String getStartDate() {
        return StartDate;
    }

    public void setStartDate(String startDate) {
        StartDate = startDate;
    }

    public String getEndDate() {
        return EndDate;
    }

    public Date getFormatEnddate() {
        return stringToDate(EndDate,"yyyy-MM-dd");
    }

    public Date getFormatStartdate() {
        return stringToDate(StartDate,"yyyy-MM-dd");
    }

    public void setEndDate(String endDate) {
        EndDate = endDate;
    }

    public List<Playlist> getPlaylist() {
        return PlayList;
    }

    public void setPlaylist(List<Playlist> playlist) {
        PlayList = playlist;
    }

    public static Schedule parse(final String input){
        Gson gson = new Gson();
        Schedule playlist = gson.fromJson(input, Schedule.class);
        return playlist;
    }

    /**
     *
     * @param strTime source string to be converted, must conform to the formatType
     * @param formatType source format: yyyy-MM-dd HH:mm:ss or yyyy年MM月dd日HH时mm分ss秒
     * @return converted date
     */
    public Date stringToDate(String strTime, String formatType){
        Date date = null;
        try{
            SimpleDateFormat formatter = new SimpleDateFormat(formatType);
            date = formatter.parse(strTime);
        }catch (Exception e){
        }
        return date;
    }

    public boolean equalsTo(Schedule another) {
        if (StartDate.equals(another.StartDate) && EndDate.equals(another.EndDate) && PlayList.size() == another.PlayList.size()) {
            for (int i=0; i < PlayList.size(); i++) {
                if (!PlayList.get(i).equalsTo(another.PlayList.get(i)))
                    return false;
            }
            return true;
        }
        return false;
    }
}
