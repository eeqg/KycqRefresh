package com.kycq.refresh.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kycq.library.refresh.RefreshHeader;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.StatusInfo;

public class BasicRefreshHeader extends FrameLayout implements RefreshHeader<StatusInfo> {
	private TextView mTVStatus;
	
	public BasicRefreshHeader(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mTVStatus = (TextView) findViewById(R.id.tvStatus);
	}
	
	@Override
	public int getRefreshOffsetPosition() {
		return (int) (getMeasuredHeight() * 0.8);
	}
	
	@Override
	public void onRefreshScale(float scale) {
		
	}
	
	@Override
	public void onPullToRefresh() {
		mTVStatus.setText("pullToRefresh");
		// System.out.println("pullToRefresh");
	}
	
	@Override
	public void onReleaseToRefresh() {
		mTVStatus.setText("releaseToRefresh");
		// System.out.println("releaseToRefresh");
	}
	
	@Override
	public void onRefresh() {
		mTVStatus.setText("refreshing");
		// System.out.println("refreshing");
	}
	
	@Override
	public void onRefreshComplete(StatusInfo statusInfo) {
		mTVStatus.setText("refreshComplete");
		// System.out.println("refreshComplete");
	}
	
}
