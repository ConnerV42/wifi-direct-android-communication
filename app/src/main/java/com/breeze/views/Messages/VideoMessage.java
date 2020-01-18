package com.breeze.views.Messages;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.VideoView;

import com.breeze.R;

public class VideoMessage {

    private MediaPlayer mediaPlayer;
    private boolean playing = false;

    private ImageButton playButton;
    private ImageButton pauseButton;
    private SeekBar seekBar;
    private VideoView video;

    private Handler updateSeekbarHandler = new Handler();
    private Runnable updateSeekbarRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekbar();
            if (playing && updateSeekbarHandler != null)
                updateSeekbarHandler.postDelayed(this, 1000);
        }
    };

    public VideoMessage(View v, VideoView video, int backgroundResource) {
        // Set the media controller's background
        v.setBackgroundResource(backgroundResource);

        this.playButton = v.findViewById(R.id.messageMediaPlayButton);
        this.pauseButton = v.findViewById(R.id.messageMediaPauseButton);

        this.seekBar = v.findViewById(R.id.messageMediaSeekbar);
        seekBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        seekBar.getThumb().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser)
                    mediaPlayer.seekTo(progress * 1000);
            }
        });
        this.video = video;
        video.setOnPreparedListener(mp -> {
            mediaPlayer = mp;
            seekBar.setMax(mediaPlayer.getDuration() / 1000);
            mediaPlayer.seekTo(1);

            mp.setOnCompletionListener((e) -> {
                mp.seekTo(1);
                pause();
            });

        });

        this.playButton.setVisibility(View.VISIBLE);
        this.pauseButton.setVisibility(View.GONE);

        playButton.setOnClickListener(e -> play());
        pauseButton.setOnClickListener(e -> pause());
    }

    public void play() {
        mediaPlayer.start();
        playButton.setVisibility(View.GONE);
        pauseButton.setVisibility(View.VISIBLE);

        playing = true;
        updateSeekbarRunnable.run();
    }

    public void pause() {
        mediaPlayer.pause();
        playButton.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.GONE);

        playing = false;
    }

    private void updateSeekbar() {
        if (mediaPlayer != null && seekBar != null)
            seekBar.setProgress(mediaPlayer.getCurrentPosition() / 1000);
    }

    public void destroy() {
        updateSeekbarHandler = null;
        updateSeekbarRunnable = null;
        playing = false;
    }
}
