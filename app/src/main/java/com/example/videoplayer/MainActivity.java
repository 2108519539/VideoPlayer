package com.example.videoplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.example.videoplayer.activity.VideoPlayerActivity;
import com.example.videoplayer.pojo.MediaInfo;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static int REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        boolean needToRequirePermission = false;
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needToRequirePermission = true;
                break;
            }
        }
        if (needToRequirePermission == true) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE);
        } else {
            startVideoPlayer();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }

        startVideoPlayer();
    }

    private void startVideoPlayer() {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        ArrayList<MediaInfo> mediaInfos = new ArrayList<>();
        MediaInfo mi1 = new MediaInfo();
        mi1.setUrl("/sdcard/Movies/62C6013CF25A75E63084FD62B2E06576.mp4");
        mi1.setTitle("小提琴曲 离开你的那一天 - 音乐短片");
        mediaInfos.add(mi1);
        MediaInfo mi2 = new MediaInfo();
        mi2.setUrl("/sdcard/Movies/videoplayback.mp4");
        mi2.setTitle("无问");
        mediaInfos.add(mi2);
        MediaInfo mi3 = new MediaInfo();
        mi3.setUrl("/sdcard/Movies/张韶涵《阿刁》 -单曲纯享 《歌手2018》第2期  Singer2018【歌手官方频道】.mp4.mpeg");
        mi3.setTitle("张韶涵《阿刁》 -单曲纯享 《歌手2018》第2期  Singer2018【歌手官方频道】.mp4");
        mediaInfos.add(mi3);
        MediaInfo mi4 = new MediaInfo();
        mi4.setUrl("/sdcard/Movies/张韶涵《阿刁》 -单曲纯享 《歌手2018》第2期  Singer2018【歌手官方频道】.mp4.mpeg2");
        mi4.setTitle("张韶涵《阿刁》 -单曲纯享 《歌手2018》第2期  Singer2018【歌手官方频道】.mp4");
        mediaInfos.add(mi4);
        intent.putExtra(VideoPlayerActivity.EXTRA_NAME_MEDIAINFOS, mediaInfos);
        startActivity(intent);

        finish();
    }
}