package com.zyvo.gnbox;

import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class RadioActivity extends BaseActivity {
    private final List<String[]> stations = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private MediaPlayer player;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stations.add(new String[]{"Monte Carlo Doualiya", "https://montecarlodoualiya128k.ice.infomaniak.ch/mc-doualiya.mp3"});
        stations.add(new String[]{"BBC Arabic", "https://stream.live.vc.bbcmedia.co.uk/bbc_arabic_radio"});
        stations.add(new String[]{"Radio Paradise", "https://stream.radioparadise.com/mp3-128"});
        stations.add(new String[]{"SomaFM Groove Salad", "https://ice1.somafm.com/groovesalad-128-mp3"});
        stations.add(new String[]{"SomaFM Drone Zone", "https://ice1.somafm.com/dronezone-128-mp3"});
        for (String[] s : stations) names.add(s[0]);

        LinearLayout root = Ui.screen(this);
        root.addView(Ui.header(this, getString(R.string.radio_title)));

        status = Ui.status(this);
        status.setText(R.string.nothing_playing);
        root.addView(status);

        ListView list = new ListView(this);
        list.setBackgroundColor(Color.TRANSPARENT);
        list.setDividerHeight(0);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convertView, parent);
                t.setTextColor(getColor(R.color.text));
                t.setTextSize(15);
                t.setPadding(Ui.dp(getContext(), 8), Ui.dp(getContext(), 14),
                        Ui.dp(getContext(), 8), Ui.dp(getContext(), 14));
                return t;
            }
        };
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        EditText custom = Ui.edit(this, getString(R.string.custom_stream));
        root.addView(custom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = Ui.row(this);
        buttons.setPadding(0, Ui.dp(this, 8), 0, 0);
        Button play = Ui.btn(this, getString(R.string.play), true);
        Button stop = Ui.btn(this, getString(R.string.stop), false);
        LinearLayout.LayoutParams slp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.leftMargin = Ui.dp(this, 8);
        buttons.addView(play);
        buttons.addView(stop, slp);
        root.addView(buttons);

        setContentView(root);

        list.setOnItemClickListener((parent, view, position, id) -> play(stations.get(position)[0], stations.get(position)[1]));
        play.setOnClickListener(v -> {
            String u = custom.getText().toString().trim();
            if (u.isEmpty()) {
                Toast.makeText(this, R.string.custom_stream, Toast.LENGTH_SHORT).show();
            } else {
                play("Custom", u);
            }
        });
        stop.setOnClickListener(v -> stop());
    }

    private void play(String name, String url) {
        stop();
        status.setText(getString(R.string.loading_player) + "  " + name);
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(url);
            player.setOnPreparedListener(mp -> {
                mp.start();
                status.setText(getString(R.string.playing) + ": " + name);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                status.setText(R.string.stream_error);
                stop();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            status.setText(R.string.stream_error);
            stop();
        }
    }

    private void stop() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        if (status != null) status.setText(R.string.nothing_playing);
    }

    @Override
    protected void onDestroy() {
        stop();
        super.onDestroy();
    }
}
