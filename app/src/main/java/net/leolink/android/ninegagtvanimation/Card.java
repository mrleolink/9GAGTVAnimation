package net.leolink.android.ninegagtvanimation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by leolink on 6/8/14.
 */
public class Card extends FrameLayout implements View.OnTouchListener {
    private final float DIFFERENCE_SCALE = 0.1f; // 0.1f/1.0f
    private final short DIFFERENCE_HEIGHT = 25; // DP
    private final short ANIM_BOUNCING_BACK_DURATION = 350; // milliseconds
    private final short ANIM_LEAVING_DURATION = 300; // milliseconds

    private MainActivity main;
    private OnCardMoveListener listener;
    private float originalX, originalY, downContainerX, downContainerY, downX, downY, progress;
    private int rotatingDirection;
    private OvershootInterpolator interpolator;
    private AnimatorSet bouncingBackAnimation;

    private View otherView;
    private TextView textView;

    private int position;
    private float scale, coordinateY;

    public Card(Context context) {
        super(context);
        init(context);
    }

    public Card(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Card(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (context instanceof MainActivity) main = (MainActivity) context;
        if (context instanceof OnCardMoveListener) {
            listener = (OnCardMoveListener) context;
        } else {
            throw new ClassCastException("MainActivity must implement OnCardMoveListener " +
                    "interface");
        }
        View.inflate(context, R.layout.card, this);
        otherView = findViewById(R.id.other_view);
        textView = (TextView) findViewById(R.id.textview);
        setOnTouchListener(this);
        interpolator = new OvershootInterpolator();
    }

    public void setContent(int i) {
        textView.setText("Card " + i);

        // set a relevant color for this card
        switch (i % MainActivity.MAX_CARDS) {
            case 0:
                otherView.setBackgroundColor(Color.RED);
                break;
            case 1:
                otherView.setBackgroundColor(Color.BLUE);
                break;
            case 2:
                otherView.setBackgroundColor(Color.GREEN);
                break;
            case 3:
                otherView.setBackgroundColor(Color.YELLOW);
                break;
            default:
                otherView.setBackgroundColor(Color.BLACK);
                break;
        }
    }

    public void setPosition(int pos) {
        // save position
        this.position = pos;

        // setup
        if (pos == 0) pos = 1;
        coordinateY = pos * Util.dpToPx(getContext(), DIFFERENCE_HEIGHT);
        scale = 1.0f - (MainActivity.MAX_CARDS - pos) * DIFFERENCE_SCALE;
        setY(coordinateY);
        setScaleXY(scale);

        // save original coordinates
        originalX = 0;
        originalY = coordinateY;

        // reset
        setX(0);
        setRotation(0.0f);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        float containerX = getX();
        float containerY = getY();
        float eventX = event.getRawX();
        float eventY = event.getRawY();
        float halfWidth = getMeasuredWidth() / 2;
        float halfHeight = getMeasuredHeight() / 2;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (bouncingBackAnimation != null && bouncingBackAnimation.isStarted()) {
                    bouncingBackAnimation.cancel();
                }
                downX = eventX;
                downY = eventY;
                downContainerX = containerX;
                downContainerY = containerY;
                // decide rotating direction
                rotatingDirection = event.getRawY() - originalY > getMeasuredHeight() / 2
                        ? -1 : 1;
                return true;

            case MotionEvent.ACTION_MOVE:
                float moveX = eventX - downX;
                float moveY = eventY - downY;
                float x = downContainerX + moveX;
                float y = downContainerY + moveY;
                setX(x);
                setY(y);
                // rotate
                float rotate = moveX / halfWidth;
                setRotation(rotatingDirection * rotate * 5);
                // update other cards: compute progress by movedDistance / halfHeight
                progress = (float) Math.sqrt(moveX * moveX + moveY * moveY) / halfHeight;
                progress = progress > 1.0f ? 1.0f : progress;
                listener.onCardMoving(position, progress);
                return true;

            case MotionEvent.ACTION_UP:
                // if the card has been moved horizontally more than a half of its width, it will be
                // removed, otherwise, bring it back to its original position
                if (Math.abs(containerX - originalX) > halfWidth &&
                        position == MainActivity.MAX_CARDS - 1 &&
                        main != null && main.getRemainingCards() > 1) {
                    removeCard(containerX);
                } else {
                    bounceBack(containerX, containerY);
                }
                return true;

            default:
                return false;
        }
    }

    private void bounceBack(float currentX, float currentY) {
        ValueAnimator vaP = ValueAnimator.ofFloat(progress, 0.0f);
        vaP.setInterpolator(interpolator);
        vaP.setDuration(ANIM_BOUNCING_BACK_DURATION);
        vaP.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (Float) valueAnimator.getAnimatedValue();
                listener.onCardMoving(position, value);
            }
        });
        ObjectAnimator oaX = ObjectAnimator.ofFloat(this, "x", currentX, originalX);
        oaX.setInterpolator(interpolator);
        oaX.setDuration(ANIM_BOUNCING_BACK_DURATION);
        ObjectAnimator oaY = ObjectAnimator.ofFloat(this, "y", currentY, originalY);
        oaY.setInterpolator(interpolator);
        oaY.setDuration(ANIM_BOUNCING_BACK_DURATION);
        ObjectAnimator oaR = ObjectAnimator.ofFloat(this, "rotation", getRotation(), 0.0f);
        oaR.setInterpolator(interpolator);
        oaR.setDuration(ANIM_BOUNCING_BACK_DURATION);
        bouncingBackAnimation = new AnimatorSet();
        bouncingBackAnimation.play(oaX).with(oaY).with(oaR).with(vaP);
        bouncingBackAnimation.start();
    }

    private void removeCard(float currentX) {
        float width = getMeasuredWidth();
        float desX = originalX + width / 2 +
                currentX > originalX + width / 2 ?
                width :
                -width;
        ObjectAnimator oaX = ObjectAnimator.ofFloat(this, "x", currentX, desX);
        oaX.setInterpolator(interpolator);
        oaX.setDuration(ANIM_LEAVING_DURATION);
        oaX.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                // not used
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                listener.onCardRemoved(position);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                // not used
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                // not used
            }
        });
        oaX.start();
    }

    public void updateProgress(float progress) {
        setY(coordinateY + progress * Util.dpToPx(getContext(), DIFFERENCE_HEIGHT));
        setScaleXY(scale + progress * DIFFERENCE_SCALE);
    }

    private void setScaleXY(float scale) {
        setScaleX(scale);
        setScaleY(scale);
    }

    public interface OnCardMoveListener {
        public void onCardMoving(int position, float progress);
        public void onCardRemoved(int position);
    }

    private void log(Object obj) {
        if ( BuildConfig.DEBUG) Log.e("linhln", "linhln: " + obj);
    }
}
