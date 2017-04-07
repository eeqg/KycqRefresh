package com.kycq.refresh.basic;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kycq.library.refresh.RecyclerAdapter;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.BasicBean;
import com.kycq.refresh.bean.StatusInfo;
import com.kycq.refresh.databinding.ItemRefreshListBinding;

public abstract class BasicAdapter<AdapterInfo extends BasicBean> extends RecyclerAdapter<StatusInfo> {
	private final int initPage = 0;
	public int currentPage = initPage;
	
	protected AdapterInfo adapterInfo;
	
	@Override
	public void swipeRefreshReady() {
		this.currentPage = this.initPage;
		resetAdapterInfo(null);
		super.swipeRefreshReady();
	}
	
	@Override
	public void swipeRefresh() {
		this.currentPage = this.initPage;
		super.swipeRefresh();
	}
	
	@Override
	public void swipeComplete(StatusInfo statusInfo) {
		this.currentPage++;
		super.swipeComplete(statusInfo);
	}
	
	public void swipeResult(AdapterInfo adapterInfo) {
		StatusInfo statusInfo = adapterInfo != null ? adapterInfo.statusInfo : null;
		boolean isRefreshing = this.currentPage == initPage;
		if (statusInfo == null) {
			if (isRefreshing) {
				resetAdapterInfo(null);
			}
			swipeComplete(null);
		} else if (statusInfo.isSuccessful()) {
			if (isRefreshing) {
				resetAdapterInfo(adapterInfo);
			} else {
				int oldItemCount = getItemCount();
				updateAdapterInfo(adapterInfo);
				int newItemCount = getItemCount();
				notifyItemRangeInserted(oldItemCount, newItemCount - oldItemCount);
			}
			swipeComplete(statusInfo);
			if (hasMore()) {
				swipeLoadReady();
			}
		} else {
			swipeComplete(statusInfo);
		}
	}
	
	private void resetAdapterInfo(AdapterInfo adapterInfo) {
		this.adapterInfo = adapterInfo;
	}
	
	protected abstract void updateAdapterInfo(@NonNull AdapterInfo adapterInfo);
	
	public Object getItem(int position) {
		return null;
	}
	
	public abstract boolean hasMore();
	
	@Override
	public RefreshHolder<StatusInfo> onCreateRefreshHolder() {
		return new RefreshHolder<StatusInfo>() {
			private ItemRefreshListBinding dataBinding;
			
			@Override
			protected View onCreateView(ViewGroup parent) {
				dataBinding = DataBindingUtil.inflate(
						LayoutInflater.from(parent.getContext()),
						R.layout.item_refresh_list,
						parent, false
				);
				return dataBinding.getRoot();
			}
			
			@Override
			protected void onRefreshReady() {
				dataBinding.setContent("refreshReady");
			}
			
			@Override
			protected void onRefreshing() {
				dataBinding.setContent("refreshing");
			}
			
			@Override
			protected void onRefreshComplete(StatusInfo statusInfo) {
				if (statusInfo == null) {
					dataBinding.setContent("networkError");
				} else if (!statusInfo.isSuccessful()) {
					dataBinding.setContent("refreshFailure");
				} else {
					dataBinding.setContent("listEmpty");
				}
			}
		};
	}
	
	@Override
	public LoadHolder<StatusInfo> onCreateLoadHolder() {
		return new LoadHolder<StatusInfo>() {
			private TextView tvStatus;
			
			@Override
			protected View onCreateView(ViewGroup parent) {
				return LayoutInflater.from(parent.getContext()).inflate(
						R.layout.item_loading_list,
						parent, false
				);
			}
			
			@Override
			protected void onViewCreated(View view) {
				tvStatus = (TextView) view.findViewById(R.id.tvStatus);
			}
			
			@Override
			protected void onLoadReady() {
				tvStatus.setText(R.string.load_ready);
			}
			
			@Override
			protected void onLoading() {
				tvStatus.setText(R.string.loading);
			}
			
			@Override
			protected boolean onLoadComplete(StatusInfo statusInfo) {
				if (statusInfo == null) {
					tvStatus.setText(R.string.network_error);
					return true;
				} else if (!statusInfo.isSuccessful()) {
					tvStatus.setText(R.string.load_failure);
					return true;
				} else {
					tvStatus.setText(R.string.load_complete);
					return false;
				}
			}
		};
	}
}
