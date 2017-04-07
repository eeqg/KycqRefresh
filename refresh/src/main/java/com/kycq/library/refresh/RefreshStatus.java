package com.kycq.library.refresh;

public interface RefreshStatus<StatusInfo> {
	
	void initOnTryRefreshListener(RefreshLayout.OnTryRefreshListener listener);
	
	void onRefreshScale(float scale);
	
	void onRefreshReady();
	
	void onRefresh();
	
	boolean onRefreshComplete(StatusInfo statusInfo);
}
