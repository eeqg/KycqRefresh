package com.kycq.library.refresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
	/** 无效操作点 */
	private static final int INVALID_POINTER = -1;
	
	/** 状态自动判断 */
	private static final int STATUS_MODE_AUTO = 0;
	/** 状态总是显示 */
	private static final int STATUS_MODE_SHOW = 1;
	/** 状态总是隐藏 */
	private static final int STATUS_MODE_HIDE = 2;
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({STATUS_MODE_AUTO, STATUS_MODE_SHOW, STATUS_MODE_HIDE})
	public @interface StatusMode {
	}
	
	private NestedScrollingParentHelper mNestedScrollingParentHelper;
	private NestedScrollingChildHelper mNestedScrollingChildHelper;
	private int[] mConsumed = new int[2];
	private int[] mOffsetInWindow = new int[2];
	
	private int mTouchSlop;
	private boolean isJustNestedScroll;
	private boolean isNestedScrollInProgress;
	private boolean mIsUnderTouch;
	private boolean mIsBeingDragged;
	private int mActivePointerId = INVALID_POINTER;
	private int mLastMotionY;
	private Interpolator mInterpolator;
	
	private SmoothScroller mSmoothScroller;
	private int mCurrentPosition;
	private Rect mTempRect = new Rect();
	
	private Status mStatus;
	
	@StatusMode
	private int mStatusMode = STATUS_MODE_AUTO;
	private RefreshHeader mRefreshHeader;
	private View mViewHeader;
	private RefreshStatus mRefreshStatus;
	private View mViewStatus;
	private View mViewTarget;
	
	private OnRefreshListener mOnRefreshListener;
	private OnRefreshScaleListener mOnRefreshScaleListener;
	private OnTryRefreshListener mOnTryRefreshListener;
	
	public RefreshLayout(Context context) {
		this(context, null);
	}
	
	public RefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mInterpolator = new Interpolator() {
			@Override
			public float getInterpolation(float input) {
				return (1 - input) * 0.45f;
			}
		};
		mSmoothScroller = new SmoothScroller();
		
		mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
		mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
		setNestedScrollingEnabled(true);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
		setJustNestedScroll(a.getBoolean(R.styleable.RefreshLayout_refresh_justNestedScroll, false));
		int refreshHeaderLayoutId = a.getResourceId(R.styleable.RefreshLayout_refresh_viewHeader, -1);
		if (refreshHeaderLayoutId != -1) {
			View viewHeader = inflater.inflate(refreshHeaderLayoutId, this, false);
			setViewHeader(viewHeader);
		}
		int refreshStatusLayoutId = a.getResourceId(R.styleable.RefreshLayout_refresh_viewStatus, -1);
		if (refreshStatusLayoutId != -1) {
			View viewStatus = inflater.inflate(refreshStatusLayoutId, this, false);
			setViewStatus(viewStatus);
		}
		// noinspection WrongConstant
		setStatusMode(a.getInt(R.styleable.RefreshLayout_refresh_statusMode, STATUS_MODE_AUTO));
		a.recycle();
	}
	
	public void setJustNestedScroll(boolean isJustNestedScroll) {
		this.isJustNestedScroll = isJustNestedScroll;
	}
	
	public void setViewHeader(View viewHeader) {
		if (mViewHeader != null) {
			removeView(mViewHeader);
			mRefreshHeader = null;
			mViewHeader = null;
		}
		if (viewHeader == null) {
			return;
		}
		if (!(viewHeader instanceof RefreshHeader)) {
			throw new IllegalArgumentException("viewHeader must implement the RefreshHeader interface!");
		}
		
		addView(viewHeader);
		mRefreshHeader = (RefreshHeader) viewHeader;
		mViewHeader = viewHeader;
	}
	
	public void setViewStatus(View viewStatus) {
		if (mViewStatus != null) {
			removeView(mViewStatus);
			mRefreshStatus = null;
			mViewStatus = null;
		}
		if (viewStatus == null) {
			return;
		}
		if (!(viewStatus instanceof RefreshStatus)) {
			throw new IllegalArgumentException("viewStatus must implement the RefreshStatus interface!");
		}
		addView(viewStatus);
		mRefreshStatus = (RefreshStatus) viewStatus;
		mViewStatus = viewStatus;
		
		if (mOnTryRefreshListener != null) {
			mOnTryRefreshListener.refreshLayout = null;
		}
		mOnTryRefreshListener = new OnTryRefreshListener();
		mOnTryRefreshListener.refreshLayout = this;
		mRefreshStatus.initOnTryRefreshListener(mOnTryRefreshListener);
		
		initStatusMode();
	}
	
	public void setStatusMode(@StatusMode int statusMode) {
		mStatusMode = statusMode;
		initStatusMode();
	}
	
	public void setOnRefreshListener(OnRefreshListener listener) {
		mOnRefreshListener = listener;
	}
	
	public void setOnRefreshScaleListener(OnRefreshScaleListener listener) {
		mOnRefreshScaleListener = listener;
	}
	
	private void ensureTarget() {
		if (mViewTarget != null) {
			return;
		}
		
		int childCount = getChildCount();
		for (int index = 0; index < childCount; index++) {
			View child = getChildAt(index);
			if (!child.equals(mViewHeader) && !child.equals(mViewStatus)) {
				mViewTarget = child;
				break;
			}
		}
		
		initStatusMode();
	}
	
	public void swipeReady() {
		if (mStatus != Status.refreshReady) {
			ensureTarget();
			notifyRefreshReady();
			mSmoothScroller.scrollToStart();
		}
	}
	
	public void swipeRefresh() {
		swipeRefresh(false);
	}
	
	public void swipeRefresh(boolean scrollToRefresh) {
		if (mStatus != Status.refreshing) {
			ensureTarget();
			notifyRefresh();
			if (scrollToRefresh) {
				mSmoothScroller.scrollToRefresh();
			}
		}
	}
	
	public void swipeComplete() {
		swipeComplete(null);
	}
	
	public <StatusInfo> void swipeComplete(StatusInfo statusInfo) {
		if (mStatus == Status.refreshing) {
			ensureTarget();
			notifyRefreshComplete(statusInfo);
			mSmoothScroller.scrollToStart();
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mOnTryRefreshListener != null) {
			mOnTryRefreshListener.refreshLayout = null;
			mOnTryRefreshListener = null;
		}
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		ensureTarget();
		if (!isEnabled() || isJustNestedScroll || isNestedScrollInProgress || canChildScrollUp()) {
			return false;
		}
		
		int actionIndex = event.getActionIndex();
		final int action = event.getActionMasked();
		
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mIsUnderTouch = true;
				if (mIsBeingDragged = !mSmoothScroller.isFinished()) {
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
				}
				
				if (!mSmoothScroller.isFinished()) {
					mSmoothScroller.abort();
				}
				
				mActivePointerId = event.getPointerId(0);
				mLastMotionY = getMotionEventY(event, actionIndex);
				startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				break;
			case MotionEvent.ACTION_MOVE:
				mIsUnderTouch = true;
				final int activeActionIndex = event.findPointerIndex(mActivePointerId);
				if (activeActionIndex == -1) {
					break;
				}
				
				final int y = getMotionEventY(event, activeActionIndex);
				int deltaY = mLastMotionY - y;
				if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
					if (dispatchNestedScroll(0, 0, 0, deltaY, mOffsetInWindow) && mOffsetInWindow[1] != 0) {
						mIsBeingDragged = true;
						deltaY += mOffsetInWindow[1];
						mLastMotionY = y - mOffsetInWindow[1];
					}
					if (dispatchNestedPreScroll(0, deltaY, mConsumed, mOffsetInWindow)) {
						mIsBeingDragged = true;
						mLastMotionY = y - mOffsetInWindow[1];
					}
					if (!mIsBeingDragged) {
						if (deltaY < 0) {
							mIsBeingDragged = true;
						}
						mLastMotionY = y;
					}
				}
				break;
			case MotionEventCompat.ACTION_POINTER_DOWN:
				mIsUnderTouch = true;
				mActivePointerId = event.getPointerId(actionIndex);
				mLastMotionY = getMotionEventY(event, actionIndex);
				break;
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(event);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mIsUnderTouch = false;
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
				stopNestedScroll();
				break;
		}
		
		return mIsBeingDragged;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isJustNestedScroll || isNestedScrollInProgress || !isEnabled()) {
			return false;
		}
		
		int actionIndex = event.getActionIndex();
		final int action = event.getActionMasked();
		
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mIsUnderTouch = true;
				if (mIsBeingDragged = !mSmoothScroller.isFinished()) {
					final ViewParent parent = getParent();
					if (parent != null) {
						parent.requestDisallowInterceptTouchEvent(true);
					}
				}
				
				if (!mSmoothScroller.isFinished()) {
					mSmoothScroller.abort();
				}
				
				mActivePointerId = event.getPointerId(0);
				mLastMotionY = getMotionEventY(event, actionIndex);
				startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				break;
			case MotionEvent.ACTION_MOVE:
				mIsUnderTouch = true;
				final int activeActionIndex = event.findPointerIndex(mActivePointerId);
				if (activeActionIndex == -1) {
					break;
				}
				
				final int y = getMotionEventY(event, activeActionIndex);
				int deltaY = mLastMotionY - y;
				if (mCurrentPosition > 0) {
					movePosition(-deltaY);
					mLastMotionY = y;
				} else {
					if (dispatchNestedScroll(0, 0, 0, deltaY, mOffsetInWindow)) {
						deltaY += mOffsetInWindow[1];
						mLastMotionY = y - mOffsetInWindow[1];
					}
					if (!movePosition(-deltaY)) {
						if (dispatchNestedPreScroll(0, deltaY, mConsumed, mOffsetInWindow)) {
							mLastMotionY = y - mOffsetInWindow[1];
						}
					}
				}
				break;
			case MotionEventCompat.ACTION_POINTER_DOWN:
				mIsUnderTouch = true;
				mActivePointerId = event.getPointerId(actionIndex);
				mLastMotionY = getMotionEventY(event, actionIndex);
				break;
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(event);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mIsUnderTouch = false;
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
				stopNestedScroll();
				break;
		}
		
		return true;
	}
	
	private void onSecondaryPointerUp(MotionEvent event) {
		final int pointerIndex = MotionEventCompat.getActionIndex(event);
		final int pointerId = event.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mActivePointerId = event.getPointerId(newPointerIndex);
			mLastMotionY = getMotionEventY(event, newPointerIndex);
		}
	}
	
	private int getMotionEventY(MotionEvent event, int pointerIndex) {
		return (int) (event.getY(pointerIndex) + 0.5f);
	}
	
	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
	}
	
	@Override
	public boolean isNestedScrollingEnabled() {
		return mNestedScrollingChildHelper.isNestedScrollingEnabled();
	}
	
	@Override
	public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
		return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
	}
	
	@Override
	public boolean startNestedScroll(int axes) {
		boolean isNestedScroll = mNestedScrollingChildHelper.startNestedScroll(axes);
		if (isNestedScroll) {
			mIsUnderTouch = true;
		}
		return isNestedScroll;
	}
	
	@Override
	public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
		mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
		mSmoothScroller.abort();
		startNestedScroll(nestedScrollAxes);
		isNestedScrollInProgress = true;
	}
	
	@Override
	public int getNestedScrollAxes() {
		return mNestedScrollingParentHelper.getNestedScrollAxes();
	}
	
	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}
	
	@Override
	public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
		dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mOffsetInWindow);
		if (!isEnabled()) {
			return;
		}
		int delta = mOffsetInWindow[1] + dyUnconsumed;
		movePosition(-delta);
	}
	
	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
	}
	
	@Override
	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
		if (isEnabled() && mCurrentPosition > 0 && dy > 0 && movePosition(-dy)) {
			consumed[1] += dy;
		} else {
			dispatchNestedPreScroll(dx, dy, consumed, mOffsetInWindow);
		}
	}
	
	@Override
	public void onStopNestedScroll(View target) {
		mNestedScrollingParentHelper.onStopNestedScroll(target);
		isNestedScrollInProgress = false;
		mIsUnderTouch = false;
		stopNestedScroll();
	}
	
	@Override
	public void stopNestedScroll() {
		if (mRefreshHeader == null) {
			return;
		}
		
		int refreshPosition = mRefreshHeader.getRefreshOffsetPosition();
		if (mStatus != Status.refreshing
				&& mCurrentPosition >= refreshPosition) {
			notifyRefresh();
		}
		if (!mIsUnderTouch) {
			if (mStatus == Status.refreshing) {
				if (mCurrentPosition > refreshPosition) {
					mSmoothScroller.scrollToRefresh();
				} else {
					mSmoothScroller.scrollToStart();
				}
			} else if (mStatus != Status.refreshing) {
				mSmoothScroller.scrollToStart();
			}
		}
		mNestedScrollingChildHelper.stopNestedScroll();
	}
	
	protected boolean canChildScrollUp() {
		View target = mViewTarget.getVisibility() == VISIBLE ? mViewTarget : mViewStatus;
		return target != null && ViewCompat.canScrollVertically(target, -1);
	}
	
	private boolean movePosition(int delta) {
		if (mRefreshHeader == null) {
			return false;
		}
		
		int maxPosition = mViewHeader.getMeasuredHeight();
		delta = delta / 3;
		// int absDelta = Math.abs(delta);
		// absDelta = (int) (absDelta * ((mInterpolator.getInterpolation(1.0f * mCurrentPosition / maxPosition))));
		// if (absDelta == 0) {
		// 	absDelta = 1;
		// }
		// if (delta < 0) {
		// 	delta = -absDelta;
		// } else if (delta > 0) {
		// 	delta = absDelta;
		// }
		int toPosition = mCurrentPosition + delta;
		
		if (toPosition < 0) {
			if (mCurrentPosition == 0) {
				return false;
			}
			toPosition = 0;
		} else if (toPosition > maxPosition) {
			if (mCurrentPosition == maxPosition) {
				return false;
			}
			toPosition = maxPosition;
		}
		
		offsetPosition(toPosition);
		
		return true;
	}
	
	private void offsetPosition(int toPosition) {
		int lastPosition = mCurrentPosition;
		mCurrentPosition = toPosition;
		int offset = toPosition - lastPosition;
		
		int refreshOffsetPosition = mRefreshHeader.getRefreshOffsetPosition();
		
		if (mStatus != Status.refreshing) {
			if ((lastPosition == 0 && mCurrentPosition > 0)
					|| (lastPosition >= refreshOffsetPosition) && mCurrentPosition < refreshOffsetPosition) {
				notifyPullToRefresh();
			} else if (lastPosition < refreshOffsetPosition && mCurrentPosition >= refreshOffsetPosition) {
				notifyReleaseToRefresh();
			}
		}
		// else if (mStatus == Status.refreshComplete) {
		// 	if (lastPosition == 0 && mCurrentPosition > 0) {
		// 		notifyPullToRefresh();
		// 	}
		// }
		
		float scale = (float) (1.0 * mCurrentPosition / mViewHeader.getMeasuredHeight());
		if (mOnRefreshScaleListener != null) {
			mOnRefreshScaleListener.onScale(scale);
		}
		
		offsetLayout(offset, scale);
	}
	
	private void offsetLayout(int offset, float scale) {
		if (mViewHeader != null) {
			ViewCompat.offsetTopAndBottom(mViewHeader, offset);
			mRefreshHeader.onRefreshScale(scale);
		}
		if (mViewStatus != null) {
			ViewCompat.offsetTopAndBottom(mViewStatus, offset);
			mRefreshStatus.onRefreshScale(scale);
		}
		if (mViewTarget != null) {
			ViewCompat.offsetTopAndBottom(mViewTarget, offset);
		}
	}
	
	private void notifyPullToRefresh() {
		if (mRefreshHeader != null) {
			mRefreshHeader.onPullToRefresh();
		}
	}
	
	private void notifyReleaseToRefresh() {
		if (mRefreshHeader != null) {
			mRefreshHeader.onReleaseToRefresh();
		}
	}
	
	private void notifyRefreshReady() {
		if (mStatus == Status.refreshReady) {
			return;
		}
		mStatus = Status.refreshReady;
		if (mRefreshStatus != null) {
			mRefreshStatus.onRefreshReady();
		}
		toggleStatus(true);
	}
	
	private void notifyRefresh() {
		if (mStatus == Status.refreshing) {
			return;
		}
		mStatus = Status.refreshing;
		if (mOnRefreshListener != null) {
			mOnRefreshListener.onRefresh();
		}
		if (mRefreshHeader != null) {
			mRefreshHeader.onRefresh();
		}
		if (mRefreshStatus != null) {
			mRefreshStatus.onRefresh();
		}
		toggleStatus(true);
	}
	
	boolean isRefreshing() {
		return mStatus == Status.refreshing;
	}
	
	private <StatusInfo> void notifyRefreshComplete(StatusInfo statusInfo) {
		mStatus = Status.refreshComplete;
		if (mRefreshHeader != null) {
			// noinspection unchecked
			mRefreshHeader.onRefreshComplete(statusInfo);
		}
		if (mRefreshStatus != null) {
			// noinspection unchecked
			toggleStatus(mRefreshStatus.onRefreshComplete(statusInfo));
		}
	}
	
	private void initStatusMode() {
		if (mViewStatus != null) {
			if (this.mStatusMode == STATUS_MODE_SHOW) {
				mViewStatus.setVisibility(VISIBLE);
			} else if (this.mStatusMode == STATUS_MODE_HIDE) {
				mViewStatus.setVisibility(GONE);
			} else if (this.mStatusMode == STATUS_MODE_AUTO) {
				mViewStatus.setVisibility(VISIBLE);
			}
			if (isInEditMode()) {
				mViewStatus.setVisibility(GONE);
			}
		}
		
		if (mViewTarget != null) {
			if (this.mStatusMode == STATUS_MODE_SHOW) {
				mViewTarget.setVisibility(GONE);
			} else if (this.mStatusMode == STATUS_MODE_HIDE) {
				mViewTarget.setVisibility(VISIBLE);
			} else if (this.mStatusMode == STATUS_MODE_AUTO) {
				mViewTarget.setVisibility(GONE);
			}
			if (isInEditMode()) {
				mViewTarget.setVisibility(VISIBLE);
			}
		}
		
		notifyRefreshReady();
	}
	
	private void toggleStatus(boolean isShowStatus) {
		if (mViewStatus != null) {
			if (this.mStatusMode == STATUS_MODE_SHOW) {
				if (isShowStatus) {
					mViewStatus.setVisibility(VISIBLE);
				} else {
					mViewStatus.setVisibility(GONE);
				}
			} else if (this.mStatusMode == STATUS_MODE_HIDE) {
				mViewStatus.setVisibility(GONE);
			} else if (this.mStatusMode == STATUS_MODE_AUTO) {
				if (!isShowStatus && mViewStatus.getVisibility() == VISIBLE) {
					mViewStatus.setVisibility(GONE);
				}
			}
		}
		
		if (mViewTarget != null) {
			if (this.mStatusMode == STATUS_MODE_SHOW) {
				if (isShowStatus) {
					mViewTarget.setVisibility(GONE);
				} else {
					mViewTarget.setVisibility(VISIBLE);
				}
			} else if (this.mStatusMode == STATUS_MODE_HIDE) {
				mViewTarget.setVisibility(VISIBLE);
			} else if (this.mStatusMode == STATUS_MODE_AUTO) {
				if (!isShowStatus && mViewTarget.getVisibility() == GONE) {
					mViewTarget.setVisibility(VISIBLE);
				}
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		if (mViewTarget == null) {
			ensureTarget();
		}
		if (mViewTarget == null) {
			return;
		}
		
		measureHeader(widthMeasureSpec, heightMeasureSpec);
		measureStatus(widthMeasureSpec, heightMeasureSpec);
		measureTarget(widthMeasureSpec, heightMeasureSpec);
	}
	
	private void measureHeader(int widthMeasureSpec, int heightMeasureSpec) {
		if (mViewHeader == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewHeader.getLayoutParams();
		int childWidthMeasureSpec = getChildMeasureSpec(
				widthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
				lp.width
		);
		int childHeightMeasureSpec = getChildMeasureSpec(
				heightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
				lp.height
		);
		mViewHeader.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}
	
	private void measureStatus(int widthMeasureSpec, int heightMeasureSpec) {
		if (mViewStatus == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewStatus.getLayoutParams();
		int childWidthMeasureSpec = getChildMeasureSpec(
				widthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
				lp.width
		);
		int childHeightMeasureSpec = getChildMeasureSpec(
				heightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
				lp.height
		);
		mViewStatus.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}
	
	private void measureTarget(int widthMeasureSpec, int heightMeasureSpec) {
		MarginLayoutParams lp = (MarginLayoutParams) mViewTarget.getLayoutParams();
		int childWidthMeasureSpec = getChildMeasureSpec(
				widthMeasureSpec,
				getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
				lp.width
		);
		int childHeightMeasureSpec = getChildMeasureSpec(
				heightMeasureSpec,
				getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
				lp.height
		);
		mViewTarget.measure(childWidthMeasureSpec, childHeightMeasureSpec);
	}
	
	@Override
	protected boolean checkLayoutParams(LayoutParams p) {
		return p instanceof MarginLayoutParams;
	}
	
	@Override
	protected MarginLayoutParams generateDefaultLayoutParams() {
		return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}
	
	@Override
	protected MarginLayoutParams generateLayoutParams(LayoutParams p) {
		return new MarginLayoutParams(p);
	}
	
	@Override
	public MarginLayoutParams generateLayoutParams(AttributeSet attrs) {
		return new MarginLayoutParams(getContext(), attrs);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mViewTarget == null) {
			ensureTarget();
		}
		if (mViewTarget == null) {
			return;
		}
		
		layoutHeader();
		layoutStatus();
		layoutTarget();
	}
	
	private void layoutHeader() {
		if (mViewHeader == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewHeader.getLayoutParams();
		mTempRect.left = getPaddingLeft() + lp.leftMargin;
		mTempRect.top = getPaddingTop() + mCurrentPosition - lp.bottomMargin - mViewHeader.getMeasuredHeight();
		mTempRect.right = mTempRect.left + mViewHeader.getMeasuredWidth();
		mTempRect.bottom = mTempRect.top + mViewHeader.getMeasuredHeight();
		
		mViewHeader.layout(mTempRect.left, mTempRect.top, mTempRect.right, mTempRect.bottom);
	}
	
	private void layoutStatus() {
		if (mViewStatus == null) {
			return;
		}
		
		MarginLayoutParams lp = (MarginLayoutParams) mViewStatus.getLayoutParams();
		mTempRect.left = getPaddingLeft() + lp.leftMargin;
		mTempRect.top = getPaddingTop() + mCurrentPosition + lp.topMargin;
		mTempRect.right = mTempRect.left + mViewStatus.getMeasuredWidth();
		mTempRect.bottom = mTempRect.top + mViewStatus.getMeasuredHeight();
		
		mViewStatus.layout(mTempRect.left, mTempRect.top, mTempRect.right, mTempRect.bottom);
	}
	
	private void layoutTarget() {
		MarginLayoutParams lp = (MarginLayoutParams) mViewTarget.getLayoutParams();
		mTempRect.left = getPaddingLeft() + lp.leftMargin;
		mTempRect.top = getPaddingTop() + mCurrentPosition + lp.topMargin;
		mTempRect.right = mTempRect.left + mViewTarget.getMeasuredWidth();
		mTempRect.bottom = mTempRect.top + mViewTarget.getMeasuredHeight();
		
		mViewTarget.layout(mTempRect.left, mTempRect.top, mTempRect.right, mTempRect.bottom);
	}
	
	@Override
	public boolean hasNestedScrollingParent() {
		return mNestedScrollingChildHelper.hasNestedScrollingParent();
	}
	
	@Override
	public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
		return false;
	}
	
	@Override
	public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
		return false;
	}
	
	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}
	
	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
	}
	
	private class SmoothScroller extends Animation {
		private int mFromPosition;
		private int mToPosition;
		
		SmoothScroller() {
			setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					finish();
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
			});
		}
		
		void scrollToStart() {
			if (mRefreshHeader == null) {
				return;
			}
			
			int refreshOffsetPosition = mRefreshHeader.getRefreshOffsetPosition();
			
			mFromPosition = mCurrentPosition;
			mToPosition = 0;
			if (mFromPosition == mToPosition) {
				finish();
				return;
			}
			if (refreshOffsetPosition == 0) {
				setDuration(400);
			} else {
				setDuration((long) (1.0 * mFromPosition / refreshOffsetPosition * 400));
			}
			clearAnimation();
			startAnimation(this);
		}
		
		void scrollToRefresh() {
			if (mRefreshHeader == null) {
				return;
			}
			
			int refreshOffsetPosition = mRefreshHeader.getRefreshOffsetPosition();
			
			mFromPosition = mCurrentPosition;
			mToPosition = refreshOffsetPosition;
			if (mToPosition == 0) {
				mToPosition = -1;
			}
			
			if (mFromPosition == mToPosition) {
				finish();
				return;
			}
			if (refreshOffsetPosition == 0 || refreshOffsetPosition >= mViewHeader.getMeasuredHeight()) {
				setDuration(400);
			} else if (mFromPosition > mToPosition) {
				setDuration((long) (1.0 * (mFromPosition - mToPosition) / (mViewHeader.getMeasuredWidth() - refreshOffsetPosition) * 400));
			} else {
				setDuration((long) (1.0 * (mToPosition - mFromPosition) / (refreshOffsetPosition - mFromPosition) * 400));
			}
			
			clearAnimation();
			startAnimation(this);
		}
		
		void finish() {
			if (isCanceled()) {
				return;
			}
			
			// if (mStatus == Status.releaseToRefresh) {
			// 	notifyRefreshing();
			// }
			// else {
			// 	notifyPullToRefresh();
			// }
		}
		
		void abort() {
			cancel();
		}
		
		boolean isFinished() {
			return !hasStarted() || hasEnded();
		}
		
		boolean isCanceled() {
			return getStartTime() == Long.MIN_VALUE;
		}
		
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			if (mToPosition < 0) {
				mToPosition = mRefreshHeader.getRefreshOffsetPosition();
			}
			int durationPosition = mToPosition - mFromPosition;
			offsetPosition((int) (interpolatedTime * durationPosition) + mFromPosition);
		}
	}
	
	private enum Status {
		// pullToRefresh,
		// releaseToRefresh,
		refreshReady,
		refreshing,
		refreshComplete
	}
	
	public interface OnRefreshListener {
		void onRefresh();
	}
	
	public interface OnRefreshScaleListener {
		
		void onScale(float scale);
	}
	
	public static class OnTryRefreshListener {
		private RefreshLayout refreshLayout;
		
		public final void onRefresh() {
			if (this.refreshLayout == null) {
				return;
			}
			if (this.refreshLayout.mStatus == Status.refreshReady
					|| this.refreshLayout.mStatus == Status.refreshComplete) {
				this.refreshLayout.notifyRefresh();
			}
		}
	}
}
