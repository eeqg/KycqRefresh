package com.kycq.refresh.list;

import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import com.kycq.library.refresh.RecyclerAdapter;
import com.kycq.refresh.R;
import com.kycq.refresh.bean.StatusInfo;
import com.kycq.refresh.bean.StringListBean;
import com.kycq.refresh.databinding.ActivityRecyclerViewBinding;
import com.kycq.refresh.databinding.DialogCompleteBinding;
import com.kycq.refresh.databinding.DialogSettingBinding;

public class RecyclerViewActivity extends AppCompatActivity {
	private ActivityRecyclerViewBinding dataBinding;
	
	private StringListAdapter stringListAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_recycler_view);
		
		observeStringList();
	}
	
	private void observeStringList() {
		this.dataBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
		this.dataBinding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
		
		this.stringListAdapter = new StringListAdapter(this);
		this.stringListAdapter.setRefreshLayout(this.dataBinding.refreshLayout);
		this.stringListAdapter.setRecyclerView(this.dataBinding.recyclerView);
		this.stringListAdapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(RecyclerAdapter.ItemHolder itemHolder) {
				
			}
		});
		this.stringListAdapter.setOnTaskListener(new RecyclerAdapter.OnTaskListener<Integer>() {
			@Override
			public Integer onTask() {
				return stringListAdapter.currentPage;
			}
			
			@Override
			public void onCancel(Integer integer) {
				
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.id.setting, Menu.NONE, R.string.setting).setIcon(R.drawable.ic_settings);
		menu.add(Menu.NONE, R.id.refreshReady, Menu.NONE, R.string.refresh_ready);
		menu.add(Menu.NONE, R.id.refreshing, Menu.NONE, R.string.refreshing);
		menu.add(Menu.NONE, R.id.completeSuccess, Menu.NONE, R.string.complete_success);
		menu.add(Menu.NONE, R.id.completeFailure, Menu.NONE, R.string.complete_failure);
		menu.add(Menu.NONE, R.id.completeError, Menu.NONE, R.string.complete_error);
		menu.add(Menu.NONE, R.id.completeCustom, Menu.NONE, R.string.complete_custom);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.refreshing).setEnabled(false);
		menu.findItem(R.id.completeSuccess).setEnabled(false);
		menu.findItem(R.id.completeFailure).setEnabled(false);
		menu.findItem(R.id.completeError).setEnabled(false);
		menu.findItem(R.id.completeCustom).setEnabled(false);
		
		if (this.stringListAdapter.getStatus() == RecyclerAdapter.REFRESH_READY) {
			menu.findItem(R.id.refreshing).setEnabled(true);
			menu.findItem(R.id.completeSuccess).setEnabled(true);
			menu.findItem(R.id.completeFailure).setEnabled(true);
			menu.findItem(R.id.completeError).setEnabled(true);
			menu.findItem(R.id.completeCustom).setEnabled(true);
			return true;
		} else if (this.stringListAdapter.getStatus() == RecyclerAdapter.REFRESHING) {
			menu.findItem(R.id.completeSuccess).setEnabled(true);
			menu.findItem(R.id.completeFailure).setEnabled(true);
			menu.findItem(R.id.completeError).setEnabled(true);
			menu.findItem(R.id.completeCustom).setEnabled(true);
			return true;
		} else if (this.stringListAdapter.getStatus() == RecyclerAdapter.REFRESH_COMPLETE) {
			menu.findItem(R.id.refreshing).setEnabled(true);
			return true;
		} else if (this.stringListAdapter.getStatus() == RecyclerAdapter.LOAD_READY) {
			menu.findItem(R.id.refreshing).setEnabled(true);
			return true;
		} else if (this.stringListAdapter.getStatus() == RecyclerAdapter.LOADING) {
			menu.findItem(R.id.refreshing).setEnabled(true);
			menu.findItem(R.id.completeSuccess).setEnabled(true);
			menu.findItem(R.id.completeFailure).setEnabled(true);
			menu.findItem(R.id.completeError).setEnabled(true);
			menu.findItem(R.id.completeCustom).setEnabled(true);
			return true;
		} else if (this.stringListAdapter.getStatus() == RecyclerAdapter.LOAD_COMPLETE) {
			menu.findItem(R.id.refreshing).setEnabled(true);
			return true;
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.setting) {
			showSettingDialog();
		} else if (item.getItemId() == R.id.refreshReady) {
			this.stringListAdapter.swipeRefreshReady();
		} else if (item.getItemId() == R.id.refreshing) {
			this.stringListAdapter.swipeRefresh();
		} else if (item.getItemId() == R.id.completeSuccess) {
			StringListBean stringListBean = new StringListBean();
			stringListBean.count = 10;
			this.stringListAdapter.swipeResult(stringListBean, true);
		} else if (item.getItemId() == R.id.completeFailure) {
			StringListBean stringListBean = new StringListBean();
			stringListBean.statusInfo.statusCode = StatusInfo.FAILURE;
			stringListBean.statusInfo.statusContent = "";
			this.stringListAdapter.swipeResult(stringListBean);
		} else if (item.getItemId() == R.id.completeError) {
			this.stringListAdapter.swipeResult(null);
		} else if (item.getItemId() == R.id.completeCustom) {
			showCompleteDialog();
		} else {
			return false;
		}
		return true;
	}
	
	private void showSettingDialog() {
		final DialogSettingBinding dataBinding = DataBindingUtil.inflate(
				LayoutInflater.from(this),
				R.layout.dialog_setting,
				null, false
		);
		new AlertDialog.Builder(this)
				.setTitle("设置状态模式")
				.setView(dataBinding.getRoot())
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int checkId = dataBinding.rgStatusMode.getCheckedRadioButtonId();
						if (checkId == R.id.rbAuto) {
							
						} else if (checkId == R.id.rbShow) {
							
						} else if (checkId == R.id.rbHide) {
							
						}
					}
				})
				.show();
	}
	
	private void showCompleteDialog() {
		final DialogCompleteBinding dataBinding = DataBindingUtil.inflate(
				LayoutInflater.from(this),
				R.layout.dialog_complete,
				null, false
		);
		new AlertDialog.Builder(this)
				.setTitle("自定义参数")
				.setView(dataBinding.getRoot())
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String countStr = dataBinding.editCount.getText().toString().trim();
						if (countStr.length() <= 0) {
							return;
						}
						
						dialog.dismiss();
						
						StringListBean stringListBean = new StringListBean();
						stringListBean.count = Integer.parseInt(countStr);
						stringListAdapter.swipeResult(stringListBean, dataBinding.cbNeedLoad.isChecked());
					}
				})
				.show();
	}
}
