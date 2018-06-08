package com.hivecdn.androidp2p;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class ReceiverActivity extends AppCompatActivity {

  private PlayerView playerView;
  private PlayerManager player;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_receiver);
    playerView = findViewById(R.id.player_view);
    player = new PlayerManager(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    player.init(this, playerView);
  }

  @Override
  public void onPause() {
    super.onPause();
    player.reset();
  }

  @Override
  public void onDestroy() {
    player.release();
    super.onDestroy();
  }

}
