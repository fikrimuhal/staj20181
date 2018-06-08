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

/**
 * Main Activity for the IMA plugin demo. {@link ExoPlayer} objects are created by
 * {@link PlayerManager}, which this class instantiates.
 */
public final class ReceiverActivity extends AppCompatActivity {

  static public AssetManager mngr;
  static public Context context;
  private PlayerView playerView;
  private PlayerManager player;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_receiver);
    context = this;
    playerView = findViewById(R.id.player_view);
    player = new PlayerManager(this);
    mngr = getAssets();
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
