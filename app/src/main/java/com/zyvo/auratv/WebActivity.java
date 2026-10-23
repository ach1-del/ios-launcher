package com.zyvo.auratv;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class WebActivity extends BaseActivity {
    private android.webkit.WebView web;
    private View progress;
    private TextView tvError;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        if (url == null || url.isEmpty()) url = "https://www.google.com";
        if (title == null || title.isEmpty()) title = getString(R.string.browser);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(title);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);
        web = findViewById(R.id.webView);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnExternal).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(web.getUrl() == null ? url : web.getUrl())));
            } catch (Exception e) {
                Ui.toast(this, getString(R.string.error));
            }
        });

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setUserAgentString(web.getSettings().getUserAgentString() + " AuraTV/2.0");
        web.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onProgressChanged(android.webkit.WebView view, int newProgress) {
                progress.setVisibility(newProgress >= 90 ? View.GONE : View.VISIBLE);
            }
        });
        web.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageStarted(android.webkit.WebView view, String u, android.graphics.Bitmap favicon) {
                tvError.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String u) {
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(android.webkit.WebView view, android.webkit.WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                progress.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
            }
        });
        web.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
