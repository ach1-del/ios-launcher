package com.zyvo.gnbox;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class MediaActivity extends BaseActivity {
    private static final int PICK_FILE = 7;
    private VideoView video;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.screen(this);
        root.addView(Ui.header(this, getString(R.string.media_title)));

        status = Ui.status(this);
        status.setText(R.string.nothing_playing);
        root.addView(status);

        LinearLayout buttons = Ui.row(this);
        Button pick = Ui.btn(this, getString(R.string.pick_file), true);
        Button stop = Ui.btn(this, getString(R.string.stop), false);
        LinearLayout.LayoutParams slp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.leftMargin = Ui.dp(this, 8);
        buttons.addView(pick);
        buttons.addView(stop, slp);
        root.addView(buttons);

        video = new VideoView(this);
        video.setMediaController(new MediaController(this));
        video.setBackgroundColor(getColor(R.color.bg1));
        root.addView(video, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, PICK_FILE);
        });
        stop.setOnClickListener(v -> {
            try { video.stopPlayback(); } catch (Exception ignored) {}
            status.setText(R.string.nothing_playing);
        });

        video.setOnErrorListener((mp, what, extra) -> {
            status.setText(R.string.stream_error);
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        String name = uri.getLastPathSegment();
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}

        status.setText(name);
        video.setVideoURI(uri);
        video.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            video.start();
        });
        video.requestFocus();
        Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        try { video.stopPlayback(); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
