package com.zyvo.auratv;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends BaseActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private View errorBox;
    private TextView tvTitle, btnAspect;
    private ImageView btnFav;
    private Prefs prefs;
    private Channel channel;
    private String sourceId;
    private int resizeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setBackgroundDrawableResource(android.R.color.black);
        setContentView(R.layout.activity_player);
        Ui.immersive(this);

        prefs = new Prefs(this);
        playerView = findViewById(R.id.playerView);
        errorBox = findViewById(R.id.errorBox);
        tvTitle = findViewById(R.id.tvTitle);
        btnAspect = findViewById(R.id.btnAspect);
        btnFav = findViewById(R.id.btnFav);

        channel = new Channel();
        channel.name = Ui.safe(getIntent().getStringExtra("name"));
        channel.url = Ui.safe(getIntent().getStringExtra("url"));
        channel.logo = Ui.safe(getIntent().getStringExtra("logo"));
        channel.id = Ui.safe(getIntent().getStringExtra("id"));
        channel.cmd = Ui.safe(getIntent().getStringExtra("cmd"));
        channel.kind = getIntent().getIntExtra("kind", Channel.KIND_LIVE);
        sourceId = Ui.safe(getIntent().getStringExtra("sourceId"));
        final String originalUrl = channel.url;

        tvTitle.setText(channel.name.isEmpty() ? getString(R.string.app_name) : channel.name);
        refreshFav();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            errorBox.setVisibility(View.GONE);
            startPlayback();
        });
        btnFav.setOnClickListener(v -> {
            boolean fav = prefs.toggleFavorite(channel);
            refreshFav();
            Ui.toast(this, getString(fav ? R.string.added_fav : R.string.removed_fav));
        });

        resizeMode = prefs.aspect();
        applyAspect();
        btnAspect.setOnClickListener(v -> {
            resizeMode = resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT;
            prefs.setAspect(resizeMode);
            applyAspect();
        });

        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(), true);
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                errorBox.setVisibility(View.VISIBLE);
            }
        });
        startPlayback();
    }

    private void applyAspect() {
        playerView.setResizeMode(resizeMode);
        btnAspect.setText(resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                ? "ZOOM" : getString(R.string.aspect));
    }

    private void refreshFav() {
        btnFav.setImageResource(prefs.isFavorite(channel.id) ? R.drawable.ic_star_filled : R.drawable.ic_star);
    }

    private void startPlayback() {
        if (channel.cmd != null && !channel.cmd.isEmpty()) {
            Source src = prefs.sourceById(sourceId);
            if (src != null) {
                Bg.work(() -> new StalkerClient(src).createLink(channel.cmd), new Bg.Done<String>() {
                    @Override
                    public void ok(String link) {
                        playUrl(link != null && !link.isEmpty() ? link : originalUrl);
                    }

                    @Override
                    public void fail(Exception e) {
                        playUrl(originalUrl);
                    }
                });
                return;
            }
        }
        playUrl(originalUrl);
    }

    private void playUrl(String url) {
        if (url == null || url.isEmpty()) {
            errorBox.setVisibility(View.VISIBLE);
            return;
        }
        MediaItem.Builder b = new MediaItem.Builder().setUri(url);
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8") || lower.contains("m3u8")) {
            b.setMimeType(MimeTypes.APPLICATION_M3U8);
        } else if (lower.contains(".mpd")) {
            b.setMimeType(MimeTypes.APPLICATION_MPD);
        } else if (lower.startsWith("rtsp")) {
            b.setMimeType("application/rtsp");
        }
        player.setMediaItem(b.build());
        player.prepare();
        player.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        playerView.setPlayer(null);
        super.onDestroy();
    }
}
