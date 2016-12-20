package remix.myplayer.ui.dialog;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.service.TimerService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.customview.CircleSeekBar;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 定时关闭界面
 */
public class TimerDialog extends BaseDialogActivity {
    //提示信息
    @BindView(R.id.timer_info_container)
    View mInfoContainer;
    @BindView(R.id.timer_content_container)
    View mContentContainer;
    //分钟
    @BindView(R.id.minute)
    TextView mMinute;
    //秒
    @BindView(R.id.second)
    TextView mSecond;
    //设置或取消默认
    SwitchCompat mSwitch;
    //圆形seekbar
    @BindView(R.id.close_seekbar)
    CircleSeekBar mSeekbar;
    //开始或取消计时
    @BindView(R.id.close_toggle)
    TextView mToggle;
    @BindView(R.id.close_stop)
    TextView mCancel;

    //是否正在计时
    public static boolean mIsTiming = false;
    //是否正在运行
    public static boolean mIsRunning = false;
    //定时时间
    private static long mTime;
    //更新seekbar与剩余时间
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.getData() != null){
                mMinute.setText(msg.getData().getString("Minute"));
                mSecond.setText(msg.getData().getString("Second"));
            }
            mSeekbar.setProgress(msg.arg1);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"Timer");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_timer);
        ButterKnife.bind(this);

        //居中显示
        Window w = getWindow();
        final WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        w.setAttributes(lp);
        w.setGravity(Gravity.CENTER);

        //如果正在计时，设置seekbar的进度
        if(mIsTiming) {
            int remain = (int)mTime * 60 - (int)(System.currentTimeMillis() - TimerService.mStartTime) / 1000;
            mSeekbar.setProgress(remain / 60);
            mSeekbar.setStart(true);
        }

        mSeekbar.setOnSeekBarChangeListener(new CircleSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircleSeekBar seekBar, long progress, boolean fromUser) {
                if (progress > 0) {
                    //记录倒计时时间和更新界面
                    mMinute.setText(progress < 10 ? "0" + progress : "" + progress);
                    mSecond.setText("00");
                    mTime = progress;
                }
            }
            @Override
            public void onStartTrackingTouch(CircleSeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(CircleSeekBar seekBar) {
            }
        });

        //初始化switch
        mSwitch = new SwitchCompat(new ContextThemeWrapper(this, ThemeStore.isDay() ? Theme.getTheme() : R.style.TimerDialogNightTheme));
        ((LinearLayout)findView(R.id.popup_timer_container)).addView(mSwitch);

        //读取保存的配置
        boolean hasdefault = SPUtil.getValue(this, "Setting", "TimerDefault", false);
        final int time = SPUtil.getValue(this,"Setting","TimerNum",-1);

        //默认选项
        if(hasdefault && time > 0){
            //如果有默认设置并且没有开始计时，直接开始计时
            //如果有默认设置但已经开始计时，打开该popupwindow,并更改switch外观
            if(!mIsTiming) {
                mTime = time;
                Toggle();
            }
        }
        mSwitch.setChecked(hasdefault);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (mTime > 0) {
                        ToastUtil.show(TimerDialog.this,R.string.set_success);
                        SPUtil.putValue(TimerDialog.this, "Setting", "TimerDefault", true);
                        SPUtil.putValue(TimerDialog.this, "Setting", "TimerNum", (int) mTime);
                    } else {
                        ToastUtil.show(TimerDialog.this,R.string.plz_set_correct_time);
                        mSwitch.setChecked(false);
                    }
                } else {
                    ToastUtil.show(TimerDialog.this,R.string.cancel_success);
                    SPUtil.putValue(TimerDialog.this, "Setting", "TimerDefault", false);
                    SPUtil.putValue(TimerDialog.this, "Setting", "TimerNum", -1);
                }
            }
        });


        mToggle.setText(mIsTiming ? "取消计时" : "开始计时");

        //分钟 秒 背景框
        final Drawable containerDrawable = Theme.getShape(
                GradientDrawable.RECTANGLE,
                Color.TRANSPARENT,
                DensityUtil.dip2px(this,1),
                DensityUtil.dip2px(this,1),
                ColorUtil.getColor(R.color.gray_404040),
                0,0,1);
        ButterKnife.apply(new View[]{findView(R.id.timer_minute_container),findView(R.id.timer_second_container)},
                new ButterKnife.Action<View>() {
                    @Override
                    public void apply(@NonNull View view, int index) {
                        view.setBackground(containerDrawable);
                    }
                });

        //点击效果
//        ButterKnife.apply(new View[]{mCancel,mToggle},
//                new ButterKnife.Action<View>() {
//                    @Override
//                    public void apply(@NonNull View view, int index) {
//                        Drawable selectDrawable = getResources().getDrawable(R.drawable.bg_corner_select_day);
//                        Drawable defaultDrawable = getResources().getDrawable(R.drawable.bg_corner_default_day);
//                        view.setBackground(Theme.getPressDrawable(defaultDrawable,selectDrawable,ColorUtil.getColor(R.color.day_ripple_color),defaultDrawable,defaultDrawable));
//                    }
//                });

    }

    /**
     * 根据是否已经开始计时来取消或开始计时
     */
    private void Toggle(){
        if(mTime <= 0 && !mIsTiming) {
            ToastUtil.show(TimerDialog.this,R.string.plz_set_correct_time);
            return;
        }
        String msg = mIsTiming ? "取消定时关闭" : "将在" + mTime + "分钟后关闭";
        ToastUtil.show(this,msg);
        mIsTiming = !mIsTiming;
        mSeekbar.setStart(mIsTiming);
        Intent intent = new Intent(Constants.CONTROL_TIMER);
        intent.putExtra("Time", mTime);
        intent.putExtra("Run", mIsTiming);
        sendBroadcast(intent);
        finish();
    }

    @OnClick({R.id.timer_info,R.id.timer_info_close,R.id.close_stop,R.id.close_toggle})
    public void OnClick(View view){
        switch (view.getId()){
            case R.id.timer_info:
                mContentContainer.setVisibility(View.INVISIBLE);
                mInfoContainer.setVisibility(View.VISIBLE);
                break;
            case R.id.timer_info_close:
                mInfoContainer.setVisibility(View.INVISIBLE);
                mContentContainer.setVisibility(View.VISIBLE);
                break;
            case R.id.close_stop:
                finish();
                break;
            case R.id.close_toggle:
                Toggle();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        if(mIsTiming) {
            TimeThread thread = new TimeThread();
            thread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsRunning = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(android.R.anim.fade_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    /**
     * 根据开始计时的时间，每隔一秒重新计算并通过handler更新界面
     */
    class TimeThread extends Thread{
        int min,sec,remain;
        @Override
        public void run(){
            while (mIsRunning){
                remain = (int)mTime * 60 - (int)(System.currentTimeMillis() - TimerService.mStartTime) / 1000;
                min = remain / 60;
                sec = remain % 60;
                Message msg = new Message();
                msg.arg1 = min;
                Bundle data = new Bundle();
                data.putString("Minute",min < 10 ? "0" + min : "" + min);
                data.putString("Second",sec < 10 ? "0" + sec : "" + sec);
                msg.setData(data);
                mHandler.sendMessage(msg);
                LogUtil.d("Timer","SendMsg");
                try {
                    sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
