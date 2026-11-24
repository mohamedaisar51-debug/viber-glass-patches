package app.template.extension.viber;

import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;

@SuppressWarnings("unused")
public class LiquidGlassHook {
    public static void applyLiquidGlass(final View navBar) {
        if (navBar == null || Boolean.TRUE.equals(navBar.getTag(0x7F0B9999))) return;
        navBar.setTag(0x7F0B9999, Boolean.TRUE);
        final float d = navBar.getResources().getDisplayMetrics().density;
        final float r = 30f * d;

        navBar.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams lp = navBar.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    mlp.leftMargin = (int) (18f * d);
                    mlp.rightMargin = (int) (18f * d);
                    mlp.bottomMargin = (int) (14f * d);
                    navBar.setLayoutParams(mlp);
                }
                navBar.setElevation(14f * d);
                navBar.setClipToOutline(true);
                navBar.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
                    }
                });
                boolean isDark = (navBar.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                final GlassDrawable bg = new GlassDrawable(d, r, isDark);
                navBar.setBackground(bg);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        navBar.setRenderEffect(RenderEffect.createBlurEffect(
                                1.4f * d, 1.4f * d, Shader.TileMode.CLAMP));
                    } catch (Throwable ignored) {}
                }
                attachSlider(navBar, bg, d);
            }
        });
    }

    private static void attachSlider(final View navBar, final GlassDrawable bg, final float d) {
        if (!(navBar instanceof ViewGroup)) return;
        final ViewGroup group = (ViewGroup) navBar;
        navBar.setOnTouchListener(new View.OnTouchListener() {
            private float downX = 0f;
            private boolean dragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                int count = group.getChildCount();
                if (count == 0) return false;
                float tw = (float) v.getWidth() / count;
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getX();
                        dragging = false;
                        bg.stretchTo(1.12f);
                        bg.movePill(e.getX() - tw * 0.42f, tw * 0.84f, false);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getX() - downX) > 8f * d) dragging = true;
                        float cx = Math.max(6f * d, Math.min(v.getWidth() - tw * 0.84f - 6f * d, e.getX() - tw * 0.42f));
                        bg.movePill(cx, tw * 0.84f, false);
                        return dragging;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        bg.stretchTo(1.0f);
                        int idx = Math.max(0, Math.min(count - 1, (int) (e.getX() / tw)));
                        bg.movePill((idx * tw) + tw * 0.08f, tw * 0.84f, true);
                        if (dragging) {
                            View tab = group.getChildAt(idx);
                            if (tab != null) tab.performClick();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private static class GlassDrawable extends android.graphics.drawable.Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lens = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lensRim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final RectF pill = new RectF();
        private final float d, r;
        private final boolean dark;
        private float px = 12f, pw = 180f, sx = 1.0f;

        GlassDrawable(float d, float r, boolean dark) {
            this.d = d; this.r = r; this.dark = dark;
            fill.setColor(dark ? Color.argb(175, 28, 28, 32) : Color.argb(190, 248, 249, 252));
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(1.3f * d);
            lens.setColor(dark ? Color.argb(65, 255, 255, 255) : Color.argb(145, 255, 255, 255));
            lensRim.setStyle(Paint.Style.STROKE);
            lensRim.setStrokeWidth(1.1f * d);
            lensRim.setColor(dark ? Color.argb(95, 255, 255, 255) : Color.argb(210, 255, 255, 255));
        }

        void movePill(float tx, float tw, boolean spring) {
            pw = tw;
            if (!spring) { px = tx; invalidateSelf(); return; }
            ValueAnimator a = ValueAnimator.ofFloat(px, tx);
            a.setDuration(300);
            a.setInterpolator(new OvershootInterpolator(1.15f));
            a.addUpdateListener(v -> { px = (Float) v.getAnimatedValue(); invalidateSelf(); });
            a.start();
        }

        void stretchTo(float ts) {
            ValueAnimator a = ValueAnimator.ofFloat(sx, ts);
            a.setDuration(160);
            a.addUpdateListener(v -> { sx = (Float) v.getAnimatedValue(); invalidateSelf(); });
            a.start();
        }

        @Override
        public void draw(Canvas c) {
            float w = getBounds().width(), h = getBounds().height();
            if (w <= 0 || h <= 0) return;
            box.set(0, 0, w, h);
            c.drawRoundRect(box, r, r, fill);
            int[] grad = dark
                ? new int[]{Color.argb(120,255,255,255), Color.argb(25,255,255,255), Color.argb(70,180,210,255)}
                : new int[]{Color.argb(235,255,255,255), Color.argb(70,255,255,255), Color.argb(150,210,225,255)};
            rim.setShader(new LinearGradient(0, 0, 0, h, grad, null, Shader.TileMode.CLAMP));
            c.drawRoundRect(box, r, r, rim);
            float pad = 6f * d, sw = pw * sx, ox = px - (sw - pw) * 0.5f;
            pill.set(ox, pad, ox + sw, h - pad);
            float lr = (h - pad * 2f) * 0.5f;
            c.drawRoundRect(pill, lr, lr, lens);
            c.drawRoundRect(pill, lr, lr, lensRim);
        }
        @Override public void setAlpha(int a) { fill.setAlpha(a); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { fill.setColorFilter(cf); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
