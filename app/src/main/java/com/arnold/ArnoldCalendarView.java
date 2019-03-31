package com.arnold;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


/**
 * @author Arno
 * @date 2018/11/29 0029
 * @des
 */
public class ArnoldCalendarView extends LinearLayout {

    /**
     * 添加日期点击事件
     *
     * @param onCalendarClickListener
     * @return
     */
    public ArnoldCalendarView setOnCalendarClickListener(OnCalendarClickListener onCalendarClickListener) {
        this.onCalendarClickListener = onCalendarClickListener;
        return this;
    }

    /**
     * 添加当前日期改变事件
     *
     * @param onCalendarCurrentDateChangeListener
     * @return
     */
    public ArnoldCalendarView setOnCalendarCurrentDateChangeListener(OnCalendarCurrentDateChangeListener onCalendarCurrentDateChangeListener) {
        this.onCalendarCurrentDateChangeListener = onCalendarCurrentDateChangeListener;
        return this;
    }

    /**
     * 添加today绘制
     *
     * @param todayStrategy
     * @return
     */
    public ArnoldCalendarView setTodayDrawStrategy(Strategy todayStrategy) {
        this.todayDrawStrategy = todayStrategy;
        return this;
    }

    /**
     * 添加选中日期绘制
     *
     * @param selectStrategy
     * @return
     */
    public ArnoldCalendarView setSelectDrawStrategy(Strategy selectStrategy) {
        this.selectDrawStrategy = selectStrategy;
        return this;
    }

    /**
     * 日期更改
     *
     * @param onCalendarTransformation
     */
    public ArnoldCalendarView onCalendarTransformation(int onCalendarTransformation) {
        this.onCalendarTransformationListener.onCalendarTransformation(onCalendarTransformation);
        return this;
    }


    public ArnoldCalendarView(Context context) {
        this(context, null);
    }

