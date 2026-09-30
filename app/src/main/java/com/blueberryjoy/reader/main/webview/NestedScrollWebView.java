/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Based on the implementation of NestedScrollView from The Android Open Source Project
 */
package com.blueberryjoy.reader.main.webview;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.EdgeEffectCompat;

public class NestedScrollWebView extends WebView implements NestedScrollingChild3, ScaleGestureDetector.OnScaleGestureListener {

    private final CoordinatorLayoutChildHelper coordinatorLayoutChildHelper = new CoordinatorLayoutChildHelper();
    private final InternalScrollDetector internalScrollDetector = new InternalScrollDetector();

    public NestedScrollWebView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public NestedScrollWebView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NestedScrollWebView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        setCoordinatorBottomMatchingBehaviourEnabled(false);

        setBlockNestedScrollingOnInternalContentScrollsEnabled(true);

        setOverScrollMode(OVER_SCROLL_NEVER);
        initNestedScrollView(context, attrs);
    }

    /* NestedScrollView class code block.
    Following functionality has been directly extracted from NestedScrollView
    class (Android Support Library Compat 1.8.0), keeping original code unaltered as
    much as possible, this way this part could be easily updated in the future if needed:
    * NestedScrollView.onTouchEvent(@NonNull MotionEvent ev) implementation
    * NestedScrollView.computeScroll() implementation
    * NestedScrollView NestedScrollingChild3 methods implementation
    */

    private static final String TAG = "NestedScrollView";

    private OverScroller mScroller;

    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public EdgeEffect mEdgeGlowTop;

    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public EdgeEffect mEdgeGlowBottom;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionX;
    private int mLastMotionY;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flung', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts their finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    @Nullable
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;

    private int mLastScrollerX;
    private int mLastScrollerY;


    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    private NestedScrollingChildHelper mChildHelper;

    private boolean isScaling = false;

    private final ScaleGestureDetector mScaleDetector = new ScaleGestureDetector(this.getContext(), this);

    // NestedScrollView constructor
    public void initNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs/*, int defStyleAttr*/) {
        mEdgeGlowTop = EdgeEffectCompat.create(context, attrs);
        mEdgeGlowBottom = EdgeEffectCompat.create(context, attrs);

        initScrollView();

        mChildHelper = new NestedScrollingChildHelper(this);

        // ...because why else would you be using this widget?
        setNestedScrollingEnabled(true);

        // ViewCompat.setAccessibilityDelegate(getView(), ACCESSIBILITY_DELEGATE);
    }

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    /* NestedScrollView onTouchEvent */
    public void onNestedTouchEvent(@NonNull MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        final int actionMasked = ev.getActionMasked();

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
            /* Not present on NestedScrollView code. We need to reset scroll offset on touch down,
            as we found while testing that in some cases action was invoked with a non-zero scroll
            offset, breaking page scroll. */
            mScrollOffset[0] = 0;
            mScrollOffset[1] = 0;
        }

        MotionEvent motionEvent = MotionEvent.obtain(ev);
        motionEvent.offsetLocation(0, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                /* WebView always returns no children
                if (getChildCount() == 0) {
                    return false;
                }*/
                if (mIsBeingDragged) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flung and user touches, stop the fling. isFinished
                 * will be false if being flung.
                 */
                if (!mScroller.isFinished()) {
                    abortAnimatedScroll();
                }

                // Remember where the motion event started
                mLastMotionX = (int) ev.getX();
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int x = (int) ev.getX(activePointerIndex);
                final int y = (int) ev.getY(activePointerIndex);
                int deltaX = mLastMotionX - x;
                int deltaY = mLastMotionY - y;
                deltaY -= releaseVerticalGlow(deltaY, ev.getY(activePointerIndex));
                if (!mIsBeingDragged) {
                    if (Math.abs(deltaY) > mTouchSlop) {
                        final ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                        mIsBeingDragged = true;
                        if (deltaY > 0) {
                            deltaY -= mTouchSlop;
                        } else {
                            deltaY += mTouchSlop;
                        }
                    }
                    if (Math.abs(deltaX) > mTouchSlop) {
                        final ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                        mIsBeingDragged = true;
                        if (deltaX > 0) {
                            deltaX -= mTouchSlop;
                        } else {
                            deltaX += mTouchSlop;
                        }
                    }
                }
                if (mIsBeingDragged) {
                    // Start with nested pre scrolling
                    if (dispatchNestedPreScroll(deltaX, deltaY, mScrollConsumed, mScrollOffset,
                            ViewCompat.TYPE_TOUCH)) {
                        deltaX -= mScrollConsumed[0];
                        deltaY -= mScrollConsumed[1];
                        mNestedYOffset += mScrollOffset[1];
                    }

                    // Scroll to follow the motion event
                    mLastMotionX = x - mScrollOffset[0];
                    mLastMotionY = y - mScrollOffset[1];

                    final int oldX = getScrollX();
                    final int oldY = getScrollY();
                    final int range = getScrollRange();
                    final int overscrollMode = getOverScrollMode();
                    boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS
                            || (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    // Calling overScrollByCompat will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    boolean clearVelocityTracker =
                            overScrollByCompat(deltaX, deltaY, getScrollX(), getScrollY(), range, range, 0,
                                    0, true) && !hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);

                    final int scrolledDeltaX = getScrollX() - oldX;
                    final int scrolledDeltaY = getScrollY() - oldY;
                    final int unconsumedX = deltaX - scrolledDeltaX;
                    final int unconsumedY = deltaY - scrolledDeltaY;

                    mScrollConsumed[0] = 0;
                    mScrollConsumed[1] = 0;

                    dispatchNestedScroll(scrolledDeltaX, scrolledDeltaY, unconsumedX, unconsumedY, mScrollOffset,
                            ViewCompat.TYPE_TOUCH, mScrollConsumed);

                    mLastMotionX -= mScrollOffset[0];
                    mLastMotionY -= mScrollOffset[1];
                    mNestedYOffset += mScrollOffset[1];

                    if (canOverscroll) {
                        deltaY -= mScrollConsumed[1];
                        final int pulledToY = oldY + deltaY;
                        if (pulledToY < 0) {
                            EdgeEffectCompat.onPullDistance(mEdgeGlowTop,
                                    (float) -deltaY / getHeight(),
                                    ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onRelease();
                            }
                        } else if (pulledToY > range) {
                            EdgeEffectCompat.onPullDistance(mEdgeGlowBottom,
                                    (float) deltaY / getHeight(),
                                    1.f - ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onRelease();
                            }
                        }
                        if (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()) {
                            postInvalidateOnAnimation();
                            clearVelocityTracker = false;
                        }
                    }
                    if (clearVelocityTracker) {
                        // Break our velocity if we hit a scroll barrier.
                        assert mVelocityTracker != null;
                        mVelocityTracker.clear();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                assert velocityTracker != null;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocityX = (int) velocityTracker.getXVelocity(mActivePointerId);
                int initialVelocityY = (int) velocityTracker.getYVelocity(mActivePointerId);
                if ((Math.abs(initialVelocityX) >= mMinimumVelocity) || (Math.abs(initialVelocityY) >= mMinimumVelocity)) {
                    if (!edgeEffectFling(initialVelocityY)
                            && !dispatchNestedPreFling(-initialVelocityX, -initialVelocityY)) {
                        dispatchNestedFling(-initialVelocityX, -initialVelocityY, true);
                        fling(-initialVelocityX, -initialVelocityY,Math.abs(initialVelocityY) > mMinimumVelocity);
                    }
                } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0,
                        getScrollRange())) {
                    postInvalidateOnAnimation();
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                // WebView always return no children
                if (mIsBeingDragged /* && getChildCount() > 0*/) {
                    if (mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0,
                            getScrollRange())) {
                        postInvalidateOnAnimation();
                    }
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionX = (int) ev.getX(index);
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                /* Code not present on NestedScrollView code.
                Prevent crash on last motion update due an invalid pointerId. */
                if (ev.findPointerIndex(mActivePointerId) == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent for ACTION_POINTER_UP");
                    break;
                }
                /* End of code not present on NestedScrollView code. */
                mLastMotionX = (int) ev.getX(ev.findPointerIndex(mActivePointerId));
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(motionEvent);
        }
        motionEvent.recycle();
    }

    @Override
    public void computeScroll() {

        if (mScroller.isFinished()) {
            return;
        }

        mScroller.computeScrollOffset();
        final int x = mScroller.getCurrX();
        final int y = mScroller.getCurrY();
        int unconsumedX = x - mLastScrollerX;
        int unconsumedY = y - mLastScrollerY;
        mLastScrollerX = x;
        mLastScrollerY = y;

        // Nested Scrolling Pre Pass
        mScrollConsumed[0] = 0;
        mScrollConsumed[1] = 0;
        dispatchNestedPreScroll(unconsumedX, unconsumedY, mScrollConsumed, null,
                ViewCompat.TYPE_NON_TOUCH);
        unconsumedX -= mScrollConsumed[0];
        unconsumedY -= mScrollConsumed[1];

        final int range = getScrollRange();

        if (unconsumedX != 0 || unconsumedY != 0) {
            // Internal Scroll
            final int oldScrollX = getScrollX();
            final int oldScrollY = getScrollY();
            overScrollByCompat(unconsumedX, unconsumedY, oldScrollX, oldScrollY, range, range, 0, 0, false);
            final int scrolledByMeX = getScrollX() - oldScrollX;
            final int scrolledByMeY = getScrollY() - oldScrollY;
            unconsumedX -= scrolledByMeX;
            unconsumedY -= scrolledByMeY;

            // Nested Scrolling Post Pass
            mScrollConsumed[0] = 0;
            mScrollConsumed[1] = 0;
            dispatchNestedScroll(scrolledByMeX, scrolledByMeY, unconsumedX, unconsumedY, mScrollOffset,
                    ViewCompat.TYPE_NON_TOUCH, mScrollConsumed);
            unconsumedX -= mScrollConsumed[0];
            unconsumedY -= mScrollConsumed[1];
        }

        if (unconsumedX != 0 && unconsumedY != 0) {
            final int mode = getOverScrollMode();
            final boolean canOverscroll = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
            if (canOverscroll) {
                if (unconsumedY < 0) {
                    if (mEdgeGlowTop.isFinished()) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                } else {
                    if (mEdgeGlowBottom.isFinished()) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }
            abortAnimatedScroll();
        }

        if (!mScroller.isFinished()) {
            postInvalidateOnAnimation();
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void abortAnimatedScroll() {
        mScroller.abortAnimation();
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
    }

    /**
     * If either of the vertical edge glows are currently active, this consumes part or all of
     * deltaY on the edge glow.
     *
     * @param deltaY The pointer motion, in pixels, in the vertical direction, positive
     *               for moving down and negative for moving up.
     * @param x      The vertical position of the pointer.
     * @return The amount of <code>deltaY</code> that has been consumed by the
     * edge glow.
     */
    private int releaseVerticalGlow(int deltaY, float x) {
        // First allow releasing existing overscroll effect:
        float consumed = 0;
        float displacement = x / getWidth();
        float pullDistance = (float) deltaY / getHeight();
        if (EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0) {
            consumed = -EdgeEffectCompat.onPullDistance(mEdgeGlowTop, -pullDistance, displacement);
            if (EdgeEffectCompat.getDistance(mEdgeGlowTop) == 0) {
                mEdgeGlowTop.onRelease();
            }
        } else if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0) {
            consumed = EdgeEffectCompat.onPullDistance(mEdgeGlowBottom, pullDistance,
                    1 - displacement);
            if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) == 0) {
                mEdgeGlowBottom.onRelease();
            }
        }
        int pixelsConsumed = Math.round(consumed * getHeight());
        if (pixelsConsumed != 0) {
            invalidate();
        }
        return pixelsConsumed;
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
    boolean overScrollByCompat(int deltaX, int deltaY,
                               int scrollX, int scrollY,
                               int scrollRangeX, int scrollRangeY,
                               int maxOverScrollX, int maxOverScrollY,
                               boolean isTouchEvent) {
        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS
                || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS
                || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        if (clampedY && !hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)) {
            mScroller.springBack(newScrollX, newScrollY, 0, getScrollRange(), 0, getScrollRange());
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    private boolean edgeEffectFling(int velocityY) {
        boolean consumed = true;
        if (EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0) {
            mEdgeGlowTop.onAbsorb(velocityY);
        } else if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0) {
            mEdgeGlowBottom.onAbsorb(-velocityY);
        } else {
            consumed = false;
        }
        return consumed;
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityX, int velocityY,boolean participateInNestedScrolling) {
        // WebView always returns no children
        //if (getChildCount() > 0) {

        mScroller.fling(getScrollX(), getScrollY(), // start
                velocityX, velocityY, // velocities
                Integer.MIN_VALUE, Integer.MAX_VALUE, // x
                Integer.MIN_VALUE, Integer.MAX_VALUE, // y
                0, 0); // overscroll
        runAnimatedScroll(participateInNestedScrolling);
        //}
    }

    private void runAnimatedScroll(boolean participateInNestedScrolling) {
        if (participateInNestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
        mLastScrollerX = getScrollX();
        mLastScrollerY = getScrollY();
        postInvalidateOnAnimation();
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();
        stopNestedScroll(ViewCompat.TYPE_TOUCH);

        mEdgeGlowTop.onRelease();
        mEdgeGlowBottom.onRelease();
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void onSecondaryPointerUp(@NonNull MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = (int) ev.getX(newPointerIndex);
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    // NestedScrollingChild3

    @Override
    public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                     int dyUnconsumed, @Nullable int[] offsetInWindow, int type, @NonNull int[] consumed) {
        mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type, consumed);
    }

    // NestedScrollingChild2

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                                           @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, @Nullable int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                                           @Nullable int[] offsetInWindow) {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    /* End of NestedScrollView class code block. */

    /* Functions not present in original NestedScrollView implementation, but required for
    NestedScrollView implementation to work properly. */

    @Override
    public boolean overScrollBy(
            int deltaX,
            int deltaY,
            int scrollX,
            int scrollY,
            int scrollRangeX,
            int scrollRangeY,
            int maxOverScrollX,
            int maxOverScrollY,
            boolean isTouchEvent
    ) {
        if (!mIsBeingDragged || isScaling) {
            overScrollByCompat(
                    deltaX,
                    deltaY,
                    scrollX,
                    scrollY,
                    scrollRangeX,
                    scrollRangeY,
                    maxOverScrollX,
                    maxOverScrollY,
                    isTouchEvent
            );
        }
        return true;
    }

    @Override
    public int getNestedScrollAxes() {
        return ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    private int getScrollRange() {
        return computeVerticalScrollRange();
    }

    /* Extra functionalities */

    public void setCoordinatorBottomMatchingBehaviourEnabled(boolean enabled) {
        coordinatorLayoutChildHelper.setBottomMatchingBehaviourEnabled(enabled);
    }

    public void setBlockNestedScrollingOnInternalContentScrollsEnabled(boolean enabled) {
        internalScrollDetector.setEnabled(enabled);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        coordinatorLayoutChildHelper.onViewAttached(this);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        internalScrollDetector.onPageScrolled();
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        post(coordinatorLayoutChildHelper::computeBottomMarginIfNeeded);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldL, int oldT) {
        internalScrollDetector.onPageScrolled();
        super.onScrollChanged(l, t, oldL, oldT);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        if (!internalScrollDetector.onTouchEvent(event) && !isScaling) {
            onNestedTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onScale(@NonNull ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
        isScaling = true;
        return true;
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        isScaling = false;
    }
}
