package net.leolink.android.ninegagtvanimation;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.LinkedList;

public class MainActivity extends Activity implements Card.OnCardMoveListener {
    private final int NUMBER_OF_CARDS = 11;
    public static final short MAX_CARDS = 4;
    private LinkedList<Integer> data;
    private LinkedList<Card> cards;

    private FrameLayout container;
    private short remainingCards = MAX_CARDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = (FrameLayout) findViewById(R.id.container);
        init();
    }

    private void init() {
        // data
        data = new LinkedList<Integer>();
        for (int i = 0; i < NUMBER_OF_CARDS; i++) data.add(i);

        // cards
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cards = new LinkedList<Card>();
        for (int i = 0; i < MAX_CARDS; i++) {
            Card c = new Card(this);
            c.setPosition(i);
            cards.add(c);
            container.addView(c, p);
        }
        for (int i = cards.size() - 1; i >= 0; i--) {
            cards.get(i).setContent(data.removeFirst());
        }
    }

    @Override
    public void onCardMoving(int position, float progress) {
        for (int i = 0; i < cards.size(); i ++) {
            if (data.isEmpty()) {
                cards.get(i).updateProgress(progress);
            } else {
                if (i != 0 && i != position) cards.get(i).updateProgress(progress);
            }
        }
    }

    @Override
    public void onCardRemoved(int position) {
        // update underlying cards
        for (int i = 0; i < cards.size(); i++) {
            if (i != position) {
                Card c = cards.get(i);
                c.setPosition(i + 1);
                // reorder views in order to reuse them
                c.bringToFront();
                c.invalidate();
            }
        }
        // handle the removed card to reuse it
        if (!data.isEmpty()) {
            Card top = cards.removeLast();
            top.setPosition(0);
            top.setContent(data.removeFirst());
            cards.addFirst(top);
        } else {
            Card top = cards.removeLast();
            cards.addFirst(top);
            container.removeView(top);
            remainingCards--;
        }
    }

    public short getRemainingCards() {
        return remainingCards;
    }

    private void log(Object obj) {
        if ( BuildConfig.DEBUG) Log.e("linhln", "linhln: " + obj);
    }
}
