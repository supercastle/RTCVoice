package com.md.rtcvoicedemo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.wjhd.wy.WJMediaEngine;
import com.wjhd.wy.audio.AudioEngine;
import com.wjhd.wy.audio.AudioEngineHandler;
import com.wjhd.wy.audio.constant.AECMode;
import com.wjhd.wy.audio.constant.AudioRecordQuality;
import com.wjhd.wy.audio.constant.AudioRole;
import com.wjhd.wy.audio.entity.AudioVolumeInfo;
import com.wjhd.wy.http.HttpRequest;
import com.wjhd.wy.http.HttpRespone;
import com.wjhd.wy.http.MyHttp;
import com.wjhd.wy.http.PostBody;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private EditText etStatus;
    private Button btnUpMic;
    private Button btnDownMic;
    private Switch sMute;
    private Switch sPlay;

    private EditText etMap3Path;
    private SeekBar sbMp3;
    private TextView tvMp3CurDuration;
    private TextView tvMp3Duration;
    private Button btnPlayStop;
    private Button btnPauseResume;
    private SeekBar sbMp3Volume;
    private TextView tvMp3Volume;
    private Button btnAudioRecordStart;
    private Button btnAudioRecordStop;
    private Button btnLogUpload;
    private Switch btnDumpFile;
    private Switch btnAutoReplay;
    private Switch swMusicVad;
    private Switch swEnergyVad;
    private Switch swDriveMode;
    private TextView tvMicVol;
    private SeekBar sbMicVol;
    private TextView tvSpeakerVol;
    private SeekBar sbSpeakerVol;
    private Switch swASMRMode;
    private CheckBox cbHardwareAec;
    private Button btnSelectSong;
    private EditText etMsg;
    private Button btnSendMsg;
    private TextView tvBroadcastIndicator;
    private TextView tvAudienceIndicator;
    private Switch swLoopBack;
    private CheckBox cbEnableAec;



    private TextView tvSummary;



    private final int fileSelectCode = 10000;
    private String selectedSongpath;


    private String recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";

    private SharedPreferences sharedPreferences;
    private UserSetting us;

    //日志文件存放路径
    private String logfileDir = "";

    private AudioEngine mAudioEngine;
    private AudioEngineHandler audioEngineHandler;

    private Handler uiHandler = new Handler();
    private boolean isMp3Start = false;
    private boolean isMp3Playing = false;

    private TextView tvDenioseLevel;
    private SeekBar sbDenioseLevel;


    private Map<Long, Queue<Long>> delayMap = new HashMap<>();

    private String timeStampAPI = "http://106.53.214.231:12345/time/unix";


    //test Environment
    private static String appId;

    boolean debugMode = true;

    MyHttp httpClient = new MyHttp();

    boolean isActor = false;

    private Intent serviceIntent;

    private void findViews() {
        etStatus = (EditText) findViewById(R.id.et_status);
        tvBroadcastIndicator = (TextView) findViewById(R.id.tv_broadcast_indicator);
        tvAudienceIndicator = (TextView) findViewById(R.id.tv_audience_indicator);
        tvBroadcastIndicator.setVisibility(View.INVISIBLE);
        tvAudienceIndicator.setVisibility(View.INVISIBLE);
        btnUpMic = (Button) findViewById(R.id.btn_up_mic);
        btnDownMic = (Button) findViewById(R.id.btn_down_mic);
        sMute = (Switch) findViewById(R.id.s_mute);
        sPlay = (Switch) findViewById(R.id.s_play);
        tvSummary = (TextView) findViewById(R.id.tv_summary);
        swLoopBack = (Switch) findViewById(R.id.sw_loop_back);
        cbEnableAec = (CheckBox) findViewById(R.id.cb_enable_aec);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            logfileDir = this.getExternalFilesDir("log").getAbsolutePath();
        }else {
            logfileDir =Environment.getExternalStorageDirectory().getPath() + File.separator + getPackageName() + File.separator + "log";
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // response screen rotation event
        //关闭屏幕旋转
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        setContentView(R.layout.activity_main);
        findViews();


        btnUpMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbEnableAec.setChecked(true);
                mAudioEngine.setRole(AudioRole.CLIENT_ROLE_BROADCASTER);
            }
        });

        btnDownMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioEngine.setRole(AudioRole.CLIENT_ROLE_AUDIENCE);
            }
        });
        sMute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEngine.muteLocalAudioStream(!isChecked);
            }
        });
        sPlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEngine.muteAllRemoteAudioStreams(!isChecked);
            }
        });


        us = new UserSetting();


        sharedPreferences = getSharedPreferences("UserInfo", 0);
        us.UserId = sharedPreferences.getLong(UserSettingKey.UserIdKey, (long)123456);
        us.RoomId = sharedPreferences.getLong(UserSettingKey.RoomIdKey, (long)1);
        appId = sharedPreferences.getString(UserSettingKey.APP_ID, "");
        debugMode = sharedPreferences.getBoolean(UserSettingKey.DEBUG_MODE, true);
        tvSummary.setText("roomID:" + us.RoomId + ",uid:" + us.UserId + ", appId:" + appId );

        isActor = sharedPreferences.getBoolean(UserSettingKey.defaultRoleKey, false);
        logToPhone("登陆房间："+ us.RoomId );
        logToPhone("uid："+ us.UserId);

        doEnter();



        etMap3Path = (EditText) findViewById(R.id.et_map3_path);
        sbMp3 = (SeekBar) findViewById(R.id.sb_mp3);
        tvMp3CurDuration = (TextView) findViewById(R.id.tv_mp3_cur_duration);
        tvMp3Duration = (TextView) findViewById(R.id.tv_mp3_duration);
        btnPlayStop = (Button) findViewById(R.id.btn_play_stop);
        btnPauseResume = (Button) findViewById(R.id.btn_pause_resume);
        sbMp3Volume = (SeekBar) findViewById(R.id.sb_mp3_volume);
        tvMp3Volume = (TextView) findViewById(R.id.tv_mp3_volume);
        btnPlayStop.setText("播放");
        btnPauseResume.setText("--");

        String songPath = sharedPreferences.getString(UserSettingKey.BackgroudMusicKey,"");
        if(!songPath.isEmpty()){
            etMap3Path.setText(songPath);
        }
        btnSelectSong = (Button) findViewById(R.id.btn_select_song);
        btnSelectSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               FileExploreActivity.start(MainActivity.this, fileSelectCode);

            }
        });

        sbMp3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                mAudioEngine.setAudioMixingPosition(progress);
                long curLength = mAudioEngine.getAudioMixingCurrentPosition();
                tvMp3CurDuration.setText(curLength / 60000 +":" + curLength % 60000 / 1000);
            }
        });
        btnPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMp3Start) {
                    String str = etMap3Path.getText().toString().trim();
                    if (TextUtils.isEmpty(str)) {
                        return;
                    }
                    mAudioEngine.startAudioMixing(str);
                    isMp3Start = true;
                    isMp3Playing = true;
                    sbMp3.setProgress(0);
                    long totalLength = mAudioEngine.getAudioMixingDuration();
                    sbMp3.setMax((int) totalLength);
                    tvMp3CurDuration.setText("00:00");
                    tvMp3Duration.setText(totalLength / 60000 +":" + totalLength % 60000 / 1000);
                    map3ProgressUpdate();
                    btnPlayStop.setText("停止");
                    btnPauseResume.setText("暂停");
                }else {
                    mAudioEngine.stopAudioMixing();
                    isMp3Start = false;
                    sbMp3.setProgress(0);
                    btnPlayStop.setText("播放");
                    btnPauseResume.setText("--");
                }
            }
        });

        btnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isMp3Start){
                    return;
                }
                if (!isMp3Playing){
                    mAudioEngine.resumeAudioMixing();
                    isMp3Playing = true;
                    map3ProgressUpdate();
                    btnPauseResume.setText("暂停");
                }else {
                    mAudioEngine.pauseAudioMixing();
                    isMp3Playing = false;
                    btnPauseResume.setText("恢复");
                }

            }
        });
        sbMp3Volume.setMax(100);
        sbMp3Volume.setProgress(100);
        tvMp3Volume.setText("音量:100");
        sbMp3Volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMp3Volume.setText("音量:"+ progress );
                if (fromUser) {
                    mAudioEngine.adjustAudioMixingVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnAutoReplay = (Switch) findViewById(R.id.btn_auto_replay);
        btnAutoReplay.setChecked(true);
        btnAutoReplay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });




        final String aurdioRecordPath = "/sdcard/audio_record";
        btnAudioRecordStart = (Button) findViewById(R.id.btn_audio_record_start);
        btnAudioRecordStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioEngine.startAudioRecording(aurdioRecordPath, AudioRecordQuality.HighQualityLowCompression);
            }
        });
        btnAudioRecordStop = (Button) findViewById(R.id.btn_audio_record_stop);
        btnAudioRecordStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioEngine.stopAudioRecording();
            }
        });


        btnLogUpload = (Button) findViewById(R.id.btn_log_upload);
        btnLogUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                logToPhone("上传日志" );
            }
        });

        btnDumpFile = (Switch) findViewById(R.id.btn_dump_file);
        btnDumpFile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    logToPhone("start dumping files ");
                    mAudioEngine.setParams("dumpFile", "true");
                }else {
                    logToPhone("stop dumping files ");
                    mAudioEngine.setParams("dumpFile", "");
                }
            }
        });

        swMusicVad = (Switch) findViewById(R.id.sw_music_vad);
        boolean defaultMusvadValue = false;
        swMusicVad.setChecked(defaultMusvadValue);
        mAudioEngine.setAudioMixingVAD(defaultMusvadValue);
        swMusicVad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEngine.setAudioMixingVAD(isChecked);
            }
        });
        swEnergyVad = (Switch) findViewById(R.id.sw_energy_vad);
        boolean defaultEnevadValue = false;
        swEnergyVad.setChecked(defaultEnevadValue);
        mAudioEngine.setEnergyVadFlag(defaultEnevadValue);
        swEnergyVad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEngine.setEnergyVadFlag(isChecked);
            }
        });

        swDriveMode = (Switch) findViewById(R.id.sw_drive_mode);
        boolean defaultDriveMode = false;
        swMusicVad.setChecked(defaultDriveMode);
        mAudioEngine.setDriveMode(0);
        swDriveMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mAudioEngine.setDriveMode(1);
                }else {
                    mAudioEngine.setDriveMode(0);
                }
            }
        });

        tvMicVol = (TextView) findViewById(R.id.tv_mic_vol);
        tvMicVol.setText("软件麦克风音量：" + 100);
        sbMicVol = (SeekBar) findViewById(R.id.sb_mic_vol);
        sbMicVol.setMax(1000);
        sbMicVol.setProgress(100);
        sbMicVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMicVol.setText("软件麦克风音量：" + progress);
                if (fromUser) {
                    mAudioEngine.adjustRecordingSignalVolume(progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tvSpeakerVol = (TextView) findViewById(R.id.tv_speaker_vol);
        tvSpeakerVol.setText("软件扬声器音量：" + 100);
        sbSpeakerVol = (SeekBar) findViewById(R.id.sb_speaker_vol);
        sbSpeakerVol.setMax(1000);
        sbSpeakerVol.setProgress(100);
        sbSpeakerVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeakerVol.setText("软件扬声器音量：" + progress);
                if (fromUser) {
                    mAudioEngine.adjustSpeakerVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        tvDenioseLevel = (TextView) findViewById(R.id.tv_deniose_level);
        tvDenioseLevel.setText("降噪等级："+ 75 );
        sbDenioseLevel = (SeekBar) findViewById(R.id.sb_deniose_level);
        sbDenioseLevel.setMax(100);
        sbDenioseLevel.setProgress(75);
        sbDenioseLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvDenioseLevel.setText("降噪等级："+ progress );
                if (fromUser) {
                    mAudioEngine.setDenioseLevel(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        swASMRMode = (Switch) findViewById(R.id.sw_ASMR_mode);
        swASMRMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sbDenioseLevel.setEnabled(!isChecked);
                mAudioEngine.setDenioseLevel(isChecked ? 0:75);
                mAudioEngine.setAudioEncodeBitRate(isChecked? 192000:128000);
                int rate = mAudioEngine.getAudioEncodeBitRate();
                etStatus.append("当前码率：" + rate);
                etStatus.append("\n");

            }
        });

        cbHardwareAec = (CheckBox) findViewById(R.id.cb_hardware_aec);
        cbHardwareAec.setChecked(false);
        cbHardwareAec.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mAudioEngine.setAECMode(AECMode.MODE_HARDWARE);
                }else {
                    mAudioEngine.setAECMode(AECMode.MODE_SOFTWARE);
                }
            }
        });

        cbEnableAec.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEngine.setAEC(isChecked);
            }
        });

        etMsg = (EditText) findViewById(R.id.et_msg);
        btnSendMsg = (Button) findViewById(R.id.btn_send_msg);

        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String str = etMsg.getText().toString();
                if (TextUtils.isEmpty(str)){
                    Toast.makeText(MainActivity.this, "请输入消息！", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpRequest request = new HttpRequest(new PostBody(""));
                        try {
                            long before = System.currentTimeMillis();
                            HttpRespone response = httpClient.Get(timeStampAPI, request);
                            long after = System.currentTimeMillis();
                            CustomMsg customMsg = new CustomMsg();
                            customMsg.setUid(us.UserId);
                            customMsg.setSendTime(Long.parseLong(response.getData()) - (after - before));
                            customMsg.setMsg(str);
                            mAudioEngine.setParams("enhancedMessageFrame",  new Gson().toJson(customMsg));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

//                CustomMsg customMsg = new CustomMsg();
//                customMsg.setUid(us.UserId);
//                customMsg.setSendTime(System.currentTimeMillis());
//                customMsg.setMsg(str);
//
//                mAudioEngine.setParams("enhancedMessageFrame",  new Gson().toJson(customMsg));
                etMsg.setText("");
            }
        });

        // 前台通知，保活
        serviceIntent = new Intent(MainActivity.this, ForegroundService.class);
        ContextCompat.startForegroundService(MainActivity.this, serviceIntent);


        swLoopBack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEngine.setLoopBack(isChecked);
            }
        });

    }



    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private void logToPhone(String msg){
        if (etStatus.getText().length() > 600){
            etStatus.getText().delete(0,400);
        }
        etStatus.append(simpleDateFormat.format(new Date(System.currentTimeMillis())) + "  ");
        etStatus.append(msg);
        etStatus.append("\n");
    }

    private void map3ProgressUpdate(){
        if (isMp3Start && isMp3Playing) {
            sbMp3.setProgress((int) mAudioEngine.getAudioMixingCurrentPosition());
            long curLength = mAudioEngine.getAudioMixingCurrentPosition();
            tvMp3CurDuration.setText(curLength / 60000 +":" + curLength % 60000 / 1000);
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    map3ProgressUpdate();
                }
            }, 1000);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                return true;
            } else if (id == R.id.action_userSettings) {
                // popu user serttig activity
                Intent intent = new Intent(MainActivity.this, UserSettingActivity.class);
                startActivity(intent);
            }
            setTitle(item.getTitle());


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == fileSelectCode) {
                selectedSongpath = data.getStringExtra(FileExploreActivity.file_path);
                sharedPreferences.edit().putString(UserSettingKey.BackgroudMusicKey, selectedSongpath);
                etMap3Path.setText(selectedSongpath);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler = null;
        doExit();
        stopService(serviceIntent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }


    class AudioHandler extends AudioEngineHandler{
        @Override
        public void onError(final int err) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("出错了！！ error code：" + err + " ");
                }
            });
        }

        @Override
        public void onErrorMsg(final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("error message：" + msg );
                }
            });
        }

        @Override
        public void onClientRoleChanged(AudioRole oldRole, final AudioRole newRole) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newRole.equals(AudioRole.CLIENT_ROLE_AUDIENCE)){
                        logToPhone("当前角色：听众 ");
                        tvBroadcastIndicator.setVisibility(View.INVISIBLE);
                        tvAudienceIndicator.setVisibility(View.VISIBLE);
                    }else if (newRole.equals(AudioRole.CLIENT_ROLE_BROADCASTER)){
                        logToPhone("当前角色：主播 ");
                        tvBroadcastIndicator.setVisibility(View.VISIBLE);
                        tvAudienceIndicator.setVisibility(View.INVISIBLE);
                    }
                }
            });

        }

        @Override
        public void onJoinChannelSuccess(long channel, long uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("加入频道成功！！ ");
                }
            });

        }

        @Override
        public void onLeaveChannel() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("离开频道成功！！ ");
                }
            });
        }

        @Override
        public void onLastmileQuality(int quality) {
            if (100 > quality && quality > 70){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logToPhone("当前网络质量较差！ ");
                    }
                });
            }
        }

        @Override
        public void onConnectionLost() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    logToPhone("链接丢失！！ ");
