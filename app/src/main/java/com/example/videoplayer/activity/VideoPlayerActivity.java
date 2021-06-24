package com.example.videoplayer.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;

import com.example.videoplayer.R;
import com.example.videoplayer.pojo.MediaInfo;
import com.example.videoplayer.pojo.PlayMode;

import java.io.IOException;
import java.util.List;

public class VideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    public static final String EXTRA_NAME_MEDIAINFOS = "mediainfos";

    private static final int INIT_PLAYER_WITH_NOTHING = 0;
    private static final int INIT_PLAYER_WITH_RESET = 1;
    private static final int INIT_PLAYER_WITH_RELEASE = 2;

    private ViewGroup flVideoArea;
    private ViewGroup vgNormalScreenContainer;
    private ViewGroup vgFullScreenContainer;
    private ImageView ivBack;
    private ImageView ivFullScreenSwitch;

    private TextureView tvMp;
    private LinearLayout llAreaTop;
    private TextView tvTitle;
    private LinearLayout llAreaCenter;
    private ListView lvListInner;
    private ListView lvListOuter;
    private LinearLayout llAreaBottom;
    private ImageView ivPlayPause;
    private AppCompatSeekBar sbMp;
    private TextView tvTime;


    private MediaPlayer mp;
    private List<MediaInfo> mediaInfos;
    private boolean autoPlayNext = true;
    private PlayMode playMode = PlayMode.REPEAT_LIST;
    private BaseMediaInfoAdapter innerMediaInfoAdapter;
    private BaseMediaInfoAdapter outerMediaInfoAdapter;

    /**
     * 是否自动播放第一首
     */
    private boolean autoPlayFirst = true;
    /**
     * 当前视频的索引号
     */
    private int currentIndex = 0;
    /**
     * /**
     * 更新播放器UI的时间间隔
     */
    private static final int UPDATE_PLAYER_UI_INTERVAL = 100;

    private MediaPlayerHandler mpHandler = new MediaPlayerHandler();


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        mediaInfos = (List<MediaInfo>) getIntent().getSerializableExtra(EXTRA_NAME_MEDIAINFOS);
        Log.i(TAG, "onCreate: mediaInfos = " + mediaInfos);
        if (null == mediaInfos || mediaInfos.size() == 0) {
            finish();
        }

        findViews();
        initViewsAndListeners();

        playByIndex(currentIndex);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mp) {
            try {
                mp.start();
                mpHandler.removeMessages(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
                ivPlayPause.setImageResource(R.drawable.play);
            } catch (Exception e) {
                Log.e(TAG, "onPause: ", e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (null != mp) {
            try {
                mp.release();
            } catch (Exception e) {
                Log.e(TAG, "onStop: ", e);
            } finally {
                mp = null;
            }
        }
        mpHandler = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (vgFullScreenContainer.indexOfChild(flVideoArea) != -1) {
            exitFullScreen();
        } else {
            super.onBackPressed();
        }
    }


    /**
     * 查找所有的视图
     */
    private void findViews() {
        flVideoArea = findViewById(R.id.fl_video_area);
        vgNormalScreenContainer = findViewById(R.id.ll_normal_screen_container);
        vgFullScreenContainer = findViewById(R.id.ll_full_screen_container);
        ivBack = findViewById(R.id.iv_back);
        ivFullScreenSwitch = findViewById(R.id.iv_full_screen_switch);

        tvMp = findViewById(R.id.tv_mp);
        llAreaTop = findViewById(R.id.ll_area_top);
        tvTitle = findViewById(R.id.tv_title);
        llAreaCenter = findViewById(R.id.ll_area_center);
        lvListInner = findViewById(R.id.lv_list_inner);
        lvListOuter = findViewById(R.id.lv_list_outer);
        llAreaBottom = findViewById(R.id.ll_area_bottom);
        ivPlayPause = findViewById(R.id.iv_play_pause);
        sbMp = findViewById(R.id.sb_process);
        tvTime = findViewById(R.id.tv_time);
    }

    /**
     * 初始化所有视图及监听器
     */
    private void initViewsAndListeners() {


        // 播放暂停
        ivPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == mp) {
                    return;
                }
                try {
                    if (mp.isPlaying()) {
                        mp.pause();
                        mpHandler.removeMessages(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
                        ivPlayPause.setImageResource(R.drawable.play);
                    } else {
                        mp.start();
                        mpHandler.sendEmptyMessage(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
                        ivPlayPause.setImageResource(R.drawable.pause);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

        // 拖动播放
        sbMp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mpHandler.removeMessages(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                int max = seekBar.getMax();
                float rate = progress / (float) max;
                try {
                    int duration = mp.getDuration();
                    int position = Math.round(duration * rate);
                    mp.seekTo(position);
                    mpHandler.sendEmptyMessage(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
                } catch (Exception e) {
                    Log.e(TAG, "onStopTrackingTouch: ", e);
                }
            }
        });

        tvMp.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                if (null != mp) {
                    try {
                        mp.setSurface(new Surface(surface));
                        fixVideoCenterInTextureView();
                    } catch (Exception e) {
                        Log.e(TAG, "onSurfaceTextureAvailable: ", e);
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });

        //
        innerMediaInfoAdapter = new InnerMediaInfoAdapter();
        lvListInner.setAdapter(innerMediaInfoAdapter);
        innerMediaInfoAdapter.setMediaInfos(mediaInfos);
        innerMediaInfoAdapter.notifyDataSetChanged();
        lvListInner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                lvListOuter.setSelection(position);
                playByIndex(position);
            }
        });
        outerMediaInfoAdapter = new OuterMediaInfoAdapter();
        lvListOuter.setAdapter(outerMediaInfoAdapter);
        outerMediaInfoAdapter.setMediaInfos(mediaInfos);
        outerMediaInfoAdapter.notifyDataSetChanged();
        lvListOuter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                lvListInner.setSelection(position);
                playByIndex(position);
            }
        });


        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitFullScreen();
            }
        });
        ivFullScreenSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                entryFullScreen();
            }
        });
        llAreaCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lvListInner.getVisibility() == View.VISIBLE) {
                    lvListInner.setVisibility(View.GONE);
                } else {
                    lvListInner.setVisibility(View.VISIBLE);
                }
            }
        });
        ivFullScreenSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vgFullScreenContainer.indexOfChild(flVideoArea) == -1) {
                    entryFullScreen();
                } else {
                    exitFullScreen();
                }
            }
        });
    }


    /**
     * 根据序号播放
     *
     * @param index
     */
    private void playByIndex(int index) {
        currentIndex = index;
        lvListInner.setSelection(index);
        lvListOuter.setSelection(index);
        //
        if (index < 0 || index >= mediaInfos.size()) {
            Toast.makeText(this, "播放序号错误", Toast.LENGTH_SHORT).show();
            return;
        }
        initMediaPlayer(INIT_PLAYER_WITH_RESET);
        String data = mediaInfos.get(index).getUrl();
        tvTitle.setText(mediaInfos.get(index).getTitle());
        setMediaSourceAndPlay(data);
    }


    /**
     * 播放上一首
     */
    private void playPrevious() {

    }

    // 播放下一首
    private void playNext() {
        if (null == playMode) {
            playMode = PlayMode.REPEAT_LIST;
        }
        Integer targetIndex = null;
        switch (playMode) {
            case RANDOM:
                targetIndex = (int) Math.round(Math.random() * 1000) % mediaInfos.size();
                break;
            case REPEAT_ONE:
                targetIndex = currentIndex;
                break;
            case REPEAT_LIST:
                targetIndex = (currentIndex + 1) % mediaInfos.size();
                break;
        }
        playByIndex(targetIndex);
    }

    /**
     * 初始化MediaPlayer。包括相关的监听器和视频控件。
     *
     * @param force 可用值： {@link #INIT_PLAYER_WITH_NOTHING}、{@link #INIT_PLAYER_WITH_RESET}、{@link #INIT_PLAYER_WITH_RELEASE}。
     */
    private void initMediaPlayer(int force) {
        // 重置的时候，先清除和MediaPlayer相关的一切相关操作。否则，会导致出现相关状态错误（what=-38）错误。
        mpHandler.removeMessages(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);

        if (mp != null) {
            try {
                if (force == INIT_PLAYER_WITH_RELEASE) {
                    mp.release();
                    mp = null;
                } else if (force == INIT_PLAYER_WITH_RESET) {
                    mp.reset();
                }
            } catch (Exception e) {
                Log.e(TAG, "initMediaPlayer: ", e);
                mp = null;
            }
        }

        if (null != mp) {
            return;
        }

        mp = new MediaPlayer();
        SurfaceTexture surfaceTexture = tvMp.getSurfaceTexture();
        if (null != surfaceTexture) {
            mp.setSurface(new Surface(surfaceTexture));
        }
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "onPrepared: ");
                try {
                    mp.start();
                    mpHandler.sendEmptyMessage(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
                    ivPlayPause.setImageResource(R.drawable.pause);
                } catch (Exception e) {
                    Log.e(TAG, "onPrepared: ", e);
                }
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(TAG, "onCompletion: ");
                mpHandler.removeMessages(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
                if (autoPlayNext) {
                    playNext();
                } else {
                    finish();
                }
            }
        });
        mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.i(TAG, "onInfo: MediaPlayer. what = " + what + ", extra = " + extra);
                return true;
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "onError: MediaPlayer. what = " + what + ", extra = " + extra, null);
                Toast.makeText(VideoPlayerActivity.this, "发生错误，自动播放下一首", Toast.LENGTH_SHORT).show();
                initMediaPlayer(INIT_PLAYER_WITH_RESET);
                playNext();
                return true;
            }
        });
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.i(TAG, "onBufferingUpdate: MediaPlayer. percent = " + percent);
            }
        });
        mp.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
            @Override
            public void onTimedText(MediaPlayer mp, TimedText text) {
                Log.i(TAG, "onTimedText: MediaPlayer. text = " + text.getText());
            }
        });
        mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                fixVideoCenterInTextureView();
            }
        });

    }

    /**
     * 设置播放源并开始播放
     *
     * @param url
     */
    private void setMediaSourceAndPlay(String url) {
        if (null == mp) {
            return;
        }
        try {
            mp.setDataSource(url);
            mp.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(this, "找不到视频：" + mediaInfos.get(currentIndex).getTitle() + "，以为您切换到下一首。", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "setMediaSourceAndPlay: ", e);
            playNext();
        }
    }


    /**
     * 时间转时分秒字符串：xx:xx:xx
     *
     * @param time
     * @return
     */
    private static final String getTimeString(int time) {
        time = time / 1000;
        int seconds = time % 60;
        int minutes = time / 60 % 60;
        int hours = time / 60 / 60;

        StringBuilder text = new StringBuilder();
        if (hours < 10) {
            text.append("0");
        }
        text.append(hours).append(":");
        if (minutes < 10) {
            text.append("0");
        }
        text.append(minutes).append(":");
        if (seconds < 10) {
            text.append("0");
        }
        text.append(seconds);
        return text.toString();
    }


    /**
     * 进入全屏
     */
    private void entryFullScreen() {
        if (vgNormalScreenContainer.indexOfChild(flVideoArea) == -1) {
            return;
        }
        vgNormalScreenContainer.removeView(flVideoArea);
        vgFullScreenContainer.addView(flVideoArea);
        vgFullScreenContainer.setVisibility(View.VISIBLE);
        llAreaCenter.setClickable(true);
        ivBack.setVisibility(View.VISIBLE);
        ivFullScreenSwitch.setImageResource(R.drawable.ic_exit_full_screen);
        //切换横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //
        fullscreen(true);
    }

    /**
     * 退出全屏
     */
    private void exitFullScreen() {
        if (vgFullScreenContainer.indexOfChild(flVideoArea) == -1) {
            return;
        }
        vgFullScreenContainer.removeView(flVideoArea);
        vgNormalScreenContainer.addView(flVideoArea);
        vgFullScreenContainer.setVisibility(View.GONE);
        llAreaCenter.setClickable(false);
        ivBack.setVisibility(View.INVISIBLE);
        ivFullScreenSwitch.setImageResource(R.drawable.ic_full_screen);
        lvListInner.setVisibility(View.GONE);
        //切换竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //
        fullscreen(false);
    }


    private void fullscreen(boolean enable) {

        View decorView = getWindow().getDecorView();
        if (enable) { //隐藏状态栏
//            WindowManager.LayoutParams lp = getWindow().getAttributes();
//            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//            getWindow().setAttributes(lp);
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            if (Build.VERSION.SDK_INT < 19) {
                decorView.setSystemUiVisibility(View.VISIBLE);
            } else {
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
            }
        } else { //显示状态栏
//            WindowManager.LayoutParams lp = getWindow().getAttributes();
//            lp.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            getWindow().setAttributes(lp);
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            if (Build.VERSION.SDK_INT < 19) {
                decorView.setSystemUiVisibility(View.GONE);
            } else {
                int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                decorView.setSystemUiVisibility(uiOptions);
            }
        }
    }

    /**
     * 使视频在TextureView中居中
     */
    private void fixVideoCenterInTextureView() {
        try {

            int mVideoWidth = mp.getVideoWidth();
            int mVideoHeight = mp.getVideoHeight();
            float sx = (float) tvMp.getWidth() / (float) mVideoWidth;
            float sy = (float) tvMp.getHeight() / (float) mVideoHeight;

            Matrix matrix = new Matrix();

            //第1步:把视频区移动到View区,使两者中心点重合.
            matrix.preTranslate((tvMp.getWidth() - mVideoWidth) / 2, (tvMp.getHeight() - mVideoHeight) / 2);

            //第2步:因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
            matrix.preScale(mVideoWidth / (float) tvMp.getWidth(), mVideoHeight / (float) tvMp.getHeight());

            //第3步,等比例放大或缩小,直到视频区的一边和View一边相等.如果另一边和view的一边不相等，则留下空隙
            if (sx >= sy) {
                matrix.postScale(sy, sy, tvMp.getWidth() / 2, tvMp.getHeight() / 2);
            } else {
                matrix.postScale(sx, sx, tvMp.getWidth() / 2, tvMp.getHeight() / 2);
            }

            tvMp.setTransform(matrix);
            tvMp.postInvalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    abstract class BaseMediaInfoAdapter extends BaseAdapter {

        private int convertViewId;

        private List<MediaInfo> mediaInfos;

        public BaseMediaInfoAdapter(int convertViewId) {
            this.convertViewId = convertViewId;
        }

        public List<MediaInfo> getMediaInfos() {
            return mediaInfos;
        }

        public void setMediaInfos(List<MediaInfo> mediaInfos) {
            this.mediaInfos = mediaInfos;
        }

        @Override
        public int getCount() {
            if (null == mediaInfos) {
                return 0;
            } else {
                return mediaInfos.size();
            }
        }

        @Override
        public Object getItem(int position) {
            if (null == mediaInfos) {
                return null;
            }
            if (position >= 0 && position < mediaInfos.size()) {
                return mediaInfos.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = getLayoutInflater().inflate(convertViewId, parent, false);
            }
            MediaInfo mediaInfo = (MediaInfo) getItem(position);
            initConvertView(position, convertView, parent, mediaInfo);
            return convertView;
        }

        /**
         * 初始化ConvertView
         *
         * @param position
         * @param convertView
         * @param parent
         * @param mediaInfo
         */
        abstract void initConvertView(int position, View convertView, ViewGroup parent, MediaInfo mediaInfo);
    }

    class InnerMediaInfoAdapter extends BaseMediaInfoAdapter {

        public InnerMediaInfoAdapter() {
            super(R.layout.ll_media_info_inner);
        }

        @Override
        void initConvertView(int position, View convertView, ViewGroup parent, MediaInfo mediaInfo) {
            ImageView ivThumbnail = convertView.findViewById(R.id.iv_thumbnail);
            TextView tvTitle = convertView.findViewById(R.id.tv_title);
            TextView tvDuration = convertView.findViewById(R.id.tv_duration);

            tvTitle.setText(mediaInfo.getTitle());
        }
    }

    class OuterMediaInfoAdapter extends BaseMediaInfoAdapter {

        public OuterMediaInfoAdapter() {
            super(R.layout.ll_media_info_outer);
        }

        @Override
        void initConvertView(int position, View convertView, ViewGroup parent, MediaInfo mediaInfo) {
            ImageView ivThumbnail = convertView.findViewById(R.id.iv_thumbnail);
            TextView tvTitle = convertView.findViewById(R.id.tv_title);
            TextView tvDuration = convertView.findViewById(R.id.tv_duration);

            tvTitle.setText(mediaInfo.getTitle());
        }
    }

    class MediaPlayerHandler extends Handler {
        public static final int WHAT_UPDATE_PLAYER_UI = 1;

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case WHAT_UPDATE_PLAYER_UI:
                    if (mp == null) {
                        return;
                    }
                    try {
                        int currentPosition = mp.getCurrentPosition();
                        int duration = mp.getDuration();
                        if (currentPosition < 0 || duration <= 0) {
                            return;
                        }

                        tvTime.setText(getTimeString(currentPosition) + "/" + getTimeString(duration));

                        float rate = currentPosition / (float) duration;
                        int max = sbMp.getMax();
                        int progress = Math.round(max * rate);
                        sbMp.setProgress(progress);
                    } catch (Exception e) {
                        Log.e(TAG, "handleMessage: ", e);
                    }
                    this.sendEmptyMessageDelayed(WHAT_UPDATE_PLAYER_UI, UPDATE_PLAYER_UI_INTERVAL);
                    break;
            }

        }
    }

}