package com.kycq.library.refresh;

import android.support.annotation.IntDef;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class RecyclerAdapter<StatusInfo> {
	/** 刷新准备 */
	public static final int REFRESH_READY = 1;
	/** 刷新中 */
	public static final int REFRESHING = 2;
	/** 刷新结束 */
	public static final int REFRESH_COMPLETE = 3;
	/** 加载准备 */
	public static final int LOAD_READY = 4;
	/** 加载中 */
	public static final int LOADING = 5;
	/** 加载结束 */
	public static final int LOAD_COMPLETE = 6;
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({REFRESH_READY,
			REFRESHING,
			REFRESH_COMPLETE,
			LOAD_READY,
			LOADING,
			LOAD_COMPLETE})
	public @interface Status {
	}
	
	private RefreshLayout refreshLayout;
	private OnRecyclerRefreshListener onRecyclerRefreshListener;
	
	private WrapRecyclerAdapter wrapRecyclerAdapter;
	private RecyclerView recyclerView;
	private OnRecyclerScrollListener onRecyclerScrollListener;
	
	private OnTaskListener onTaskListener;
	
	private
	@Status
	int status = REFRESH_READY;
	
	private OnItemClickListener onItemClickListener;
	private OnItemLongClickListener onItemLongClickListener;
	
	private StatusInfo statusInfo;
	private boolean autoLoad;
	
	public void setRefreshLayout(RefreshLayout refreshLayout) {
		if (this.onRecyclerRefreshListener == null) {
			this.onRecyclerRefreshListener = new OnRecyclerRefreshListener(this);
		}
		
		this.refreshLayout = refreshLayout;
		this.refreshLayout.setOnRefreshListener(this.onRecyclerRefreshListener);
	}
	
	public void setRecyclerView(RecyclerView recyclerView) {
		if (this.onRecyclerScrollListener == null) {
			this.onRecyclerScrollListener = new OnRecyclerScrollListener(this);
		}
		if (this.wrapRecyclerAdapter == null) {
			this.wrapRecyclerAdapter = new WrapRecyclerAdapter(this);
		}
		if (this.recyclerView != null) {
			this.recyclerView.removeOnScrollListener(this.onRecyclerScrollListener);
		}
		this.recyclerView = recyclerView;
		if (this.recyclerView == null) {
			return;
		}
		
		this.recyclerView.setAdapter(this.wrapRecyclerAdapter);
		this.recyclerView.addOnScrollListener(this.onRecyclerScrollListener);
	}
	
	public <Task> void setOnTaskListener(OnTaskListener<Task> listener) {
		this.onTaskListener = listener;
	}
	
	public void setOnItemClickListener(OnItemClickListener listener) {
		this.onItemClickListener = listener;
	}
	
	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		this.onItemLongClickListener = listener;
	}
	
	public void swipeRefreshReady() {
		this.status = REFRESH_READY;
		if (this.refreshLayout != null) {
			this.refreshLayout.swipeReady();
		}
		if (this.onTaskListener != null) {
			this.onTaskListener.cancel();
		}
		
		notifyDataSetChanged();
	}
	
	public void swipeRefresh() {
		if (this.status == REFRESHING) {
			return;
		}
		this.status = REFRESHING;
		if (this.refreshLayout != null) {
			this.refreshLayout.swipeRefresh();
		}
		notifyDataSetChanged();
		if (this.onTaskListener != null) {
			this.onTaskListener.notifyRefresh();
		}
	}
	
	public void swipeLoadReady() {
		swipeLoadReady(true);
	}
	
	public void swipeLoadReady(boolean autoLoad) {
		this.autoLoad = autoLoad;
		this.status = LOAD_READY;
		if (this.onTaskListener != null) {
			this.onTaskListener.cancel();
		}
		
		notifyItemChanged(getItemCount());
	}
	
	private void swipeLoading(boolean reload) {
		if (!reload && this.status != LOAD_READY) {
			return;
		}
		this.status = LOADING;
		if (this.onTaskListener != null) {
			this.onTaskListener.notifyLoading();
		}
		
		notifyItemChanged(getItemCount());
	}
	
	public void swipeComplete(StatusInfo statusInfo) {
		if (this.status == REFRESHING) {
			this.statusInfo = statusInfo;
			this.status = REFRESH_COMPLETE;
			if (this.refreshLayout != null) {
				this.refreshLayout.swipeComplete(statusInfo);
			}
			if (this.onTaskListener != null) {
				this.onTaskListener.cancel();
			}
			
			notifyDataSetChanged();
		} else if (this.status == LOADING || this.status == LOAD_READY) {
			this.statusInfo = statusInfo;
			this.status = LOAD_COMPLETE;
			if (this.onTaskListener != null) {
				this.onTaskListener.cancel();
			}
			
			notifyItemChanged(getItemCount());
		} else {
			this.statusInfo = statusInfo;
			this.status = REFRESH_COMPLETE;
			if (this.refreshLayout != null) {
				this.refreshLayout.swipeComplete(statusInfo);
			}
			if (this.onTaskListener != null) {
				this.onTaskListener.cancel();
			}
			
			notifyDataSetChanged();
		}
	}
	
	public
	@Status
	int getStatus() {
		return this.status;
	}
	
	public abstract int getItemCount();
	
	public int getItemType(int position) {
		return 0;
	}
	
	public long getItemId(int position) {
		return View.NO_ID;
	}
	
	public abstract ItemHolder onCreateItemHolder(int viewType);
	
	public abstract RefreshHolder<StatusInfo> onCreateRefreshHolder();
	
	public abstract LoadHolder<StatusInfo> onCreateLoadHolder();
	
	public void onViewRecycled(ItemHolder holder) {
	}
	
	public boolean onFailedToRecycleView(ItemHolder holder) {
		return false;
	}
	
	public void onViewAttachedToWindow(ItemHolder holder) {
	}
	
	public void onViewDetachedFromWindow(ItemHolder holder) {
	}
	
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
	}
	
	public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
	}
	
	public void setHasStableIds(boolean hasStableIds) {
		wrapRecyclerAdapter.setHasStableIds(hasStableIds);
	}
	
	public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
		wrapRecyclerAdapter.registerAdapterDataObserver(observer);
	}
	
	public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
		wrapRecyclerAdapter.unregisterAdapterDataObserver(observer);
	}
	
	public final void notifyDataSetChanged() {
		wrapRecyclerAdapter.notifyDataSetChanged();
	}
	
	public final void notifyItemChanged(int position) {
		wrapRecyclerAdapter.notifyItemRangeChanged(position, 1);
	}
	
	public final void notifyItemChanged(int position, Object payload) {
		wrapRecyclerAdapter.notifyItemRangeChanged(position, 1, payload);
	}
	
	public final void notifyItemRangeChanged(int positionStart, int itemCount) {
		wrapRecyclerAdapter.notifyItemRangeChanged(positionStart, itemCount);
	}
	
	public final void notifyItemRangeChanged(int positionStart, int itemCount, Object payload) {
		wrapRecyclerAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
	}
	
	public final void notifyItemInserted(int position) {
		wrapRecyclerAdapter.notifyItemRangeInserted(position, 1);
	}
	
	public final void notifyItemMoved(int fromPosition, int toPosition) {
		wrapRecyclerAdapter.notifyItemMoved(fromPosition, toPosition);
	}
	
	public final void notifyItemRangeInserted(int positionStart, int itemCount) {
		wrapRecyclerAdapter.notifyItemRangeInserted(positionStart, itemCount);
	}
	
	public final void notifyItemRemoved(int position) {
		wrapRecyclerAdapter.notifyItemRangeRemoved(position, 1);
	}
	
	public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
		wrapRecyclerAdapter.notifyItemRangeRemoved(positionStart, itemCount);
	}
	
	public static abstract class ItemHolder extends RecyclerHolder
			implements View.OnClickListener, View.OnLongClickListener {
		
		protected abstract void onBindView(int position);
		
		@Override
		public void onClick(View v) {
			if (recyclerAdapter.onItemClickListener != null) {
				recyclerAdapter.onItemClickListener.onItemClick(this);
			}
		}
		
		@Override
		public boolean onLongClick(View v) {
			return recyclerAdapter.onItemLongClickListener != null
					&& recyclerAdapter.onItemLongClickListener.onItemLongClick(this);
		}
	}
	
	public static abstract class RefreshHolder<StatusInfo> extends RecyclerHolder
			implements View.OnClickListener {
		
		@Override
		public void onClick(View v) {
			swipeRefresh();
		}
		
		protected void swipeRefresh() {
			recyclerAdapter.swipeRefresh();
		}
		
		protected abstract void onRefreshReady();
		
		protected abstract void onRefreshing();
		
		protected abstract void onRefreshComplete(StatusInfo statusInfo);
	}
	
	public static abstract class LoadHolder<StatusInfo> extends RecyclerHolder
			implements View.OnClickListener {
		private boolean reload;
		
		@Override
		public void onClick(View v) {
			swipeLoading();
		}
		
		protected void swipeLoading() {
			recyclerAdapter.swipeLoading(reload);
		}
		
		protected abstract void onLoadReady();
		
		protected abstract void onLoading();
		
		protected abstract boolean onLoadComplete(StatusInfo statusInfo);
	}
	
	private static abstract class RecyclerHolder {
		RecyclerAdapter recyclerAdapter;
		WrapHolder wrapHolder;
		
		protected abstract View onCreateView(ViewGroup parent);
		
		protected void onViewCreated(View view) {
		}
		
		public int getItemViewType() {
			return wrapHolder.getItemViewType();
		}
		
		public long getItemId() {
			return wrapHolder.getItemId();
		}
		
		public int getLayoutPosition() {
			return wrapHolder.getLayoutPosition();
		}
		
		public int getAdapterPosition() {
			return wrapHolder.getAdapterPosition();
		}
		
		public int getOldPosition() {
			return wrapHolder.getOldPosition();
		}
		
		public boolean isRecyclable() {
			return wrapHolder.isRecyclable();
		}
		
		public void setIsRecyclable(boolean recyclable) {
			wrapHolder.setIsRecyclable(recyclable);
		}
	}
	
	public static abstract class OnTaskListener<Task> {
		private Task mTask;
		
		private void cancel() {
			if (mTask != null) {
				taskCancel(mTask);
				mTask = null;
			}
		}
		
		private void notifyRefresh() {
			cancel();
			mTask = taskRefreshing();
		}
		
		private void notifyLoading() {
			cancel();
			mTask = taskLoading();
		}
		
		/**
		 * @deprecated Use {@link #onTask()}, delete at next version.
		 */
		@Deprecated
		public Task taskRefreshing() {
			return onTask();
		}
		
		/**
		 * @deprecated Use {@link #onTask()}, delete at next version.
		 */
		@Deprecated
		public Task taskLoading() {
			return onTask();
		}
		
		/**
		 * @deprecated Use {@link #onCancel(Task)}, delete at next version.
		 */
		@Deprecated
		public void taskCancel(Task task) {
			onCancel(task);
		}
		
		public Task onTask() {
			return null;
		}
		
		public void onCancel(Task task) {
		}
	}
	
	public interface OnItemClickListener {
		
		void onItemClick(ItemHolder itemHolder);
	}
	
	public interface OnItemLongClickListener {
		
		boolean onItemLongClick(ItemHolder itemHolder);
	}
	
	public static class OnRecyclerRefreshListener implements RefreshLayout.OnRefreshListener {
		private RecyclerAdapter recyclerAdapter;
		
		public OnRecyclerRefreshListener(RecyclerAdapter recyclerAdapter) {
			this.recyclerAdapter = recyclerAdapter;
		}
		
		@Override
		public void onRefresh() {
			this.recyclerAdapter.swipeRefresh();
		}
	}
	
	public static class OnRecyclerScrollListener extends RecyclerView.OnScrollListener {
		private RecyclerAdapter recyclerAdapter;
		
		private int[] scrollPositions;
		
		public OnRecyclerScrollListener(RecyclerAdapter recyclerAdapter) {
			this.recyclerAdapter = recyclerAdapter;
		}
		
		@Override
		public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
			checkFooter(recyclerView);
		}
		
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			checkFooter(recyclerView);
		}
		
		private boolean checkHeader(RecyclerView recyclerView) {
			if (!recyclerAdapter.autoLoad || recyclerAdapter.status != LOAD_READY) {
				return false;
			}
			
			int firstVisibleItemPosition;
			RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
			if (manager instanceof LinearLayoutManager) {
				firstVisibleItemPosition = ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
			} else if (manager instanceof StaggeredGridLayoutManager) {
				StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) manager;
				int spanCount = staggeredGridLayoutManager.getSpanCount();
				if (spanCount != scrollPositions.length) {
					scrollPositions = new int[spanCount];
				}
				staggeredGridLayoutManager.findFirstVisibleItemPositions(scrollPositions);
				firstVisibleItemPosition = findMinPosition(scrollPositions);
			} else {
				throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
			}
			
			if (firstVisibleItemPosition == 0) {
				recyclerView.removeCallbacks(mLoadingDelay);
				recyclerView.post(mLoadingDelay);
				return true;
			}
			
			return false;
		}
		
		private int findMinPosition(int[] scrollPositions) {
			int minPosition = scrollPositions[0];
			for (int valuePosition : scrollPositions) {
				if (valuePosition > minPosition) {
					minPosition = valuePosition;
				}
			}
			return minPosition;
		}
		
		private boolean checkFooter(RecyclerView recyclerView) {
			if (!recyclerAdapter.autoLoad || recyclerAdapter.status != LOAD_READY) {
				return false;
			}
			
			int lastVisibleItemPosition;
			RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
			if (manager instanceof LinearLayoutManager) {
				lastVisibleItemPosition = ((LinearLayoutManager) manager).findLastVisibleItemPosition();
			} else if (manager instanceof StaggeredGridLayoutManager) {
				StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) manager;
				int spanCount = staggeredGridLayoutManager.getSpanCount();
				if (spanCount != scrollPositions.length) {
					scrollPositions = new int[spanCount];
				}
				staggeredGridLayoutManager.findLastVisibleItemPositions(scrollPositions);
				lastVisibleItemPosition = findMaxPosition(scrollPositions);
			} else {
				throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
			}
			
			int visibleItemCount = manager.getChildCount();
			int totalItemCount = manager.getItemCount();
			if (visibleItemCount >= totalItemCount || lastVisibleItemPosition == totalItemCount - 1) {
				recyclerView.removeCallbacks(mLoadingDelay);
				recyclerView.post(mLoadingDelay);
				return true;
			}
			
			return false;
		}
		
		private int findMaxPosition(int[] scrollPositions) {
			int maxPosition = scrollPositions[0];
			for (int valuePosition : scrollPositions) {
				if (valuePosition > maxPosition) {
					maxPosition = valuePosition;
				}
			}
			return maxPosition;
		}
		
		private Runnable mLoadingDelay = new Runnable() {
			@Override
			public void run() {
				recyclerAdapter.swipeLoading(false);
			}
		};
	}
	
	public static class WrapRecyclerAdapter extends RecyclerView.Adapter<WrapHolder> {
		/** 刷新类别 */
		static final int TYPE_REFRESH = Integer.MAX_VALUE;
		/** 加载类别 */
		static final int TYPE_LOAD = Integer.MIN_VALUE;
		
		private GridLayoutManager.SpanSizeLookup spanSizeLookup;
		
		private RecyclerAdapter recyclerAdapter;
		
		public WrapRecyclerAdapter(RecyclerAdapter recyclerAdapter) {
			this.recyclerAdapter = recyclerAdapter;
		}
		
		public final <T> T getRecyclerAdapter() {
			// noinspection unchecked
			return (T) recyclerAdapter;
		}
		
		@Override
		public int getItemCount() {
			if (recyclerAdapter == null) {
				return 0;
			}
			int itemCount = recyclerAdapter.getItemCount();
			if (recyclerAdapter.status == REFRESH_READY
					|| (recyclerAdapter.status == REFRESHING && itemCount == 0)
					|| (recyclerAdapter.status == REFRESH_COMPLETE && itemCount == 0)) {
				return 1;
			}
			if (recyclerAdapter.status == LOAD_READY
					|| recyclerAdapter.status == LOADING
					|| recyclerAdapter.status == LOAD_COMPLETE) {
				itemCount++;
			}
			return itemCount;
		}
		
		@Override
		public int getItemViewType(int position) {
			int itemCount = recyclerAdapter.getItemCount();
			if (recyclerAdapter.status == REFRESH_READY
					|| (recyclerAdapter.status == REFRESHING && itemCount == 0)
					|| (recyclerAdapter.status == REFRESH_COMPLETE && itemCount == 0)) {
				return TYPE_REFRESH;
			} else if ((recyclerAdapter.status == LOAD_READY
					|| recyclerAdapter.status == LOADING
					|| recyclerAdapter.status == LOAD_COMPLETE)
					&& position == itemCount) {
				return TYPE_LOAD;
			}
			return recyclerAdapter.getItemType(position);
		}
		
		@Override
		public long getItemId(int position) {
			int viewType = getItemViewType(position);
			if (viewType == TYPE_REFRESH || viewType == TYPE_LOAD) {
				return View.NO_ID;
			}
			return recyclerAdapter.getItemId(position);
		}
		
		@Override
		public WrapHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == TYPE_REFRESH) {
				RefreshHolder refreshHolder = recyclerAdapter.onCreateRefreshHolder();
				refreshHolder.wrapHolder = new WrapHolder(refreshHolder.onCreateView(parent));
				refreshHolder.wrapHolder.itemHolder = refreshHolder;
				refreshHolder.wrapHolder.itemView.setOnClickListener(refreshHolder);
				refreshHolder.onViewCreated(refreshHolder.wrapHolder.itemView);
				return refreshHolder.wrapHolder;
			} else if (viewType == TYPE_LOAD) {
				LoadHolder loadHolder = recyclerAdapter.onCreateLoadHolder();
				loadHolder.wrapHolder = new WrapHolder(loadHolder.onCreateView(parent));
				loadHolder.wrapHolder.itemHolder = loadHolder;
				loadHolder.wrapHolder.itemView.setOnClickListener(loadHolder);
				loadHolder.onViewCreated(loadHolder.wrapHolder.itemView);
				return loadHolder.wrapHolder;
			} else {
				ItemHolder itemHolder = recyclerAdapter.onCreateItemHolder(viewType);
				itemHolder.wrapHolder = new WrapHolder(itemHolder.onCreateView(parent));
				itemHolder.wrapHolder.itemHolder = itemHolder;
				itemHolder.onViewCreated(itemHolder.wrapHolder.itemView);
				return itemHolder.wrapHolder;
			}
		}
		
		@Override
		public void onBindViewHolder(WrapHolder holder, int position) {
			if (holder.itemHolder instanceof ItemHolder) {
				ItemHolder itemHolder = (ItemHolder) holder.itemHolder;
				itemHolder.recyclerAdapter = this.recyclerAdapter;
				itemHolder.onBindView(position);
				holder.itemView.setOnClickListener(itemHolder);
				holder.itemView.setOnLongClickListener(itemHolder);
			} else if (holder.itemHolder instanceof RefreshHolder) {
				RefreshHolder refreshHolder = (RefreshHolder) holder.itemHolder;
				refreshHolder.recyclerAdapter = this.recyclerAdapter;
				if (recyclerAdapter.status == REFRESH_READY) {
					// noinspection unchecked
					refreshHolder.onRefreshReady();
				} else if (recyclerAdapter.status == REFRESHING) {
					refreshHolder.onRefreshing();
				} else if (recyclerAdapter.status == REFRESH_COMPLETE) {
					// noinspection unchecked
					refreshHolder.onRefreshComplete(recyclerAdapter.statusInfo);
				}
			} else if (holder.itemHolder instanceof LoadHolder) {
				LoadHolder loadHolder = (LoadHolder) holder.itemHolder;
				loadHolder.recyclerAdapter = this.recyclerAdapter;
				if (recyclerAdapter.status == LOAD_READY) {
					loadHolder.onLoadReady();
				} else if (recyclerAdapter.status == LOADING) {
					loadHolder.onLoading();
				} else if (recyclerAdapter.status == LOAD_COMPLETE) {
					// noinspection unchecked
					loadHolder.reload = loadHolder.onLoadComplete(recyclerAdapter.statusInfo);
				}
			} else {
				throw new RuntimeException("holder must extends ItemHolder");
			}
		}
		
		@Override
		public void onViewRecycled(WrapHolder holder) {
			if (holder.itemHolder instanceof ItemHolder) {
				recyclerAdapter.onViewRecycled((ItemHolder) holder.itemHolder);
				holder.itemHolder.recyclerAdapter = null;
			}
		}
		
		@Override
		public boolean onFailedToRecycleView(WrapHolder holder) {
			if (holder.itemHolder instanceof ItemHolder) {
				return recyclerAdapter.onFailedToRecycleView((ItemHolder) holder.itemHolder);
			}
			return super.onFailedToRecycleView(holder);
		}
		
		@Override
		public void onViewAttachedToWindow(WrapHolder holder) {
			if (holder.itemHolder instanceof ItemHolder) {
				recyclerAdapter.onViewAttachedToWindow((ItemHolder) holder.itemHolder);
			}
			if (holder.itemHolder instanceof RefreshHolder
					|| holder.itemHolder instanceof LoadHolder) {
				ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
				if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
					((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(true);
				}
			}
		}
		
		@Override
		public void onViewDetachedFromWindow(WrapHolder holder) {
			if (holder.itemHolder instanceof ItemHolder) {
				recyclerAdapter.onViewDetachedFromWindow((ItemHolder) holder.itemHolder);
			}
		}
		
		@Override
		public void onAttachedToRecyclerView(RecyclerView recyclerView) {
			recyclerAdapter.onAttachedToRecyclerView(recyclerView);
			
			if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
				final GridLayoutManager manager = ((GridLayoutManager) recyclerView.getLayoutManager());
				final GridLayoutManager.SpanSizeLookup spanSizeLookup = manager.getSpanSizeLookup();
				if (this.spanSizeLookup == null || spanSizeLookup != this.spanSizeLookup) {
					this.spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
						@Override
						public int getSpanSize(int position) {
							int viewType = getItemViewType(position);
							return viewType == TYPE_REFRESH || viewType == TYPE_LOAD ?
									manager.getSpanCount() : (spanSizeLookup != null ? spanSizeLookup.getSpanSize(position) : 1);
						}
					};
					manager.setSpanSizeLookup(this.spanSizeLookup);
				}
			}
		}
		
		@Override
		public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
			recyclerAdapter.onDetachedFromRecyclerView(recyclerView);
			if (recyclerAdapter.onTaskListener != null) {
				recyclerAdapter.onTaskListener.cancel();
			}
		}
	}
	
	static class WrapHolder extends RecyclerView.ViewHolder {
		private RecyclerHolder itemHolder;
		
		WrapHolder(View itemView) {
			super(itemView);
		}
	}
	
	public static abstract class ItemDecoration extends RecyclerView.ItemDecoration {
		
		/**
		 * 判断是否忽略绘制
		 *
		 * @param view   子控件
		 * @param parent 父控件
		 * @param state  列表状态
		 * @return true忽略
		 */
		public boolean isSkipDraw(View view, RecyclerView parent, RecyclerView.State state) {
			if (view.getVisibility() == View.GONE) {
				return true;
			}
			RecyclerView.ViewHolder viewHolder = parent.getChildViewHolder(view);
			if (viewHolder instanceof WrapHolder) {
				RecyclerHolder recyclerHolder = ((WrapHolder) viewHolder).itemHolder;
				return recyclerHolder instanceof RefreshHolder || recyclerHolder instanceof LoadHolder;
			}
			return false;
		}
	}
}
