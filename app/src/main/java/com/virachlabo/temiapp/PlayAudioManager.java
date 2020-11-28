package com.virachlabo.temiapp;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

public class PlayAudioManager {
    private static MediaPlayer mediaPlayer;

    public static void playAudio(final Context context, final String url) throws Exception {
        if (mediaPlayer == null) {
//            mediaPlayer = MediaPlayer.create(context, Uri.parse(url));
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(context, Uri.parse(url));
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                killMediaPlayer();
            }
        });
        //mediaPlayer.setLooping(true);
        mediaPlayer.prepareAsync();
    }

    private static void killMediaPlayer() {
        Log.i("MEDIAPLAYER", "KILL");
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}