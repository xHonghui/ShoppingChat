package com.netease.nim.uikit.business.session.activity;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.laka.androidlib.util.StatusBarUtil;
import com.netease.nim.uikit.R;
import com.netease.nim.uikit.api.model.session.SessionCustomization;
import com.netease.nim.uikit.business.preference.UserPreferences;
import com.netease.nim.uikit.business.session.audio.MessageAudioControl;
import com.netease.nim.uikit.business.session.constant.Extras;
import com.netease.nim.uikit.business.session.fragment.MessageFragment;
import com.netease.nim.uikit.business.session.receiver.CustomDeviceAdminReceiver;
import com.netease.nim.uikit.common.CommonUtil;
import com.netease.nim.uikit.common.ToastHelper;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.util.sys.ScreenUtil;

import java.util.List;

/**
 * Created by zhoujianghua on 2015/9/10.
 */
public abstract class BaseMessageActivity extends UI {

    private static final String TAG = "BaseMessageActivity";
    protected String sessionId;
    protected String anchor;
    private SessionCustomization customization;

    private MessageFragment messageFragment;

    private SensorManager sensorManager;

    private Sensor proximitySensor;
    //息屏亮屏
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    protected abstract MessageFragment fragment();

    protected abstract int getContentViewId();

    protected abstract void initToolBar();

    /**
     * 是否开启距离传感器
     */
    protected abstract boolean enableSensor();

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewId());
        parseIntent();
        initToolBar();
        messageFragment = (MessageFragment) switchContent(fragment());
        if (enableSensor()) {
            initSensor();
        }

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "MyWate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && proximitySensor != null) {
            sensorManager.registerListener(sensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null && proximitySensor != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    public void onBackPressed() {
        if (messageFragment != null && messageFragment.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (messageFragment != null) {
            messageFragment.onActivityResult(requestCode, resultCode, data);
        }

        if (customization != null) {
            customization.onActivityResult(this, requestCode, resultCode, data);
        }
    }

    private void parseIntent() {
        Intent intent = getIntent();
        sessionId = intent.getStringExtra(Extras.EXTRA_ACCOUNT);
        anchor = intent.getStringExtra(Extras.EXTRA_ANCHOR);
        customization = (SessionCustomization) intent.getSerializableExtra(Extras.EXTRA_CUSTOMIZATION);

        if (customization != null) {
            addRightCustomViewOnActionBar(this, customization.buttons);
        }
    }

    // 添加action bar的右侧按钮及响应事件
    private void addRightCustomViewOnActionBar(UI activity, List<SessionCustomization.OptionsButton> buttons) {
        if (CommonUtil.isEmpty(buttons)) {
            return;
        }

        Toolbar toolbar = getToolBar();
        if (toolbar == null) {
            return;
        }

        LinearLayout buttonContainer = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.nim_action_bar_custom_view, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        for (final SessionCustomization.OptionsButton button : buttons) {
            ImageView imageView = new ImageView(activity);
            imageView.setImageResource(button.iconId);
            imageView.setBackgroundResource(R.drawable.nim_nim_action_bar_button_selector);
            imageView.setPadding(ScreenUtil.dip2px(10), 0, ScreenUtil.dip2px(10), 0);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button.onClick(BaseMessageActivity.this, v, sessionId);
                }
            });
            buttonContainer.addView(imageView, params);
        }

        toolbar.addView(buttonContainer, new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER));
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] dis = event.values;
            if (0.0f == dis[0]) {
                //靠近，设置为听筒模式
                MessageAudioControl.getInstance(BaseMessageActivity.this).setEarPhoneModeEnable(true);
                if (MessageAudioControl.getInstance(BaseMessageActivity.this).isPlayingAudio()) {
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire();// 申请设备电源锁
                    }
                }
            } else {
                //离开，复原
                MessageAudioControl.getInstance(BaseMessageActivity.this).setEarPhoneModeEnable(UserPreferences.isEarPhoneModeEnable());
                if (wakeLock.isHeld()) {
                    wakeLock.release(); // 释放设备电源锁
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public SessionCustomization getCustomization() {
        return customization;
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }
}
