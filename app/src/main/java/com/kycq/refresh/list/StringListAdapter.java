package com.kycq.refresh.list;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kycq.refresh.R;
import com.kycq.refresh.basic.BasicAdapter;
import com.kycq.refresh.bean.StringListBean;
import com.kycq.refresh.databinding.ItemTextListBinding;

class StringListAdapter extends BasicAdapter<StringListBean> {
	private LayoutInflater inflater;
	private boolean hasMore;
	
	StringListAdapter(Context context) {
		this.inflater = LayoutInflater.from(context);
	}
	
	void swipeResult(StringListBean stringListBean, boolean hasMore) {
		this.hasMore = hasMore;
		swipeResult(stringListBean);
	}
	
	@Override
	protected void updateAdapterInfo(@NonNull StringListBean stringListBean) {
		adapterInfo.count += stringListBean.count;
	}
	
	@Override
	public boolean hasMore() {
		return this.hasMore;
	}
	
	@Override
	public int getItemCount() {
		return this.adapterInfo != null ? this.adapterInfo.count : 0;
	}
	
	@Override
	public ItemHolder onCreateItemHolder(int viewType) {
		return new ItemHolder() {
			private ItemTextListBinding dataBinding;
			
			@Override
			protected View onCreateView(ViewGroup parent) {
				dataBinding = DataBindingUtil.inflate(
						inflater,
						R.layout.item_text_list,
						parent, false
				);
				return dataBinding.getRoot();
			}
			
			@Override
			protected void onBindView(int position) {
				dataBinding.setPosition(position);
			}
		};
	}
}
