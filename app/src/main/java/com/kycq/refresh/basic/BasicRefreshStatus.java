package com.kycq.refresh.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kycq.library.refresh.RefreshLayout;
import com.kycq.library.refresh.RefreshStatus;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.StatusInfo;

public class BasicRefreshStatus extends FrameLayout implements RefreshStatus<StatusInfo> {
	private TextView mTVStatus;
	
	private RefreshLayout.OnTryRefreshListener onTryRefreshListener;
	
	public BasicRefreshStatus(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		mTVStatus = (TextView) findViewById(R.id.tvStatus);
		mTVStatus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onTryRefreshListener != null) {
					onTryRefreshListener.onRefresh();
				}
			}
		});
	}
	
	@Override
	public void initOnTryRefreshListener(RefreshLayout.OnTryRefreshListener listener) {
		this.onTryRefreshListener = listener;
	}
	
	@Override
	public void onRefreshScale(float scale) {
		
	}
	
	@Override
	public void onRefreshReady() {
		mTVStatus.setText("ready");
	}
	
	@Override
	public void onRefresh() {
		mTVStatus.setText("refreshing");
		// System.out.println("refreshing");
	}
	
	@Override
	public boolean onRefreshComplete(StatusInfo statusInfo) {
		if (statusInfo == null) {
			mTVStatus.setText("refreshComplete");
			return false;
		} else {
			mTVStatus.setText(statusInfo.statusContent);
		}
		return !statusInfo.isSuccessful();
	}
	
}
