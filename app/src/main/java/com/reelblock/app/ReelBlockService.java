package com.reelblock.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReelBlockService extends AccessibilityService {

    static boolean isRunning = false;
    static ArrayList<String> logLines = new ArrayList<>();

    // instagram calls reels "clips" in there code so all the ids start with clips
    static final String IG = "com.instagram.android:id/";

    boolean inSingleReel = false;
    int firstReelIndex = -1;
    long lastActionTime = 0;

    @Override
    protected void onServiceConnected() {
        isRunning = true;
        addLog("blocker started");
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) {
            return;
        }
        if (!root.getPackageName().toString().equals("com.instagram.android")) {
            return;
        }

        boolean tabBarShowing = isShowing(root, "feed_tab") || isShowing(root, "clips_tab");
        boolean reelsShowing = isShowing(root, "clips_viewer_view_pager") || isShowing(root, "clips_viewer_container");
        boolean reelsTabSelected = isSelected(root, "clips_tab");

        if (tabBarShowing && (reelsTabSelected || reelsShowing)) {
            //this is the reels tab, just send them to home
            if (canAct()) {
                addLog("reels tab opened, going to home");
                goToHome(root);
            }
            inSingleReel = false;
            return;
        }

        if (reelsShowing && !inSingleReel) {
            inSingleReel = true;
            firstReelIndex = -1;
            addLog("single reel opened, its allowed");
        } else if (!reelsShowing) {
            inSingleReel = false;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED && !tabBarShowing) {
            checkScroll(event);
        }
    }

    void checkScroll(AccessibilityEvent event) {
        String id = getId(event.getSource());
        int index = event.getFromIndex();
        addLog("scroll " + id + " index " + index);

        if (!inSingleReel || id == null || index < 0) {
            return;
        }
        if (!id.contains("clips_viewer")) {
            return;
        }

        if (firstReelIndex == -1) {
            firstReelIndex = index;
        } else if (index != firstReelIndex && canAct()) {
            // they swiped to a diffrent reel. thats not allowed
            addLog("swiped to another reel, going back");
            lastActionTime = System.currentTimeMillis();
            performGlobalAction(GLOBAL_ACTION_BACK);
            inSingleReel = false;
        }
    }

    void goToHome(AccessibilityNodeInfo root) {
        lastActionTime = System.currentTimeMillis();
        List<AccessibilityNodeInfo> homeButtons = root.findAccessibilityNodeInfosByViewId(IG + "feed_tab");
        if (homeButtons.size() > 0 && homeButtons.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return;
        }
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    boolean isShowing(AccessibilityNodeInfo root, String id) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(IG + id);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isVisibleToUser()) {
                return true;
            }
        }
        return false;
    }

    boolean isSelected(AccessibilityNodeInfo root, String id) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(IG + id);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isVisibleToUser() && node.isSelected()) {
                return true;
            }
        }
        return false;
    }

    // dont spam the back button, wait a bit between actions
    boolean canAct() {
        return System.currentTimeMillis() - lastActionTime > 1500;
    }

    String getId(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        return node.getViewIdResourceName();
    }

    static void addLog(String text) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        logLines.add(0, time + "  " + text);
        if (logLines.size() > 60) {
            logLines.remove(logLines.size() - 1);
        }
    }
}
