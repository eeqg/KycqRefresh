package com.kycq.refresh.nested;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kycq.library.refresh.RefreshLayout;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.StatusInfo;
import com.kycq.refresh.databinding.ActivityNestedScrollViewBinding;

public class NestedScrollViewActivity extends AppCompatActivity implements Handler.Callback {
	private ActivityNestedScrollViewBinding dataBinding;
	
	private Handler handler = new Handler(this);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_nested_scroll_view);
		
		this.dataBinding.setRefreshClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dataBinding.refreshLayout.swipeRefresh();
			}
		});
		this.dataBinding.setCompleteClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				StatusInfo statusInfo = new StatusInfo();
				dataBinding.refreshLayout.swipeComplete(statusInfo);
			}
		});
		this.dataBinding.refreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				handler.sendEmptyMessageDelayed(0, 3000);
			}
		});
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		StatusInfo statusInfo = new StatusInfo();
		statusInfo.statusContent = "success";
		dataBinding.refreshLayout.swipeComplete(statusInfo);
		return true;
	}
}
