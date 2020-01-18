package com.breeze.views.Messages;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.breeze.R;

public class AudioMessage {

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean playing = false;

    private ImageButton playButton;
    private ImageButton pauseButton;
    private SeekBar seekBar;

    private Handler updateSeekbarHandler = new Handler();
    private Runnable updateSeekbarRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekbar();
            if (playing && updateSeekbarHandler != null)
                updateSeekbarHandler.postDelayed(this, 1000);
        }
    };

    public AudioMessage(View v, Context ctx, Uri audioFile, int backroundResource) {
        v.setBackgroundResource(backroundResource);

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

        try {
            this.mediaPlayer.setDataSource(ctx, audioFile);
            this.mediaPlayer.prepare();

            seekBar.setMax(mediaPlayer.getDuration() / 1000);

            mediaPlayer.setOnCompletionListener((e) -> {
                mediaPlayer.seekTo(1);
                pause();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

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
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = null;
        updateSeekbarHandler = null;
        updateSeekbarRunnable = null;
        playing = false;
    }
}
