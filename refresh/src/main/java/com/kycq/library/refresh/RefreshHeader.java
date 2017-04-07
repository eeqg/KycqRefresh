package com.kycq.library.refresh;

public interface RefreshHeader<StatusInfo> {
	
	int getRefreshOffsetPosition();
	
	void onRefreshScale(float scale);
	
	void onPullToRefresh();

	void onReleaseToRefresh();
	
	void onRefresh();
	
	void onRefreshComplete(StatusInfo statusInfo);
}
