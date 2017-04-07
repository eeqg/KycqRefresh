package com.kycq.refresh;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kycq.refresh.databinding.ActivitySplashBinding;
import com.kycq.refresh.list.RecyclerViewActivity;
import com.kycq.refresh.nested.NestedScrollViewActivity;

public class SplashActivity extends AppCompatActivity {
	private ActivitySplashBinding dataBinding;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_splash);
		
		observeNestedScrollView();
		observeRecyclerView();
	}
	
	private void observeNestedScrollView() {
		this.dataBinding.setNestedScrollViewClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(SplashActivity.this, NestedScrollViewActivity.class));
			}
		});
	}
	
	private void observeRecyclerView() {
		this.dataBinding.setRecyclerViewClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(SplashActivity.this, RecyclerViewActivity.class));
			}
		});
	}
	
}
