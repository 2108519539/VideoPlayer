package com.example.videoplayer.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.videoplayer.R;
import com.example.videoplayer.pojo.MediaInfo;
import com.example.videoplayer.pojo.PlayMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    public static final String EXTRA_NAME_MEDIAINFOS = "mediainfos";

    private static final int INIT_PLAYER_WITH_NOTHING = 0;
    private static final int INIT_PLAYER_WITH_RESET = 1;
    private static final int INIT_PLAYER_WITH_RELEASE = 2;


    private TextureView tvMp;
    private LinearLayout llAreaTop;
    private TextView tvTitle;
    private LinearLayout llAreaCenter;
    private ListView lvList;
    private LinearLayout llAreaBottom;
    private ImageView ivPlayPause;
    private AppCompatSeekBar sbMp;
    private TextView tvTime;


    private MediaPlayer mp;
    private List<MediaInfo> mediaInfos;
    private boolean autoPlayNext = true;
    private PlayMode playMode = PlayMode.REPEAT_LIST;

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
                e.printStackTrace();
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
                e.printStackTrace();
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

    /**
     * 查找所有的视图
     */
    private void findViews() {
        tvMp = findViewById(R.id.tv_mp);
        llAreaTop = findViewById(R.id.ll_area_top);
        tvTitle = findViewById(R.id.tv_title);
        llAreaCenter = findViewById(R.id.ll_area_center);
        lvList = findViewById(R.id.lv_list);
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
                initMediaPlayer(INIT_PLAYER_WITH_NOTHING);
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
                    e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
        });

        tvMp.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                initMediaPlayer(INIT_PLAYER_WITH_NOTHING);
                playByIndex(currentIndex);
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
    }


    /**
     * 根据序号播放
     *
     * @param index
     */
    private void playByIndex(int index) {
        currentIndex = index;
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
        if (mp != null) {
            try {
                if (force == INIT_PLAYER_WITH_RELEASE) {
                    mp.release();
                } else if (force == INIT_PLAYER_WITH_RESET) {
                    mp.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mp = null;
            }
        }

        mp = new MediaPlayer();
        mp.setSurface(new Surface(tvMp.getSurfaceTexture()));
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "onPrepared: ");
                try {
                    mp.start();
                    mpHandler.sendEmptyMessage(MediaPlayerHandler.WHAT_UPDATE_PLAYER_UI);
                    ivPlayPause.setImageResource(R.drawable.pause);
                } catch (Exception e) {
                    e.printStackTrace();
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
            e.printStackTrace();
            playNext();
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
                        e.printStackTrace();
                    }
                    this.sendEmptyMessageDelayed(WHAT_UPDATE_PLAYER_UI, UPDATE_PLAYER_UI_INTERVAL);
                    break;
            }

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
}