/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.google_io_exoplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback;
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter;
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static com.example.google_io_exoplayer.C.MEDIA_SESSION_TAG;
import static com.example.google_io_exoplayer.C.PLAYBACK_CHANNEL_ID;
import static com.example.google_io_exoplayer.C.PLAYBACK_NOTIFICATION_ID;
import static com.example.google_io_exoplayer.Samples.SAMPLES;

public class AudioPlayerService extends Service {

  private static final String TAG = "AudioPlayerService";
  
  private SimpleExoPlayer player;
  private PlayerNotificationManager playerNotificationManager;
  private MediaSessionCompat mediaSession;
  private MediaSessionConnector mediaSessionConnector;

  @Override
  public void onCreate() {
    super.onCreate();
    final Context context = this;

    player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
    DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
        context, Util.getUserAgent(context, getString(R.string.app_name)));
    CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(
        DownloadUtil.getCache(context),
        dataSourceFactory,
        CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
    for (Samples.Sample sample : SAMPLES) {
      MediaSource mediaSource = new ExtractorMediaSource.Factory(cacheDataSourceFactory)
          .createMediaSource(sample.uri);
      concatenatingMediaSource.addMediaSource(mediaSource);

    }
    player.prepare(concatenatingMediaSource);
    player.setPlayWhenReady(true);

    playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
        context,
        PLAYBACK_CHANNEL_ID,
        R.string.playback_channel_name,
        PLAYBACK_NOTIFICATION_ID,
        new MediaDescriptionAdapter() {
          @Override
          public String getCurrentContentTitle(Player player) {
            return SAMPLES[player.getCurrentWindowIndex()].title;
          }

          @Nullable
          @Override
          public PendingIntent createCurrentContentIntent(Player player) {
            return null;
          }

          @Nullable
          @Override
          public String getCurrentContentText(Player player) {
            return SAMPLES[player.getCurrentWindowIndex()].description;
          }

          @Nullable
          @Override
          public Bitmap getCurrentLargeIcon(Player player, BitmapCallback callback) {
            return Samples.getBitmap(
                context, SAMPLES[player.getCurrentWindowIndex()].bitmapResource);
          }
        }
    );
    playerNotificationManager.setNotificationListener(new NotificationListener() {
      @Override
      public void onNotificationStarted(int notificationId, Notification notification) {
        startForeground(notificationId, notification);
      }

      @Override
      public void onNotificationCancelled(int notificationId) {
        stopSelf();
      }
    });
    playerNotificationManager.setPlayer(player);

    mediaSession = new MediaSessionCompat(context, MEDIA_SESSION_TAG);
    mediaSession.setActive(true);
    playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());

    mediaSessionConnector = new MediaSessionConnector(mediaSession);
    mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
      @Override
      public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
        return Samples.getMediaDescription(context, SAMPLES[windowIndex]);
      }
    });
    mediaSessionConnector.setPlayer(player, null);

  }


  @Override
  public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);
    stopSelf();
  }

  @Override
  public void onDestroy() {
    mediaSession.release();
    mediaSessionConnector.setPlayer(null, null);
    playerNotificationManager.setPlayer(null);
    player.release();
    player = null;

    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

}
