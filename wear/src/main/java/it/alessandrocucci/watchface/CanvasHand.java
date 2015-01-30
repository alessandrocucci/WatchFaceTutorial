package it.alessandrocucci.watchface;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

public class CanvasHand extends CanvasWatchFaceService {

    private final static String TAG = CanvasHand.class.getSimpleName();


    @Override
    public Engine onCreateEngine() {

        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {
        float HOUR_HAND_LENGTH = 80.0f;
        final float MINUTE_HAND_LENGTH = 120.0f;

        Paint mHourPaint;
        Paint mMinutePaint;
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        Time mTime;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Log.d(TAG, "onCreate");
            setWatchFaceStyle(new WatchFaceStyle.Builder(CanvasHand.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 200, 200, 200);
            mHourPaint.setStrokeWidth(5.f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 200, 200, 200);
            mMinutePaint.setStrokeWidth(3.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mTime = new Time();
        }


        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();
            float centerX = width / 2f;
            float centerY = height / 2f;



            Resources resources = CanvasHand.this.getResources();
            int imgResId;
            if (isInAmbientMode()) {
                imgResId = R.drawable.black_background;
            } else {
                imgResId = R.drawable.image_1;
            }
            Drawable backgroundDrawable = resources.getDrawable(imgResId);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    width, height, true);
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);


            float minRot = mTime.minute / 30f * (float) Math.PI;
            if (isInAmbientMode()) {
                mMinutePaint.setARGB(255, 200, 200, 200);
            } else {
                mMinutePaint.setARGB(255, 0, 128, 0);
            }
            float minX = (float) Math.sin(minRot) * MINUTE_HAND_LENGTH;
            float minY = (float) -Math.cos(minRot) * MINUTE_HAND_LENGTH;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mMinutePaint);


            float hrRot = ((mTime.hour + (mTime.minute / 60f)) / 6f) * (float) Math.PI;
            if (isInAmbientMode()) {
                mHourPaint.setARGB(255, 200, 200, 200);
            } else {
                mHourPaint.setARGB(255, 255, 0, 0);
            }
            float hrX = (float) Math.sin(hrRot) * HOUR_HAND_LENGTH;
            float hrY = (float) -Math.cos(hrRot) * HOUR_HAND_LENGTH;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHourPaint);
        }
    }
}