    public ArnoldCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(VERTICAL);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.addView(new WeekView(context));
        this.addView(new Adapter(context).getTargetView());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }


    private static final String[] SUNDAY_COLUMN_FONT = new String[]{"日", "一", "二", "三", "四", "五", "六"};

    /**
     * 日期选中绘制策略
     */
    private Strategy todayDrawStrategy = new SolidCircleStrategy();
    /**
     * 日期选中绘制策略
     */
    private Strategy selectDrawStrategy = new SolidRectStrategy();

    /**
     * 点击日期事件
     */
    private OnCalendarClickListener onCalendarClickListener;

    /**
     * 月份改变事件
     */
    private OnCalendarCurrentDateChangeListener onCalendarCurrentDateChangeListener;
    /**
     * 点击上个月或者下个月日期事件
     */
    private OnCalendarTransformationListener onCalendarTransformationListener;


    private int spacing = dp2px(2);
    private int sundayColumnFontColor = Color.parseColor("#3785ea");
    private int sundayColumnBackgroundColor = Color.WHITE;

    private boolean isDisplayBorder = true;
    //月历背景颜色
    private int calendarMonthBackgroundColor = Color.WHITE;
    //月历正常日期字体颜色
    private int calendarFontColor = Color.BLACK;
    //月历背景边框颜色
    private int calendarBackgroundBorderColor = Color.parseColor("#C8C8C8");

    //月历today
    private int todayBackgroundColor = Color.parseColor("#3785ea");
    //选中
    private int selectDayBackgroundColor = Color.parseColor("#3785ea");
    //月历上个月，下个月的 灰色 日期字体颜色
    private int lastOrNextMonthFontColor = Color.parseColor("#C8C8C8");


    /**
     * 星期
     */
    private class WeekView extends View {

        private final Paint paint;

        public WeekView(Context context) {
            super(context);
            this.setBackgroundColor(sundayColumnBackgroundColor);
            paint = new Paint();
            paint.setColor(sundayColumnFontColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(20f);
            paint.setTextSize(sp2px(12));
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec) / 7, MeasureSpec.EXACTLY));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int height = getHeight();
            int columnWidth = getWidth() / 7;
            for (int i = 0, len = SUNDAY_COLUMN_FONT.length; i < len; i++) {
                String text = SUNDAY_COLUMN_FONT[i];
                int fontWidth = (int) paint.measureText(text);
                int startX = columnWidth * i + (columnWidth - fontWidth) / 2;
                int startY = (int) (height / 2 - (paint.ascent() + paint.descent()) / 2);
                canvas.drawText(text, startX, startY, paint);
            }
        }
    }

    /**
     * 月历
     */
    private class MonthDateView extends View {

        private int mSelYear;
        private int mSelMonth;
        private int mSelDay;

        public int getSelYear() {
            return mSelYear;
        }

        public int getSelMonth() {
            return mSelMonth;
        }

        public int getSelDay() {
            return mSelDay;
        }


        private int mColumnSize;
        private int[][] drawTextSet;
        private int selectData = -1;
        Paint fillPaint;
        Paint solarPaint;

        public MonthDateView(Context context) {
            super(context);
            this.fillPaint = new Paint();
            this.solarPaint = new Paint();
            Calendar calendar = Calendar.getInstance();
            setSelectYearMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
            this.setBackgroundColor(calendarMonthBackgroundColor);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            mColumnSize = getWidth() / 7;
            Calendar instance = Calendar.getInstance();
            boolean currentMonth = instance.get(Calendar.YEAR) == mSelYear && instance.get(Calendar.MONTH) == mSelMonth;
            instance.set(mSelYear, mSelMonth, 1);
            drawTextSet = new int[6][7];
            for (int day = 0,
                 /*当前月份的最大天数*/totalDaysOfMonth = instance.getActualMaximum(Calendar.DATE),
                 /*当前月份的第一天位于周几  日：1一：2二：3三：4四：5五：6六*/weekNumber = instance.get(Calendar.DAY_OF_WEEK) - 1,
                 /*当前月份的第一天在月历的位置*/firstDayIndexOfMonth = weekNumber == 0 ? 7 : weekNumber; day < 42; day++) {

                int column = day % 7, row = day / 7;

                if (day < firstDayIndexOfMonth) {
                    drawTextSet[row][column] = -1;
                    onDrawText(canvas, getIsCurrentCalendar(mSelYear, mSelMonth, 1, -1).getActualMaximum(Calendar.DATE) - firstDayIndexOfMonth + day + 1, column, row, -1, currentMonth);
                } else if (day >= firstDayIndexOfMonth && day < totalDaysOfMonth + firstDayIndexOfMonth) {
                    drawTextSet[row][column] = day + 1 - firstDayIndexOfMonth;
                    onDrawText(canvas, day - firstDayIndexOfMonth + 1, column, row, 0, currentMonth);
                } else {
                    drawTextSet[row][column] = -2;
                    onDrawText(canvas, day - firstDayIndexOfMonth - totalDaysOfMonth + 1, column, row, 1, currentMonth);
                }
            }
        }

        /**
         * @param canvas
         * @param day
         * @param column         The first few columns
         * @param row            The first few lines
         * @param isOverflowGray {@code -1} last month day {@code 0} current month day {@code 1} next month day
         * @param isCurrentMonth {@code true} it is the current month  {@code false} not the current month.
         */
        private void onDrawText(Canvas canvas, int day, int column, int row, int isOverflowGray, boolean isCurrentMonth) {
            solarPaint.setColor(calendarFontColor);
            solarPaint.setStrokeWidth(10f);
            solarPaint.setTextSize(mColumnSize * 0.3f);
            if (isDisplayBorder) {
                onDrawDives(canvas, column, row);
            }
            //is last month or next month
            if (isOverflowGray != 0) {
                solarPaint.setColor(lastOrNextMonthFontColor);
            }
            //is current month
            if (isOverflowGray == 0) {

                //is today and select
                if (mSelDay == day && (selectData == day || selectData == -1)) {
                    fillPaint.setColor(isCurrentMonth ? todayBackgroundColor : selectDayBackgroundColor);
                    todayDrawStrategy.onDraw(canvas, mColumnSize, column, row, fillPaint, solarPaint);
                }
                //is today and unSelect
                else if (isCurrentMonth && mSelDay == day) {
                    new HollowCircleStrategy().onDraw(canvas, mColumnSize, column, row, fillPaint, solarPaint);
                }
                //is select day
                else if (selectData == day) {
                    fillPaint.setColor(selectDayBackgroundColor);
                    selectDrawStrategy.onDraw(canvas, mColumnSize, column, row, fillPaint, solarPaint);
                }
            }

            int startY1 = (int) (mColumnSize * row + (mColumnSize - solarPaint.ascent() - solarPaint.descent()) * 0.5f);
            int startX1 = (int) (mColumnSize * column + (mColumnSize - solarPaint.measureText(day + "")) * 0.5f);
            canvas.drawText(day + "", startX1, startY1, solarPaint);
        }


        /**
         * 绘制 格子 边框
         *
         * @param canvas
         * @param column
         * @param row
         */
        private void onDrawDives(Canvas canvas, int column, int row) {
            Path boxPath = new Path();
            boxPath.moveTo(mColumnSize * column, mColumnSize * row);
            boxPath.lineTo(mColumnSize * (column + 1), mColumnSize * row);
            boxPath.lineTo(mColumnSize * (column + 1), mColumnSize * (row + 1));
            boxPath.lineTo(mColumnSize * column, mColumnSize * (row + 1));
            boxPath.close();
            //边框
            Paint borderPaint = new Paint();
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2f);
            borderPaint.setColor(calendarBackgroundBorderColor);
            canvas.drawPath(boxPath, borderPaint);
        }


        private int downX = 0, downY = 0;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int eventCode = event.getAction();
            switch (eventCode) {
                case MotionEvent.ACTION_DOWN:
                    downX = (int) event.getX();
                    downY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    int upX = (int) event.getX();
                    int upY = (int) event.getY();
                    //点击事件
                    if (Math.abs(upX - downX) < 10 && Math.abs(upY - downY) < 10) {
                        performClick();
                        doClickAction((upX + downX) / 2, (upY + downY) / 2);
                    }
                    break;
                default:
                    break;
            }
            return true;
        }

        /**
         * 设置年月
         *
         * @param year
         * @param month
         */
        public MonthDateView setSelectYearMonth(int year, int month, int day) {
            mSelYear = year;
            mSelMonth = month;
            mSelDay = day;
            selectData = -1;
            return this;
        }

        /**
         * 设置年月
         *
         * @param calendar
         */
        public MonthDateView setSelectYearMonth(Calendar calendar) {
            return setSelectYearMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }

        /**
         * 执行点击事件
         *
         * @param x
         * @param y
         */
        private void doClickAction(int x, int y) {
            int row = y / mColumnSize;
            int column = x / mColumnSize;

            if (drawTextSet[row][column] > 0) {
                selectData = drawTextSet[row][column];
                invalidate();
                //执行activity发送过来的点击处理事件
                if (onCalendarClickListener != null) {
                    onCalendarClickListener.onCalendarClick(mSelYear, mSelMonth, drawTextSet[row][column]);
                }
            } else if (null != onCalendarTransformationListener) {
                if (drawTextSet[row][column] == -1) {
                    onCalendarTransformationListener.onCalendarTransformation(-1);
                } else if (drawTextSet[row][column] == -2) {
                    onCalendarTransformationListener.onCalendarTransformation(1);
                }
            }
        }
    }

    /**
     * @param year
     * @param month
     * @param day
     * @param isCurrent 是否是上个月，当前月，下个月
     * @return
     */
    private Calendar getIsCurrentCalendar(int year, int month, int day, int isCurrent) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);
        if (isCurrent != 0) {
            calendar.add(Calendar.MONTH, +isCurrent);
        }
        if (day == 1) {
            return calendar;
        }
        if (calendar.getActualMaximum(Calendar.DATE) > day) {
            calendar.set(Calendar.DATE, day);
        } else {
            calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
        }
        return calendar;
    }

    /**
     * dpתpx
     *
     * @return
     */
    private int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, this.getContext().getResources().getDisplayMetrics());
    }

    /**
     * @param spVal
     * @return
     */
    private int sp2px(float spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spVal, this.getContext().getResources().getDisplayMetrics());
    }


    /***************************************************************************************************************************/


    private class Adapter extends PagerAdapter implements ViewPager.OnPageChangeListener, OnCalendarTransformationListener {

        @Override
        public void onPageSelected(int position) {
            MonthDateView positionItem = this.getPositionItem(position);

            int years = positionItem.getSelYear();
            int month = positionItem.getSelMonth();
            int day = positionItem.getSelDay();

            if (null != onCalendarCurrentDateChangeListener) {
                onCalendarCurrentDateChangeListener.onCalendarCurrentDateChange(years, month, day);
            }
            day = Calendar.getInstance().get(Calendar.DATE);
            getPositionItem(position + 1).setSelectYearMonth(getIsCurrentCalendar(years, month, day, +1)).invalidate();
            getPositionItem(position - 1).setSelectYearMonth(getIsCurrentCalendar(years, month, day, -1)).invalidate();
        }

        @Override
        public void onCalendarTransformation(int calendarTransformation) {
            if (calendarTransformation == 0) {
                return;
            }
            if (calendarTransformation == 1 || calendarTransformation == -1) {
                this.viewPager.setCurrentItem(this.viewPager.getCurrentItem() + calendarTransformation);
                return;
            }
            int currentItem = getTargetView().getCurrentItem();
            MonthDateView positionItem = this.getPositionItem(currentItem);
            int years = positionItem.getSelYear();
            int month = positionItem.getSelMonth();
            int day = positionItem.getSelDay();

            if (calendarTransformation > 0) {
                this.viewPager.setCurrentItem(this.viewPager.getCurrentItem() + 1);
            } else if (calendarTransformation < 0) {
                this.viewPager.setCurrentItem(this.viewPager.getCurrentItem() - 1);
            }

            currentItem = getTargetView().getCurrentItem();
            positionItem = this.getPositionItem(currentItem);

            positionItem.setSelectYearMonth(getIsCurrentCalendar(years, month, day, calendarTransformation)).invalidate();

            years = positionItem.getSelYear();
            month = positionItem.getSelMonth();
            day = positionItem.getSelDay();

            if (null != onCalendarCurrentDateChangeListener) {
                onCalendarCurrentDateChangeListener.onCalendarCurrentDateChange(years, month, day);
            }

            day = Calendar.getInstance().get(Calendar.DATE);
            getPositionItem(currentItem + 1).setSelectYearMonth(getIsCurrentCalendar(years, month, day, +1)).invalidate();
            getPositionItem(currentItem - 1).setSelectYearMonth(getIsCurrentCalendar(years, month, day, -1)).invalidate();
        }

        private int currentItem = 1000;
        private List<MonthDateView> viewList;
        private ViewPager viewPager;

        Adapter(Context context) {
            onCalendarTransformationListener = this;
            this.viewList = Arrays.asList(new MonthDateView(context), new MonthDateView(context), new MonthDateView(context), new MonthDateView(context));
            this.viewPager = new ViewPager(context);
            this.viewPager.addOnPageChangeListener(this);
            this.viewPager.setAdapter(this);
            this.viewPager.setCurrentItem(currentItem);

        }

        ViewPager getTargetView() {
            return viewPager;
        }

        @Override
        public int getCount() {
            return 2000;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            // 最简单解决 notifyDataSetChanged() 页面不刷新问题的方法
            return POSITION_NONE;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            MonthDateView monthDateView = getPositionItem(position);
            //如果View已经在之前添加到了一个父组件，则必须先remove，否则会抛出IllegalStateException。
            if (container != null) {
                container.removeView(monthDateView);
            }
            container.addView(monthDateView);
            return monthDateView;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        private MonthDateView getPositionItem(int position) {
            return viewList.get((position = (position - currentItem) % 4) < 0 ? 4 + position : position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }


        @Override
        public void onPageScrollStateChanged(int state) {

        }


    }


    /***************************************************************************************************************************/
    private interface OnCalendarTransformationListener {
        /**
         * 日期改变事件
         *
         * @param onCalendarTransformation
         */
        void onCalendarTransformation(int onCalendarTransformation);
    }

    public interface OnCalendarClickListener {
        /**
         * 日期点击事件
         *
         * @param year
         * @param month
         * @param day
         */
        void onCalendarClick(int year, int month, int day);
    }

    public interface OnCalendarCurrentDateChangeListener {
        /**
         * 当前日期改变
         *
         * @param year
         * @param month
         * @param day
         */
        void onCalendarCurrentDateChange(int year, int month, int day);
    }

    /*-------------添加策略模式添加日期选中绘制--------------------*/

    public interface Strategy {
        /**
         * 更新策略
         *
         * @param canvas               画布
         * @param latticeModel         格子模型
         * @param columnIndex          列位置
         * @param rowIndex             横位置
         * @param calendarFillingPaint 填充颜色
         * @param calendarColorPaint   画笔颜色
         */
        void onDraw(Canvas canvas, int latticeModel, int columnIndex, int rowIndex, Paint calendarFillingPaint, Paint calendarColorPaint);

    }

    public class HollowCircleStrategy implements Strategy {

        @Override
        public void onDraw(Canvas canvas, int latticeModel, int columnIndex, int rowIndex, Paint calendarFillingPaint, Paint calendarColorPaint) {

            canvas.drawCircle(latticeModel * columnIndex + latticeModel * 0.5f, latticeModel * rowIndex + latticeModel * 0.5f, latticeModel * 0.5f, calendarFillingPaint);
            calendarFillingPaint.setColor(calendarMonthBackgroundColor);
            canvas.drawCircle(latticeModel * columnIndex + latticeModel * 0.5f, latticeModel * rowIndex + latticeModel * 0.5f, latticeModel * 0.5f - dp2px(2), calendarFillingPaint);
        }
    }

    public class SolidCircleStrategy implements Strategy {

        @Override
        public void onDraw(Canvas canvas, int latticeModel, int columnIndex, int rowIndex, Paint calendarFillingPaint, Paint calendarColorPaint) {
            calendarColorPaint.setColor(Color.WHITE);
            canvas.drawCircle(latticeModel * columnIndex + latticeModel * 0.5f, latticeModel * rowIndex + latticeModel * 0.5f, latticeModel * 0.5f, calendarFillingPaint);
        }
    }

    public class SolidRectStrategy implements Strategy {

        @Override
        public void onDraw(Canvas canvas, int latticeModel, int columnIndex, int rowIndex, Paint calendarFillingPaint, Paint calendarColorPaint) {
            calendarColorPaint.setColor(Color.WHITE);
            canvas.drawRect(new RectF(latticeModel * columnIndex, latticeModel * rowIndex, latticeModel * columnIndex + latticeModel, latticeModel * rowIndex + latticeModel), calendarFillingPaint);
        }
    }

    public class SolidRoundRectStrategy implements Strategy {

        @Override
        public void onDraw(Canvas canvas, int latticeModel, int columnIndex, int rowIndex, Paint calendarFillingPaint, Paint calendarColorPaint) {
            RectF rectF = new RectF(latticeModel * columnIndex, latticeModel * rowIndex, latticeModel * columnIndex + latticeModel, latticeModel * rowIndex + latticeModel);
            calendarColorPaint.setColor(Color.WHITE);
            canvas.drawRoundRect(rectF, dp2px(5), dp2px(5), calendarFillingPaint);
        }
    }

    public class HollowRectStrategy implements Strategy {

        @Override
        public void onDraw(Canvas canvas, int latticeModel, int columnIndex, int rowIndex, Paint calendarFillingPaint, Paint calendarColorPaint) {
            canvas.drawRect(latticeModel * columnIndex, latticeModel * rowIndex, latticeModel * columnIndex + latticeModel, latticeModel * rowIndex + latticeModel, calendarFillingPaint);
            calendarFillingPaint.setColor(calendarMonthBackgroundColor);
            canvas.drawRect(latticeModel * columnIndex + spacing, latticeModel * rowIndex + spacing, latticeModel * columnIndex + latticeModel - spacing, latticeModel * rowIndex + latticeModel - spacing, calendarFillingPaint);
        }
    }

}
