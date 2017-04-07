package com.kycq.refresh.basic;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kycq.library.refresh.RecyclerAdapter;

public class DividerItemDecoration extends RecyclerAdapter.ItemDecoration {
	private Paint paint = new Paint();
	
	public DividerItemDecoration() {
		paint.setColor(Color.BLACK);
	}
	
	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
		if (isSkipDraw(view, parent, state)) {
			return;
		}
		outRect.bottom = 1;
	}
	
	@Override
	public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
		int childCount = parent.getChildCount();
		for (int index = 0; index < childCount; index++) {
			View child = parent.getChildAt(index);
			if (isSkipDraw(child, parent, state)) {
				continue;
			}
			
			int left = child.getLeft();
			int top = child.getBottom();
			int right = child.getRight();
			int bottom = child.getBottom() + 1;
			canvas.drawRect(left, top, right, bottom, paint);
		}
	}
}