//                }
//            });
        }

        @Override
        public void onUserMuteAudio(final long uid, final boolean muted) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (muted){
                        logToPhone("用户uid:" + uid + "关闭了麦克风 ");
                    }else {
                        logToPhone("用户uid:" + uid + "打开了麦克风 ");
                    }

                }
            });
        }
        
        @Override
        public void onAudioVolumeIndication(final AudioVolumeInfo[] speakers) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (speakers.length == 0){
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (AudioVolumeInfo speaker : speakers) {
                        sb.append(speaker.getUid()).append(":").append(speaker.getVolume()).append(",");
                    }
                    if (sb.length() > 0) {
                        sb.deleteCharAt(sb.lastIndexOf(","));
                    }
                    logToPhone("正在说话 uid:[" + sb.toString() + "] ");
                }
            });

        }

        @Override
        public void onAudioMixingFinished() {
            super.onAudioMixingFinished();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isMp3Start = false;
                    isMp3Playing = false;
                    btnPlayStop.setText("播放");
                    btnPauseResume.setText("--");
                    sbMp3.setProgress(0);
                    logToPhone("一首歌播放完成！");

                    if (btnAutoReplay.isChecked()){
                        String str = etMap3Path.getText().toString().trim();
                        if (TextUtils.isEmpty(str)) {
                            return;
                        }
                        mAudioEngine.startAudioMixing(str);
                        isMp3Start = true;
                        isMp3Playing = true;
                        sbMp3.setProgress(0);
                        long totalLength = mAudioEngine.getAudioMixingDuration();
                        sbMp3.setMax((int) totalLength);
                        tvMp3CurDuration.setText("00:00");
                        tvMp3Duration.setText(totalLength / 60000 +":" + totalLength % 60000 / 1000);
                        map3ProgressUpdate();
                        btnPlayStop.setText("停止");
                        btnPauseResume.setText("暂停");
                    }


                }
            });

        }

        @Override
        public void onBeKicked(final int code, final String reason) {
            super.onBeKicked(code, reason);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("被服务器踢出code: " + code + " reason:" + reason );
                    logToPhone("请退出！！");
                }
            });
        }

        @Override
        public void onReceivePhoneCall(final boolean inInCall) {
            super.onReceivePhoneCall(inInCall);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("电话 "+ (inInCall ? "进来":"结束") );
                }
            });
        }

        @Override
        public void onOfflineTimeOut(final long channel, final long uid) {
            super.onOfflineTimeOut(channel, uid);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone(" 断线超过服务器的信息保存时间，用户信息已经被服务器清理掉了,channel："+ channel + " uid："+ uid );
                    showChoseInRoomDialog();

                }
            });
        }

        @Override
        public void OnMicUniqueIDUpdate(final String micUniqueID){
            super.OnMicUniqueIDUpdate(micUniqueID);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("收到上麦micUniqueID:  " + micUniqueID);
                }
            });

        }

        @Override
        public void OnKickedToAudience(){
            super.OnKickedToAudience();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("被服务器踢出成观众  ");
                }
            });

        }

        @Override
        public void onReceiveAudioEnhancedMessage(final List<byte[]> msgList) {
            super.onReceiveAudioEnhancedMessage(msgList);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logToPhone("收到增强信息：\n  ");
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {

                    final StringBuffer sb = new StringBuffer();
                    for (byte[] bytes : msgList) {
                        String str = new String(bytes);
                        CustomMsg customMsg = new Gson().fromJson(str, CustomMsg.class);
                        ArrayBlockingQueue<Long> queue;
                        if (!delayMap.containsKey(customMsg.getUid())){
                            queue = new ArrayBlockingQueue<>(10, true);
                            delayMap.put(customMsg.getUid(),queue);
                        }else {
                            queue = (ArrayBlockingQueue<Long>) delayMap.get(customMsg.getUid());
                        }
                        if ( queue.size() >= 10){
                            try {
                                queue.take();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        long delayMs = -1;

                        HttpRequest request = new HttpRequest(new PostBody(""));
                        try {
                            long before = System.currentTimeMillis();
                            HttpRespone respone = httpClient.Get(timeStampAPI, request);
                            long after = System.currentTimeMillis();
                            delayMs = Long.parseLong(respone.getData()) - (after - before) - customMsg.getSendTime();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        try {
                            queue.put(delayMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int len = queue.size();
                        Long tmp[] =new Long[len];
                        queue.toArray(tmp);
                        long sum = 0;
                        for (int i = 0; i < len; i++) {
                            sum += tmp[i];
                        }
                        long avg = sum / len;
                        sb.append("uid:" + customMsg.getUid() + ",msg:" + customMsg.getMsg() + ",send time:" + simpleDateFormat.format(new Date(customMsg.getSendTime()))
                                + "\n,delay(ms):" + delayMs + ",avg delay(ms):" + avg).append("\n");
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logToPhone(sb.toString());
                        }
                    });
                }
            }).start();


//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    final StringBuffer sb = new StringBuffer();
//                    for (byte[] bytes : msgList) {
//                        String str = new String(bytes);
//                        CustomMsg customMsg = new Gson().fromJson(str, CustomMsg.class);
//                        ArrayBlockingQueue<Long> queue;
//                        if (!delayMap.containsKey(customMsg.getUid())){
//                            queue = new ArrayBlockingQueue<>(10, true);
//                            delayMap.put(customMsg.getUid(),queue);
//                        }else {
//                            queue = (ArrayBlockingQueue<Long>) delayMap.get(customMsg.getUid());
//                        }
//                        if ( queue.size() >= 10){
//                            try {
//                                queue.take();
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        long delayMs = System.currentTimeMillis() - customMsg.getSendTime();
//                        try {
//                            queue.put(delayMs);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        int len = queue.size();
//                        Long tmp[] =new Long[len];
//                        queue.toArray(tmp);
//                        long sum = 0;
//                        for (int i = 0; i < len; i++) {
//                            sum += tmp[i];
//                        }
//                        long avg = sum / len;
//                        sb.append("uid:" + customMsg.getUid() + ",msg:" + customMsg.getMsg() + ",send time:" + simpleDateFormat.format(new Date(customMsg.getSendTime()))
//                                + "\n,delay(ms):" + delayMs + ",avg delay(ms):" + avg).append("\n");
//                    }
//                    logToPhone(sb.toString());
//                }
//            });


        }
    }

    private void showChoseInRoomDialog() {
        new AlertDialog.Builder(this)
                .setMessage("是否重新进房？")
                .setPositiveButton("重新进房", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enterChannel(us.RoomId, us.UserId);
                    }
                })
                .setNegativeButton("退出", new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
        .show();
    }


    private void enterChannel(long channelId, long uid) {
        if (audioEngineHandler == null) {
            audioEngineHandler = new AudioHandler();
        }

        if (mAudioEngine == null) {
            try {

                String signalIp = sharedPreferences.getString(UserSettingKey.SIGNAL_IP_KEY, "");
                if(!signalIp.isEmpty()){
                    AudioEngine.setSignalIp(signalIp);
                }
                int signalPort = sharedPreferences.getInt(UserSettingKey.SIGNAL_PORT_KEY, 0);
                if(signalPort != 0){
                    AudioEngine.setSignalPort(signalPort);
                }
                boolean developMode = sharedPreferences.getBoolean(UserSettingKey.DEVELOP_MODE,false);
                AudioEngine.setDevelopMode(developMode);

                AudioEngine.setRecordingAudioFrameParameters(44100, 2);
                AudioEngine.setLogFileDir(logfileDir);
                AudioEngine.setDebugMode(debugMode);//设置是否调试模式，底层默认是false
                //设置连接域名及ip，不设置默认为原来国内环境的值
                final String proEnvDomain = "ws-media-sdk.wujiemm.com";
                final String proEnvBackupIp ="42.194.153.131";
                final String betaEnvDomain = "beta-ws-media-sdk.wujiemm.com";
                final String betaEnvBackupIp = "1.14.168.202";

                //AudioEngine.setConDomainIP(proEnvDomain,proEnvBackupIp,betaEnvDomain,betaEnvBackupIp);

                mAudioEngine = WJMediaEngine.createAudioEngine(getApplicationContext(), appId, audioEngineHandler);

            } catch (Exception e) {
                throw new RuntimeException("NEED TO check  sdk init fatal error" + Log.getStackTraceString(e));
            }
            mAudioEngine.setDefaultAudioRoutetoSpeakerphone(true);
            // 回调谁在说话
            mAudioEngine.enableAudioVolumeIndication(400, 0);
//            mAudioEngine.setParams("dumpFile","true");
            //mAudioEngine.setAudioMixingVAD(true);
            //mAudioEngine.setEnergyVadFlag(true);

        }
        //创建并加入频道
        mAudioEngine.joinChannel(channelId, uid, "demo-"+uid);
    }


    //登录服务器
    private int doEnter(){
        enterChannel(us.RoomId, us.UserId);
        mAudioEngine.setRole(isActor ? AudioRole.CLIENT_ROLE_BROADCASTER : AudioRole.CLIENT_ROLE_AUDIENCE);
        return 1;
    }

    //下线服务器并停止音视频
    protected void doExit(){
        mAudioEngine.leaveChannel();
        WJMediaEngine.destroyAudioEngine();
    }



}
