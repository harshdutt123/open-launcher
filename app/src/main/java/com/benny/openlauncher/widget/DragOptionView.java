package com.benny.openlauncher.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.Home;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.viewutil.DialogHelper;
import com.benny.openlauncher.util.DragAction;
import com.benny.openlauncher.util.Tool;

public class DragOptionView extends CardView {
    public boolean isDraggedFromDrawer = false;
    public boolean dragging = false;
    private View[] hideViews;
    private LinearLayout dragOptions;
    private TextView editIcon;
    private TextView removeIcon;
    private TextView infoIcon;
    private TextView deleteIcon;
    private Home home;
    private Long animSpeed = 120L;

    public DragOptionView(Context context) {
        super(context);
        init();
    }

    public DragOptionView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    public void setHome(Home home) {
        this.home = home;
    }

    public void setAutoHideView(View... v) {
        hideViews = v;
    }

    public void resetAutoHideView() {
        hideViews = null;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            ((MarginLayoutParams) getLayoutParams()).topMargin = insets.getSystemWindowInsetTop() + Tool.dp2px(14,getContext());
        }
        return insets;
    }

    private void init() {
        setCardElevation(Tool.dp2px(4, getContext()));
        setRadius(Tool.dp2px(2, getContext()));

        dragOptions = (LinearLayout) ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_drag_option, this, false);
        addView(dragOptions);

        editIcon = (TextView) dragOptions.findViewById(R.id.editIcon);
        editIcon.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(final View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        if (((DragAction) dragEvent.getLocalState()).action == DragAction.Action.APP_DRAWER) {
                            return false;
                        }
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;
                    case DragEvent.ACTION_DROP:
                        Intent intent = dragEvent.getClipData().getItemAt(0).getIntent();
                        intent.setExtrasClassLoader(Item.class.getClassLoader());
                        final Item item = intent.getParcelableExtra("mDragData");

                        DialogHelper.editItemDialog("Edit Item", item.name, getContext(), new DialogHelper.onItemEditListener() {
                            @Override
                            public void itemLabel(String label) {
                                item.name = label;
                                Home.db.updateItem(item);

                                Home.launcher.desktop.addItemToCell(item, item.x, item.y);
                                Home.launcher.desktop.removeItem((AppItemView) Home.launcher.desktop.getCurrentPage().coordinateToChildView(new Point(item.x, item.y)));
                            }
                        });
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        return true;
                }
                return false;
            }
        });
        removeIcon = (TextView) dragOptions.findViewById(R.id.removeIcon);
        removeIcon.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        switch (((DragAction) dragEvent.getLocalState()).action) {
                            case GROUP:
                            case APP:
                            case WIDGET:
                            case SHORTCUT:
                            case APP_DRAWER:
                            case ACTION:
                                return true;
                        }
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;
                    case DragEvent.ACTION_DROP:
                        Intent intent = dragEvent.getClipData().getItemAt(0).getIntent();
                        intent.setExtrasClassLoader(Item.class.getClassLoader());
                        Item item = intent.getParcelableExtra("mDragData");

                        // remove all items from the database
                        Home.launcher.db.deleteItem(item);
                        if (item.type == Item.Type.GROUP) {
                            for (Item i : item.items) {
                                Home.launcher.db.deleteItem(i);
                            }
                        }

                        home.desktop.consumeRevert();
                        home.dock.consumeRevert();
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        return true;
                }
                return false;
            }
        });
        infoIcon = (TextView) dragOptions.findViewById(R.id.infoIcon);
        infoIcon.setOnDragListener(new OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        switch (((DragAction) dragEvent.getLocalState()).action) {
                            case APP_DRAWER:
                            case APP:
                                return true;
                        }
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;
                    case DragEvent.ACTION_DROP:
                        Intent intent = dragEvent.getClipData().getItemAt(0).getIntent();
                        intent.setExtrasClassLoader(Item.class.getClassLoader());
                        Item item = intent.getParcelableExtra("mDragData");
                        if (item.type == Item.Type.APP) {
                            try {
                                getContext().startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + item.intent.getComponent().getPackageName())));
                            } catch (Exception e) {
                                Tool.toast(getContext(), R.string.toast_app_uninstalled);
                            }
                        }
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        return true;
                }
                return false;
            }
        });
        deleteIcon = (TextView) dragOptions.findViewById(R.id.deleteIcon);
        deleteIcon.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        switch (((DragAction) dragEvent.getLocalState()).action) {
                            case APP_DRAWER:
                            case APP:
                                return true;
                        }
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;
                    case DragEvent.ACTION_DROP:
                        DialogHelper.deletePackageDialog(getContext(), dragEvent);
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        return true;
                }
                return false;
            }
        });

        editIcon.setText(editIcon.getText(), TextView.BufferType.SPANNABLE);
        removeIcon.setText(removeIcon.getText(), TextView.BufferType.SPANNABLE);
        infoIcon.setText(infoIcon.getText(), TextView.BufferType.SPANNABLE);
        deleteIcon.setText(deleteIcon.getText(), TextView.BufferType.SPANNABLE);

        for (int i = 0; i < dragOptions.getChildCount(); i++) {
            dragOptions.getChildAt(i).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean dispatchDragEvent(DragEvent ev) {
        final DragEvent event=ev;
        boolean r = super.dispatchDragEvent(ev);
        if (r && (ev.getAction() == DragEvent.ACTION_DRAG_STARTED || ev.getAction() == DragEvent.ACTION_DRAG_ENDED)) {
            // If we got a start or end and the return value is true, our
            // onDragEvent wasn't called by ViewGroup.dispatchDragEvent
            // So we do it here.
            this.post(new Runnable() {
                @Override
                public void run() {
                    onDragEvent(event);
                }
            });


            // fix crash on older versions of android
            try {
                super.dispatchDragEvent(ev);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        return r;
    }

    private void animShowView() {
        if (hideViews != null) {
            isDraggedFromDrawer = true;
            Tool.invisibleViews(Math.round(animSpeed / 1.3f), hideViews);
            animate().alpha(1);
        }
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                dragging = true;
                animShowView();
                home.dock.setHideGrid(false);
                for (CellContainer cellContainer : home.desktop.pages) {
                    cellContainer.setHideGrid(false);
                }
                switch (((DragAction) event.getLocalState()).action) {
                    case ACTION:
                        editIcon.setVisibility(View.VISIBLE);
                        removeIcon.setVisibility(View.VISIBLE);
                        return true;
                    case APP:
                        editIcon.setVisibility(View.VISIBLE);
                        removeIcon.setVisibility(View.VISIBLE);
                        infoIcon.setVisibility(View.VISIBLE);
                        deleteIcon.setVisibility(View.VISIBLE);
                    case APP_DRAWER:
                        removeIcon.setVisibility(View.VISIBLE);
                        infoIcon.setVisibility(View.VISIBLE);
                        deleteIcon.setVisibility(View.VISIBLE);
                        return true;
                    case WIDGET:
                        removeIcon.setVisibility(View.VISIBLE);
                        return true;
                    case GROUP:
                        editIcon.setVisibility(View.VISIBLE);
                        removeIcon.setVisibility(View.VISIBLE);
                        return true;
                    case SHORTCUT:
                        removeIcon.setVisibility(View.VISIBLE);
                        return true;
                }
            case DragEvent.ACTION_DRAG_ENTERED:
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                return true;
            case DragEvent.ACTION_DROP:
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                dragging = false;
                home.dock.setHideGrid(true);
                for (CellContainer cellContainer : home.desktop.pages) {
                    cellContainer.setHideGrid(true);
                }

                animate().alpha(0);
                editIcon.setVisibility(View.GONE);
                removeIcon.setVisibility(View.GONE);
                infoIcon.setVisibility(View.GONE);
                deleteIcon.setVisibility(View.GONE);

                Tool.visibleViews(Math.round(animSpeed / 1.3f), hideViews);

                // the search view might be disabled
                Home.launcher.updateSearchBar(true);

                isDraggedFromDrawer = false;

                home.dock.revertLastItem();
                home.desktop.revertLastItem();
                return true;
        }
        return false;
    }
}
