package com.benny.openlauncher.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.Home;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.DesktopCallBack;
import com.benny.openlauncher.viewutil.ItemViewFactory;

import java.util.List;

public class Dock extends CellContainer implements View.OnDragListener, DesktopCallBack {
    private AppSettings appSettings;
    public static int bottomInset;
    public View previousItemView;
    public Item previousItem;
    private float startPosX, startPosY;
    private Home home;

    public Dock(Context c) {
        this(c, null);
    }

    public Dock(Context c, AttributeSet attr) {
        super(c, attr);
    }

    @Override
    public void init() {
        appSettings = AppSettings.get();
        if (isInEditMode()) {
            return;
        }
        setOnDragListener(this);
        super.init();
    }

    public void initDockItem(Home home) {
        int columns = AppSettings.get().getDockSize();
        setGridSize(columns, 1);
        List<Item> dockItems = Home.db.getDock();

        this.home = home;
        removeAllViews();
        for (Item item : dockItems) {
            if (item.x < columns && item.y == 0) {
                addItemToPage(item, 0);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        detectSwipe(ev);
        super.dispatchTouchEvent(ev);
        return true;
    }

    private void detectSwipe(MotionEvent ev) {
        if (Home.launcher == null) return;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                Tool.print("ACTION_UP");
                float minDist = 150f;
                Tool.print((int) ev.getX(), (int) ev.getY());
                if (startPosY - ev.getY() > minDist) {
                    if (appSettings.getGestureDockSwipeUp()) {
                        Point p = Tool.convertPoint(new Point((int) ev.getX(), (int) ev.getY()), this, Home.launcher.appDrawerController);
                        Home.launcher.openAppDrawer(this, p.x, p.y);
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                Tool.print("ACTION_DOWN");
                startPosX = ev.getX();
                startPosY = ev.getY();
                break;
        }
    }

    @Override
    public boolean onDrag(View p1, DragEvent p2) {
        switch (p2.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                if (((DragAction) p2.getLocalState()).action == DragAction.Action.WIDGET) {
                    return false;
                }
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                return true;
            case DragEvent.ACTION_DROP:
                Intent intent = p2.getClipData().getItemAt(0).getIntent();
                intent.setExtrasClassLoader(Item.class.getClassLoader());
                Item item = intent.getParcelableExtra("mDragData");

                // this statement makes sure that adding an app multiple times from the app drawer works
                // the app will get a new id every time
                if (((DragAction) p2.getLocalState()).action == DragAction.Action.APP_DRAWER) {
                    item.reset();
                }

                if (addItemToPoint(item, (int) p2.getX(), (int) p2.getY())) {
                    home.desktop.consumeRevert();
                    home.dock.consumeRevert();

                    // add the item to the database
                    home.db.setItem(item, 0, 0);
                } else {
                    Point pos = touchPosToCoordinate((int) p2.getX(), (int) p2.getY(), item.spanX, item.spanY, false);
                    View itemView = coordinateToChildView(pos);
                    if (itemView != null) {
                        if (Desktop.handleOnDropOver(home, item, (Item) itemView.getTag(), itemView, this, 0, 0, this)) {
                            home.desktop.consumeRevert();
                            home.dock.consumeRevert();
                        } else {
                            Toast.makeText(getContext(), R.string.toast_not_enough_space, Toast.LENGTH_SHORT).show();
                            home.dock.revertLastItem();
                            home.desktop.revertLastItem();
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.toast_not_enough_space, Toast.LENGTH_SHORT).show();
                        home.dock.revertLastItem();
                        home.desktop.revertLastItem();
                    }
                }
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                return true;
        }
        return false;
    }

    @Override
    public void setLastItem(Object... args) {
        // args stores the item in [0] and the view reference in [1]
        View v = (View) args[1];
        Item item = (Item) args[0];

        previousItemView = v;
        previousItem = item;
        removeView(v);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            bottomInset = insets.getSystemWindowInsetBottom();
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), bottomInset);
        }
        return insets;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        int iconSize = AppSettings.get().getIconSize();
        if (appSettings.isDockShowLabel()) {
            height = Tool.dp2px(16 + iconSize + 14 + 10, getContext()) + Dock.bottomInset;
        } else {
            height = Tool.dp2px(16 + iconSize + 10, getContext()) + Dock.bottomInset;
        }
        getLayoutParams().height = height;

        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), height);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void consumeRevert() {
        previousItem = null;
        previousItemView = null;
    }

    @Override
    public void revertLastItem() {
        if (previousItemView != null) {
            addViewToGrid(previousItemView);
            previousItem = null;
            previousItemView = null;
        }
    }

    @Override
    public boolean addItemToPage(final Item item, int page) {
        int flag = appSettings.isDockShowLabel() ? ItemViewFactory.NO_FLAGS : ItemViewFactory.NO_LABEL;
        View itemView = ItemViewFactory.getItemView(getContext(), this, item, flag);

        if (itemView == null) {
            home.db.deleteItem(item);
            return false;
        } else {
            addViewToGrid(itemView, item.x, item.y, item.spanX, item.spanY);
            return true;
        }
    }

    @Override
    public boolean addItemToPoint(final Item item, int x, int y) {
        CellContainer.LayoutParams positionToLayoutPrams = coordinateToLayoutParams(x, y, item.spanX, item.spanY);
        if (positionToLayoutPrams != null) {
            item.x = positionToLayoutPrams.x;
            item.y = positionToLayoutPrams.y;

            int flag = appSettings.isDockShowLabel() ? ItemViewFactory.NO_FLAGS : ItemViewFactory.NO_LABEL;
            View itemView = ItemViewFactory.getItemView(getContext(), this, item, flag);

            if (itemView != null) {
                itemView.setLayoutParams(positionToLayoutPrams);
                addView(itemView);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addItemToCell(final Item item, int x, int y) {
        item.x = x;
        item.y = y;

        int flag = appSettings.isDockShowLabel() ? ItemViewFactory.NO_FLAGS : ItemViewFactory.NO_LABEL;
        View itemView = ItemViewFactory.getItemView(getContext(), this, item, flag);

        if (itemView != null) {
            addViewToGrid(itemView, item.x, item.y, item.spanX, item.spanY);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeItem(AppItemView view) {
        removeViewInLayout(view);
    }
}
