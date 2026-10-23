package com.zyvo.auratv;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One background worker for network + parsing, results posted on the UI thread. */
public final class Bg {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Bg() {
    }

    public static void run(Runnable task) {
        POOL.execute(task);
    }

    public static void ui(Runnable task) {
        MAIN.post(task);
    }

    public interface Job<T> {
        T run() throws Exception;
    }

    public interface Done<T> {
        void ok(T value);

        void fail(Exception e);
    }

    public static <T> void work(Job<T> job, Done<T> done) {
        POOL.execute(() -> {
            try {
                T value = job.run();
                MAIN.post(() -> done.ok(value));
            } catch (Exception e) {
                MAIN.post(() -> done.fail(e));
            }
        });
    }
}
