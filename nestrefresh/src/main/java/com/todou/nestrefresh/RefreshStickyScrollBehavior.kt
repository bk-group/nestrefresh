package com.todou.nestrefresh

import android.content.Context
import android.graphics.Rect
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.todou.nestrefresh.base.BaseBehavior

class RefreshStickyScrollBehavior @JvmOverloads constructor(context: Context? = null, attrs: AttributeSet? = null) :
    BaseBehavior<View>(context, attrs) {

    private val rectOut = Rect()
    private var refreshStickyBehavior: RefreshStickyBehavior? = null
    private var loadMoreBehavior: LoadMoreBehavior? = null

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        val childLpHeight = child.layoutParams.height
        if (childLpHeight == ViewGroup.LayoutParams.WRAP_CONTENT || childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            val dependencies = parent.getDependencies(child)
            val header = findFirstDependency(dependencies)
            if (header != null) {
                var availableHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec)
                if (availableHeight == 0) {
                    availableHeight = parent.height
                }

                val height = availableHeight - getPinHeightWithoutInset(header)
                val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    height, if (childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT)
                        View.MeasureSpec.EXACTLY
                    else
                        View.MeasureSpec.AT_MOST
                )
                parent.onMeasureChild(child, parentWidthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed)
                return true
            }
        }
        return false
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val behavior = getBehavior(dependency) ?: return false
        return behavior is RefreshStickyBehavior
                || behavior is LoadMoreBehavior
                || behavior is RefreshBehavior
    }

    override fun layoutChild(parent: CoordinatorLayout, child: View, layoutDirection: Int) {
        val dependencies = parent.getDependencies(child)
        val header = this.findFirstDependency(dependencies)
        if (header != null) {
            val lp = child.layoutParams as CoordinatorLayout.LayoutParams
            rectOut.left = lp.leftMargin + parent.paddingLeft
            rectOut.right = rectOut.left + child.measuredWidth
            rectOut.top = lp.topMargin + header.bottom - getHeaderOffset(header)
            rectOut.bottom = rectOut.top + child.measuredHeight
            child.layout(rectOut.left, rectOut.top, rectOut.right, rectOut.bottom)
        } else {
            super.layoutChild(parent, child, layoutDirection)
        }
    }

    private fun getHeaderOffset(header: View): Int {
        if (header.layoutParams is CoordinatorLayout.LayoutParams) {
            val params = header.layoutParams as CoordinatorLayout.LayoutParams
            if (params.behavior is RefreshStickyBehavior) {
                return (params.behavior as RefreshStickyBehavior).getTopAndBottomOffset()
            }
        }
        return 0
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val behavior = getBehavior(dependency)
        var offset = 0
        if (behavior is RefreshStickyBehavior) {
            refreshStickyBehavior = behavior
            offset = behavior.getTopAndBottomOffset()
        } else if (behavior is LoadMoreBehavior) {
            loadMoreBehavior = behavior
            offset = behavior.currentRange + getHeaderOffsetByBehavior()
        }
        return setTopAndBottomOffset(offset)
    }

    private fun getHeaderOffsetByBehavior(): Int {
        return refreshStickyBehavior?.getTopAndBottomOffset() ?: 0
    }

    fun getFooterTotalUnconsumed(): Float {
        return loadMoreBehavior?.totalUnconsumed ?: 0f
    }

    private fun findFirstDependency(dependencies: List<View>): View? {
        for (view in dependencies) {
            val layoutParams = view.layoutParams
            if (layoutParams is CoordinatorLayout.LayoutParams) {
                if (layoutParams.behavior is RefreshStickyBehavior) {
                    return view
                }
            }
        }
        return null
    }

    private fun getPinHeightWithoutInset(view: View): Int {
        return if (view is RefreshStickyLayout) {
            view.getPinHeightWithoutInsetTop()
        } else {
            0
        }
    }
}
