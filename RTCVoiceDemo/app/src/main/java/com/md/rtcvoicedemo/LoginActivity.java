package com.md.rtcvoicedemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


import com.baoyz.actionsheet.ActionSheet;
import com.tencent.bugly.crashreport.CrashReport;
import com.wjhd.wy.WJMediaEngine;

import java.util.Objects;


public class LoginActivity extends FragmentActivity implements OnClickListener {
    private static final String TAG = "LoginActivity";

    private EditText etRoom;
    private EditText etUid;

    private Button loginButton;
    private TextView tvVersion;



    private long userId;
    private int domainId;
    private long roomId ;

    private SharedPreferences sharedPreferences;
    private boolean hasWifiConnected = false;

    private EditText etSignalIp;
    private EditText etSignalPort;
    private CheckBox cbDevelopMode;

    private EditText etAppId;
    private Button btnEnvTest;
    private Button btnEnvDev;
    private Button btnEnvRelease;

    private CheckBox cbDefaultRole;




    private String[] envTestAppId = new String[]{"8kJzRafedna967WbqyeyrPCLYsmxi6A1"};
    private String[] envDevAppId = new String[]{};
    private String[] envReleaseAppId = new String[]{};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        init();
    }

    private void init() {
        buglyInit();



        sharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);

        etRoom = (EditText) findViewById(R.id.login_edit_room);
        loginButton = (Button) findViewById(R.id.login_button_login);

        etUid = (EditText) findViewById(R.id.et_uid);

        etSignalIp = (EditText) findViewById(R.id.et_signal_ip);
        etSignalPort = (EditText) findViewById(R.id.et_signal_port);
        cbDevelopMode = (CheckBox) findViewById(R.id.cb_develop_mode);
        tvVersion = (TextView) findViewById(R.id.tv_version);
        tvVersion.setText("version: " + WJMediaEngine.getVersion());
        etSignalIp.setText(sharedPreferences.getString(UserSettingKey.SIGNAL_IP_KEY, "0"));
        etSignalPort.setText(sharedPreferences.getInt(UserSettingKey.SIGNAL_PORT_KEY, 0) + "");
        cbDevelopMode.setChecked(sharedPreferences.getBoolean(UserSettingKey.DEVELOP_MODE, false));

        setClickListener();

        etRoom.setText(Long.toString(sharedPreferences.getLong(UserSettingKey.RoomIdKey, (long)100)) + "");
        etUid.setText(Long.toString(sharedPreferences.getLong(UserSettingKey.UserIdKey, (long)10))+ "");

        //TODO 这里加上权限检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAppPermission();

        }

        etAppId = (EditText) findViewById(R.id.et_app_id);
        etAppId.setText(sharedPreferences.getString(UserSettingKey.APP_ID, envTestAppId[0]));
        btnEnvTest = (Button) findViewById(R.id.btn_env_test);
        btnEnvDev = (Button) findViewById(R.id.btn_env_dev);
        btnEnvRelease = (Button) findViewById(R.id.btn_env_release);

        btnEnvTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionSheet.createBuilder(LoginActivity.this, getSupportFragmentManager())
                        .setCancelButtonTitle("Cancel")
                        .setOtherButtonTitles(envTestAppId)
                        .setCancelableOnTouchOutside(true)
                        .setListener(new ActionSheet.ActionSheetListener() {
                            @Override
                            public void onDismiss(ActionSheet actionSheet, boolean isCancel) {

                            }

                            @Override
                            public void onOtherButtonClick(ActionSheet actionSheet, int index) {
                                etAppId.setText(envTestAppId[index]);
                                btnEnvTest.setTextColor(Color.BLUE);
                                btnEnvDev.setTextColor(Color.BLACK);
                                btnEnvRelease.setTextColor(Color.BLACK);
                                cbDevelopMode.setChecked(false);
                                sharedPreferences.edit().putBoolean(UserSettingKey.DEBUG_MODE, true).commit();
                            }
                        }).show();

            }
        });

        btnEnvDev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionSheet.createBuilder(LoginActivity.this, getSupportFragmentManager())
                        .setCancelButtonTitle("Cancel")
                        .setOtherButtonTitles(envDevAppId)
                        .setCancelableOnTouchOutside(true)
                        .setListener(new ActionSheet.ActionSheetListener() {
                            @Override
                            public void onDismiss(ActionSheet actionSheet, boolean isCancel) {

                            }

                            @Override
                            public void onOtherButtonClick(ActionSheet actionSheet, int index) {
                                etAppId.setText(envDevAppId[index]);
                                btnEnvTest.setTextColor(Color.BLACK);
                                btnEnvDev.setTextColor(Color.WHITE);
                                btnEnvRelease.setTextColor(Color.BLACK);
                                cbDevelopMode.setChecked(true);
                                sharedPreferences.edit().putBoolean(UserSettingKey.DEBUG_MODE, true).commit();
                            }
                        }).show();

            }
        });

        btnEnvRelease.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionSheet.createBuilder(LoginActivity.this, getSupportFragmentManager())
                        .setCancelButtonTitle("Cancel")
                        .setOtherButtonTitles(envReleaseAppId)
                        .setCancelableOnTouchOutside(true)
                        .setListener(new ActionSheet.ActionSheetListener() {
                            @Override
                            public void onDismiss(ActionSheet actionSheet, boolean isCancel) {

                            }

                            @Override
                            public void onOtherButtonClick(ActionSheet actionSheet, int index) {
                                etAppId.setText(envReleaseAppId[index]);
                                btnEnvTest.setTextColor(Color.BLACK);
                                btnEnvDev.setTextColor(Color.BLACK);
                                btnEnvRelease.setTextColor(Color.WHITE);
                                cbDevelopMode.setChecked(false);
                                sharedPreferences.edit().putBoolean(UserSettingKey.DEBUG_MODE, false).commit();
                            }
                        }).show();

            }
        });

        etSignalIp.setEnabled(false);
        etSignalPort.setEnabled(false);
        cbDevelopMode.setEnabled(false);
        btnEnvTest.setEnabled(false);
        btnEnvDev.setEnabled(false);
        btnEnvRelease.setEnabled(false);

        cbDefaultRole = (CheckBox) findViewById(R.id.cb_default_role);
        cbDefaultRole.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

    }

    private void buglyInit() {
        if (!BuildConfig.DEBUG)
        {
            CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this.getApplicationContext());
            strategy.setDeviceModel(Build.MANUFACTURER + " " + Build.MODEL);
            strategy.setAppVersion( WJMediaEngine.getVersion());
            CrashReport.initCrashReport(this.getApplicationContext(), "de33acd898", true, strategy);
        }
    }

    private void checkAppPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);

            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                boolean isPermissionOk = true;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!shouldShowRequestPermissionRationale(permissions[i])) {
                                if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)) {
                                    Toast.makeText(this, "你已经禁止了录音权限提示，必须授予权限才能正常运行,请到应用管理里面打开权限", Toast.LENGTH_SHORT).show();
                                } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                    Toast.makeText(this, "你已经禁止了读写存储权限，必须授予权限才能正常运行，请到应用管理里面打开权限", Toast.LENGTH_SHORT).show();
                                }
                                finish();
                                return;
                            }
                        }
                        isPermissionOk = false;
                    }
                }

                if (isPermissionOk) {
                    Toast.makeText(this, "已经授予权限", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "必须授予权限才能正常运行！！！！", Toast.LENGTH_SHORT).show();
                    checkAppPermission();
                }
                break;
        }
    }

    private void setClickListener() {
        loginButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button_login:
                login();
                break;

        }
    }

    public void login() {

        ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        //移动运营商网络或者WIFI网络连通
        try {
            State state = Objects.requireNonNull(manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)).getState();

            if (State.CONNECTED == state) {
                hasWifiConnected = true;
            }

            if (hasWifiConnected == false) {
                state = Objects.requireNonNull(manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).getState();
                if (State.CONNECTED == state) {
                    hasWifiConnected = true;
                }
            }

            if (hasWifiConnected == false) {
                Toast.makeText(LoginActivity.this, "设置失败，请链接网络", Toast.LENGTH_SHORT).show();
                return;
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        final String room = etRoom.getText().toString().trim();

        if (room.length() == 0) {
            Toast.makeText(this, "请输入直播间ID", Toast.LENGTH_SHORT).show();
            return;
        }

        userId = 100000 + (int) (Math.random() * (999999 - 100000));
        String str = etUid.getText().toString().trim();
        if (!TextUtils.isEmpty(str) && TextUtils.isDigitsOnly(str)) {
            userId = Long.parseLong(str);
        }

        roomId = 1;
        try {
            roomId = Long.parseLong(room);
        } catch (Exception e) {

        }

        String signalIp = etSignalIp.getText().toString();
        int signalPort = 0;
        try {
            signalPort = Integer.parseInt(etSignalPort.getText().toString());
        } catch (Exception e) {

        }
        boolean developMode = cbDevelopMode.isChecked();

        sharedPreferences.edit()
                .putLong(UserSettingKey.RoomIdKey, roomId)
                .putLong(UserSettingKey.UserIdKey, userId)
                .putInt(UserSettingKey.DomainIdKey, domainId)
                .putString(UserSettingKey.SIGNAL_IP_KEY, signalIp)
                .putInt(UserSettingKey.SIGNAL_PORT_KEY, signalPort)
                .putBoolean(UserSettingKey.DEVELOP_MODE, developMode)
                .putString(UserSettingKey.APP_ID, etAppId.getText().toString())
                .putBoolean(UserSettingKey.defaultRoleKey, cbDefaultRole.isChecked())
                .commit();


        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Log.i(TAG, "KEYCODE_BACK");
                moveTaskToBack(true);
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    public static boolean isIpv4(String ipv4) {
        if (ipv4 == null || ipv4.length() == 0) {
            return false;//字符串为空或者空串
        }
        String[] parts = ipv4.split("\\.");//因为java doc里已经说明, split的参数是reg, 即正则表达式, 如果用"|"分割, 则需使用"\\|"
        if (parts.length != 4) {
            return false;//分割开的数组根本就不是4个数字
        }
        for (int i = 0; i < parts.length; i++) {
            try {
                int n = Integer.parseInt(parts[i]);
                if (n < 0 || n > 255) {
                    return false;//数字不在正确范围内
                }
            } catch (NumberFormatException e) {
                return false;//转换数字不正确
            }
        }
        return true;
    }
}
