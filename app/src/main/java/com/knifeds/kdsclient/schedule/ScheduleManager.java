package com.knifeds.kdsclient.schedule;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.knifeds.kdsclient.data.Consts;
import com.knifeds.kdsclient.data.DataContext;
import com.knifeds.kdsclient.data.Playlist;
import com.knifeds.kdsclient.data.Schedule;
import com.knifeds.kdsclient.utils.StayForTrigger;
import com.knifeds.kdsclient.utils.TimeUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScheduleManager implements Observer {
    private static final String TAG = "ScheduleManager";
    private static final int SCHEDULE_TYPE_CONDITION = 2;

    @Inject
    DataContext dataContext;

    @Inject
    public ScheduleManager() {}

    public void executeCurrentSchedule() {
        checkSchedule(null);
    }

    public void executeUpdatedSchedule(Schedule schedule) {
        checkSchedule(schedule);
    }

    public void provisionNextPlaylist() {
        List<Schedule> scheduleList = dataContext.getScheduleList();
        if (scheduleList.get(0).Type != SCHEDULE_TYPE_CONDITION) {
            traversePlaylist();
        }
    }

    public void executeWithCondition(final String condition) {
        List<Schedule> scheduleList = dataContext.getScheduleList();
        for(Schedule schedule : scheduleList) {
            for (Playlist playlist : schedule.PlayList) {
                if (playlist.Condition != null && playlist.Condition.equals(condition)) {
                    postMessageDelayed(new TimerMessage(Consts.TIMER_SCHEDULER, playlist), 10);

                    // Fixme: should wait for playlist to finish
                    StayForTrigger.getInstance().reset();
                    return;
                }
            }
        }
    }

    // Implementation
    private Playlist findIdlePlaylist(Schedule schedule) {
        if (schedule != null) {
            for (Playlist playlist : schedule.PlayList) {
                if (playlist.PlayListType == 0) {
                    return playlist;
                }
            }
        }

        return dataContext.getIdlePlaylist();
    }

    private void checkSchedule(Schedule newSchedule) {
        try{
            List<Schedule> scheduleList = dataContext.getScheduleList();

            removeOutOfDateSchedules(scheduleList);

            if (newSchedule != null) {
                if (newSchedule.PlayList.get(0).isIdlePlaylist()) {
                    // We save idlePlaylist, but will scheduleCurrentPlaylist it as a normal playlist, only when the list is empty, we will switch to the idlePlaylist
                    dataContext.setIdlePlaylist(newSchedule.PlayList.get(0));
                }

                // FIXME: We require that each client has ONLY ONE schedule.
                scheduleList.clear();

                boolean found = false;
                /*
                for(int i = scheduleList.size()-1;i>=0;i--) {
                    if(scheduleList.get(i).equalsTo(schedule)) {
                        found = true;
                    }
                }*/
                if (!found) {
                    scheduleList.add(newSchedule);
                }
            }

            sortSchedulesAndPlaylists(scheduleList);

//            dataContext.saveScheduleList();   // Don't save yet

            prepareAllPlaylists();
        } catch (Exception ex) {
            Log.e(TAG, "checkSchedule: " + ex.getMessage());
        }
    }

    private void sortSchedulesAndPlaylists(List<Schedule> scheduleList) {
        Collections.sort(scheduleList, new DsSchedulerComprator());
        for(int i = 0;i < scheduleList.size(); i++){
            Collections.sort(scheduleList.get(i).getPlaylist(), new DsPlaylistComprator());
        }
    }

    private void removeOutOfDateSchedules(List<Schedule> scheduleList) {
        for(int i = scheduleList.size()-1; i >= 0; i--){
            Schedule schedule = scheduleList.get(i);
            if (schedule.Type == SCHEDULE_TYPE_CONDITION) {
                // Schedule with conditions, Do not remove it.
                continue;
            }

            if(TimeUtil.afterSchedulerDate(scheduleList.get(i).getEndDate())){
                scheduleList.remove(i);
            }
        }

        if (scheduleList.size() == 0) {
            Log.d(TAG, "removeOutOfDateSchedules: Schedule is empty after removing all out-of-date schedules.");
        }
    }

    private void postMessageDelayed(final TimerMessage msg, final long delay) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(msg);
            }
        }, delay);
    }

    private String getStartDateTime(String date, String time) {
        // TODO: replace T00:00:00 with time + ":00"
        return date.substring(0, date.lastIndexOf("T")) + " " + time + ":00";
    }

    private void prepareAllPlaylists() {
        if (dataContext.getScheduleList().size() == 0)
            return;

        Schedule schedule = dataContext.getScheduleList().get(0); // Assume there is only one schedule per device

        preparingPlaylist.clear();
//        HashSet<String> idSet = new HashSet<>();

//        for(int i = 0; i < schedule.PlayList.size(); i++) {
//            if (!idSet.contains(schedule.PlayList.get(i).PlayListId)) {
//                idSet.add(schedule.PlayList.get(i).PlayListId);
//                preparingPlaylist.add(schedule.PlayList.get(i));
//            }
//        }

        for(int i = 0; i < schedule.PlayList.size(); i++) {
            preparingPlaylist.add(schedule.PlayList.get(i));
        }

        if (preparingPlaylist.size() > 0) {
            preparingPlaylistIndex = 0;
            preparePlaylist(preparingPlaylistIndex);
        }
    }

    private void preparePlaylist(int playlistIndex) {
        Playlist playlist = preparingPlaylist.get(playlistIndex);
        playlist.addObserver(this);
        Playlist.dataContext = dataContext;
        playlist.prepareContent();
    }

    private void startSchedule() {
        if(timer != null){
            timer.cancel();
            timer = null;
        }

        List<Schedule> schedulerList = dataContext.getScheduleList();
        if (schedulerList.get(0).Type == SCHEDULE_TYPE_CONDITION) {
            // Find the idle playlist and play it
            Playlist idlePlaylist = findIdlePlaylist(schedulerList.get(0));
            if (idlePlaylist != null) {
                postMessageDelayed(new TimerMessage(Consts.TIMER_SCHEDULER, idlePlaylist), 10);
            }
        } else {
            currentScheduleIndex = -1;
            currentPlaylistIndex = -1;
        }

        traversePlaylist();
    }

    private void traversePlaylist() {
        boolean hasMorePlaylist = advanceCurrentIndex();

        if (hasMorePlaylist) {
            // schedule currentPlaylist
            List<Schedule> schedulerList = dataContext.getScheduleList();
            Playlist playlist = schedulerList.get(currentScheduleIndex).getPlaylist().get(currentPlaylistIndex);

            long delay = TimeUtil.beforeSchedulerTimeMill(getStartDateTime(schedulerList.get(currentScheduleIndex).getStartDate(), playlist.getStartTime()));
            if (delay <= 0){
                delay = 10;
            }
            Log.d(TAG, "traversePlaylist: Playlist id=" + playlist.PlayListId + " scheduled with delay: " + delay / 1000 + "(s)");
            postMessageDelayed(new TimerMessage(Consts.TIMER_SCHEDULER, playlist), delay);
        } else {
            // Find the idle playlist and play it
            Playlist idlePlaylist = findIdlePlaylist(null);
            if (idlePlaylist != null) {
                postMessageDelayed(new TimerMessage(Consts.TIMER_SCHEDULER, idlePlaylist), 10);
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        int tag = (int)arg;
        switch(tag) {
            case Consts.O_PREPARE_PLAYLIST:
                if (++preparingPlaylistIndex < preparingPlaylist.size()) {
//                    Logger.d("Prepare next playlist...");
                    preparePlaylist(preparingPlaylistIndex);
                } else {
                    Log.d(TAG, "update: All playlists have been prepared. Save it to local storage and start running...");
                    dataContext.saveScheduleList();
                    startSchedule();
                }
                break;
        }
    }

    class DsSchedulerComprator implements Comparator<Schedule> {
        @Override
        public int compare(Schedule schedule, Schedule schedule1) {
            if((schedule.getFormatStartdate().before(schedule1.getFormatStartdate()))
                    ||(schedule.getFormatStartdate().equals(schedule1.getFormatStartdate()))){
                return -1;
            }
            return 1;
        }
    }

    class DsPlaylistComprator implements Comparator<Playlist> {
        @Override
        public int compare(Playlist dsPlaylist, Playlist dsPlaylist1) {
//            if(currentPlaylist.getFormatStarttime().getTime() <= dsPlaylist1.getFormatStarttime().getTime()){
//                return -1;
//            }else {
//                return 1;
//            }
            // Sort by StartTime ascending
            if(dsPlaylist.getFormatStarttime()<=(dsPlaylist1.getFormatStarttime())){
                return -1;
            }
            return 1;
        }
    }

    private boolean advanceCurrentIndex(){
        List<Schedule> scheduleList = dataContext.getScheduleList();

        int tempSchedulerIndex = 0;
        int tempPlaylistIndex = 0;

        if (currentScheduleIndex != -1) {
            if(scheduleList.get(currentScheduleIndex).getPlaylist().size() == currentPlaylistIndex + 1){
                if(scheduleList.size() == currentScheduleIndex + 1){
                    currentScheduleIndex = -1;
                    currentPlaylistIndex = -1;
                    return false;
                }else{
                    currentScheduleIndex = currentScheduleIndex + 1;
                }
            }else{
                currentPlaylistIndex = currentPlaylistIndex + 1;
            }
            tempSchedulerIndex = currentScheduleIndex;
            tempPlaylistIndex = currentPlaylistIndex;
        }

        for (int i = tempSchedulerIndex;i < scheduleList.size(); i++) {
            Schedule schedule = scheduleList.get(i);
            if (TimeUtil.beforeSchedulerDate(schedule.getEndDate())) {
                for (int j = tempPlaylistIndex; j < schedule.getPlaylist().size(); j++) {
                    Playlist dsPlaylist = schedule.getPlaylist().get(j);
                    if (TimeUtil.beforeSchedulerEndTime(dsPlaylist.getEndTime())) {
                        currentScheduleIndex = i;
                        currentPlaylistIndex = j;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int currentScheduleIndex = -1;
    private int currentPlaylistIndex = -1;
    private int preparingPlaylistIndex = -1;

    private Playlist idlePlaylist;
    private Timer timer;
    private List<Playlist> preparingPlaylist = new ArrayList<>();
}

