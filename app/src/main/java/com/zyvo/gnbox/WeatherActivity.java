package com.zyvo.gnbox;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WeatherActivity extends BaseActivity {
    private EditText city;
    private TextView status;
    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.screen(this);
        root.addView(Ui.header(this, getString(R.string.weather_title)));

        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        city = Ui.edit(this, getString(R.string.city_hint));
        city.setText("Baghdad");
        row.addView(city, Ui.lpw(1f));
        Button search = Ui.btn(this, getString(R.string.search), true);
        LinearLayout.LayoutParams lp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 8);
        row.addView(search, lp);
        root.addView(row);

        status = Ui.status(this);
        root.addView(status);

        result = Ui.tv(this, "", 17, getColor(R.color.text));
        result.setLineSpacing(Ui.dp(this, 6), 1f);
        root.addView(result, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        search.setOnClickListener(v -> fetch());
        fetch();
    }

    private void fetch() {
        final String query = city.getText().toString().trim();
        if (query.isEmpty()) return;
        status.setText(R.string.loading);
        result.setText("");
        new Thread(() -> {
            try {
                String q = URLEncoder.encode(query, "UTF-8");
                String geo = get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&format=json&name=" + q);
                JSONObject g = new JSONObject(geo);
                JSONArray arr = g.optJSONArray("results");
                if (arr == null || arr.length() == 0) {
                    ui(() -> {
                        status.setText("");
                        result.setText(R.string.net_error);
                    });
                    return;
                }
                JSONObject place = arr.getJSONObject(0);
                double lat = place.getDouble("latitude");
                double lon = place.getDouble("longitude");
                String name = place.optString("name", query);
                String country = place.optString("country", "");

                String w = get("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&timezone=auto");
                JSONObject cur = new JSONObject(w).getJSONObject("current");
                double temp = cur.getDouble("temperature_2m");
                int hum = cur.getInt("relative_humidity_2m");
                double wind = cur.getDouble("wind_speed_10m");
                int code = cur.optInt("weather_code", -1);

                String text = name + (country.isEmpty() ? "" : ", " + country) + "\n\n"
                        + getString(R.string.temp) + ": " + temp + " °C\n"
                        + getString(R.string.humidity) + ": " + hum + " %\n"
                        + getString(R.string.wind) + ": " + wind + " km/h\n\n"
                        + condition(code);
                ui(() -> {
                    status.setText("");
                    result.setText(text);
                });
            } catch (Exception e) {
                ui(() -> {
                    status.setText(R.string.net_error);
                    result.setText("");
                });
            }
        }).start();
    }

    private void ui(Runnable r) {
        runOnUiThread(r);
    }

    private String condition(int code) {
        if (code == 0) return "Clear sky";
        if (code <= 3) return "Partly cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code <= 57) return "Drizzle";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Rain showers";
        if (code <= 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "—";
    }

    private String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "GN-RS8-Box/1.0");
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }
}
