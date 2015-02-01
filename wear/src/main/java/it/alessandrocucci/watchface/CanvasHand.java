package it.alessandrocucci.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CanvasHand extends CanvasWatchFaceService {

    private final static String TAG = CanvasHand.class.getSimpleName();

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        boolean mMute;
        Time mTime;

        /** Handler to update the time once a second in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        Bitmap mHourBitmap;
        Bitmap mHourScaledBitmap;

        Bitmap mMinuteBitmap;
        Bitmap mMinuteScaledBitmap;

        Bitmap mSecondBitmap;
        Bitmap mSecondScaledBitmap;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(CanvasHand.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = CanvasHand.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.image_1);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            Drawable hourDrawable = resources.getDrawable(R.drawable.hour);
            mHourBitmap = ((BitmapDrawable) hourDrawable).getBitmap();

            Drawable minuteDrawable = resources.getDrawable(R.drawable.minute);
            mMinuteBitmap = ((BitmapDrawable) minuteDrawable).getBitmap();

            Drawable secondDrawable = resources.getDrawable(R.drawable.second);
            mSecondBitmap = ((BitmapDrawable) secondDrawable).getBitmap();

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;

            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;

                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);


            int seconds = mTime.second;
            int minutes = mTime.minute;
            int hours = mTime.hour;

            float secRot = seconds / 30f * 180f;
            float minRot = minutes / 30f * 180f;
            float hrRot = hours / 6f * 180f;

            // Draw the hour hand, scaled to fit.

            // Rotate the hour hand
            if (mHourScaledBitmap == null
                    || mHourScaledBitmap.getWidth() != width
                    || mHourScaledBitmap.getHeight() != height) {
                mHourScaledBitmap = Bitmap.createScaledBitmap(mHourBitmap,
                        width, height, true /* filter */);
            }
            // Rotate
            canvas.save();
            canvas.rotate(hrRot,canvas.getWidth()/2,canvas.getHeight()/2);


            canvas.drawBitmap(mHourScaledBitmap, 0, 0, null);

            canvas.restore();

            // Draw the minute hand, scaled to fit.

            // Rotate the minute hand
            if (mMinuteScaledBitmap == null
                    || mMinuteScaledBitmap.getWidth() != width
                    || mMinuteScaledBitmap.getHeight() != height) {
                mMinuteScaledBitmap = Bitmap.createScaledBitmap(mMinuteBitmap,
                        width, height, true /* filter */);
            }
            // Rotate the second hand
            canvas.save();
            canvas.rotate(minRot,canvas.getWidth()/2,canvas.getHeight()/2);


            canvas.drawBitmap(mMinuteScaledBitmap, 0, 0, null);

            canvas.restore();


            if (!isInAmbientMode()) {
                if (mSecondScaledBitmap == null
                        || mSecondScaledBitmap.getWidth() != width
                        || mSecondScaledBitmap.getHeight() != height) {
                    mSecondScaledBitmap = Bitmap.createScaledBitmap(mSecondBitmap,
                            width, height, true /* filter */);
                }
                // Rotate the second hand
                canvas.save();
                canvas.rotate(secRot,canvas.getWidth()/2,canvas.getHeight()/2);


                canvas.drawBitmap(mSecondScaledBitmap, 0, 0, null);

                canvas.restore();

            }

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            CanvasHand.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            CanvasHand.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

    }
}
