package com.example.pulltorefreshlistviewdemo;


import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PullToRefreshListView extends ListView implements OnScrollListener {

	private final static String TAG = PullToRefreshListView.class.getSimpleName();

	//Flag
	private final static int PULL_To_REFRESH = 0;
	private final static int RELEASE_To_REFRESH = 1;
	private final static int REFRESHING = 2;
	private final static int DONE = 3;

	private LayoutInflater inflater;

	private LinearLayout mHeaderView;
	private TextView mTipsText;
	private ImageView mArrowView;
	private ProgressBar mSpinner;
	private RotateAnimation animRotate;
	private RotateAnimation animReverseRotate;

	private boolean isRecored;

	private int mHeaderViewPaddingTop;
	private int mHeaderOrgPaddingTop;

	private int startY;
	private GestureDetector gestureDetector;

	private int mPullState;

	public OnRefreshListener refreshListener;
	public OnLastItemVisibleListener lastItemVisibleListener;
	private boolean lastItemVisible;

	private boolean isFirstItemVisible;

	public interface OnRefreshListener {
		public void onRefresh();
	}

	public interface OnLastItemVisibleListener {
		public void onLastItemVisible(int lastIndex);
	}

	public PullToRefreshListView(Context context) {
		this(context, null);
	}

	public PullToRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}


	private void init(Context context) {
		initArrowAnimation();
		initPullHeader(context);
		
		setOnScrollListener(this);
		gestureDetector = new GestureDetector(context, gestureListener);
	}

	private void initPullHeader(Context context) {
		inflater = LayoutInflater.from(context);
		mHeaderView = (LinearLayout) inflater.inflate(
				R.layout.pull_to_refresh_head, null);

		mArrowView = (ImageView) mHeaderView
				.findViewById(R.id.head_arrowImageView);
		mSpinner = (ProgressBar) mHeaderView
				.findViewById(R.id.head_progressBar);
		mTipsText = (TextView) mHeaderView.findViewById(R.id.head_tipsTextView);

		mHeaderOrgPaddingTop = mHeaderView.getPaddingTop();
		measureView(mHeaderView);
		mHeaderViewPaddingTop = -mHeaderView.getMeasuredHeight();
		setHeaderPaddingTop(mHeaderViewPaddingTop);
		mHeaderView.invalidate();
		addHeaderView(mHeaderView);		
	}
	
	private void setHeaderPaddingTop(int paddingTop) {
		mHeaderView.setPadding(mHeaderView.getPaddingLeft(), paddingTop,
				mHeaderView.getPaddingRight(), mHeaderView.getPaddingBottom());
	}

	/**
	 * initalize of the arrow animations.
	 */
	private void initArrowAnimation() {
		animRotate = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		animRotate.setInterpolator(new LinearInterpolator());
		animRotate.setDuration(100);
		animRotate.setFillAfter(true);

		animReverseRotate = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		animReverseRotate.setInterpolator(new LinearInterpolator());
		animReverseRotate.setDuration(100);
		animReverseRotate.setFillAfter(true);		
	}

	public void onScroll(AbsListView view, int firstVisiableItem,
			int visibleItemCount, int totalItemCount) {
		isFirstItemVisible = firstVisiableItem == 0? true : false;
		
		if (firstVisiableItem + visibleItemCount >= totalItemCount) {
			if (mPullState != REFRESHING && lastItemVisible == false
					&& lastItemVisibleListener != null) {
				lastItemVisible = true;
				// including Header View,here using totalItemCount - 2
				lastItemVisibleListener.onLastItemVisible(totalItemCount - 2);
			}
		} else {
			lastItemVisible = false;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	public boolean dispatchTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		}
		onTouched.onTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}
	
	private interface OnTouchEventListener {
		public void onTouchEvent(MotionEvent ev);
	}
	private OnTouchEventListener onTouched = new OnTouchEventListener() {
		@Override
		public void onTouchEvent(MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (isRecored) {
					requestDisallowInterceptTouchEvent(false);
				}
				if (mPullState != REFRESHING) {
					if (mPullState == PULL_To_REFRESH) {
						mPullState = DONE;
						changeHeaderViewByState(mPullState);
					} else if (mPullState == RELEASE_To_REFRESH) {
						mPullState = REFRESHING;
						changeHeaderViewByState(mPullState);
						onRefresh();
					}
				}
				isRecored = false;
				break;
			}
		}
	};

	private OnGestureListener gestureListener = new OnGestureListener() {
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}
		@Override
		public void onShowPress(MotionEvent e) {
		}
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			startY = (int) e1.getY();
			int tempY = (int) e2.getY();
			int deltaY = (int) (e1.getY() - e2.getY());
			if (!isRecored && isFirstItemVisible) {
				if(deltaY < 0) {
					isRecored = true;
					requestDisallowInterceptTouchEvent(true);
				}
			}
			if (mPullState != REFRESHING && isRecored) {
				int paddingTop = mHeaderView.getPaddingTop();
				if(paddingTop < 0 && paddingTop > mHeaderViewPaddingTop) {
					if(mPullState == RELEASE_To_REFRESH) {
						changeHeaderViewByState(PULL_To_REFRESH);
					}
					mPullState = PULL_To_REFRESH;
				} else if(paddingTop >= 0) {
					if(mPullState == PULL_To_REFRESH) {
						changeHeaderViewByState(RELEASE_To_REFRESH);
					}
					mPullState = RELEASE_To_REFRESH;
				}
				
				int topPadding = (int) (mHeaderViewPaddingTop - deltaY/2);
				mHeaderView.setPadding(mHeaderView.getPaddingLeft(), topPadding,
						mHeaderView.getPaddingRight(), mHeaderView.getPaddingBottom());
				mHeaderView.invalidate();
				return true;
			}
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}
	};

	public void onRefreshing() {
		mPullState = REFRESHING;
		changeHeaderViewByState(mPullState);
	}

	// Call this method to refresh the pull header
	private void changeHeaderViewByState(int state) {
		switch (state) {
		case RELEASE_To_REFRESH:
			mSpinner.setVisibility(View.GONE);
			mTipsText.setVisibility(View.VISIBLE);
			mArrowView.setVisibility(View.VISIBLE);
			mArrowView.clearAnimation();
			mArrowView.startAnimation(animRotate);
			mTipsText.setText(R.string.pull_to_refresh_release_label);
			break;
		case PULL_To_REFRESH:
			mSpinner.setVisibility(View.GONE);
			mTipsText.setVisibility(View.VISIBLE);
			mArrowView.setVisibility(View.VISIBLE);
			mArrowView.clearAnimation();
			mArrowView.startAnimation(animReverseRotate);
			mTipsText.setText(R.string.pull_to_refresh_pull_label);
			break;
		case REFRESHING:
			setHeaderPaddingTop(mHeaderOrgPaddingTop);
			mHeaderView.invalidate();

			mSpinner.setVisibility(View.VISIBLE);
			mArrowView.clearAnimation();
			mArrowView.setVisibility(View.GONE);
			mTipsText.setText(R.string.pull_to_refresh_refreshing_label);
			break;
		case DONE:
			if (mHeaderViewPaddingTop - 1 < mHeaderView.getPaddingTop()) {
				ResetAnimimation animation = new ResetAnimimation(mHeaderView,
						mHeaderViewPaddingTop, false);
				animation.setDuration(300);
				mHeaderView.startAnimation(animation);
			}
			
			mSpinner.setVisibility(View.GONE);
			mArrowView.clearAnimation();
			mArrowView.setImageResource(R.drawable.ic_pulltorefresh_arrow);

			mTipsText.setText(R.string.pull_to_refresh_pull_label);
			setSelection(0);
			break;
		}
	}

	// 点击刷新
	public void clickRefresh() {
		setSelection(0);
		mPullState = REFRESHING;
		changeHeaderViewByState(mPullState);
		onRefresh();
	}

	public void setOnRefreshListener(OnRefreshListener refreshListener) {
		this.refreshListener = refreshListener;
	}

	public void setOnLastItemVisibleListener(OnLastItemVisibleListener listener) {
		this.lastItemVisibleListener = listener;
	}

	public void onRefreshComplete(String update) {
		onRefreshComplete();
	}

	public void onRefreshComplete() {
		mPullState = DONE;
		changeHeaderViewByState(mPullState);
	}

	private void onRefresh() {
		if (refreshListener != null) {
			refreshListener.onRefresh();
		}
	}

	// 计算headView的width及height值
	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
					MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0,
					MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}
	
	public class ResetAnimimation extends Animation {
		private int targetHeight;
		private int originalHeight;
		private int extraHeight;
		private View view;
		private boolean down;
		private int viewPaddingBottom;
		private int viewPaddingRight;
		private int viewPaddingLeft;

		protected ResetAnimimation(View view, int targetHeight, boolean down) {
			this.view = view;
			this.viewPaddingLeft = view.getPaddingLeft();
			this.viewPaddingRight = view.getPaddingRight();
			this.viewPaddingBottom = view.getPaddingBottom();
			this.targetHeight = targetHeight;
			this.down = down;
			originalHeight = view.getPaddingTop();
			extraHeight = this.targetHeight - originalHeight;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {

			int newHeight;
			newHeight = (int) (targetHeight - extraHeight * (1 - interpolatedTime));
			view.setPadding(viewPaddingLeft, newHeight, 
					viewPaddingRight, viewPaddingBottom);
			view.requestLayout();
		}

		@Override
		public void initialize(int width, int height, int parentWidth,
				int parentHeight) {
			super.initialize(width, height, parentWidth, parentHeight);
		}

	}


}
