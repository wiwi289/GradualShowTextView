package swu.cx.gradualshowtextview

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import kotlin.properties.Delegates

/**
 * Created by chenxiong
 * date 12/30/21
 */
class GradualShowTextView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    style: Int = 0
) : AppCompatTextView(context, attr, style) {

    //存储每一行的内容信息
    private val cellRows: MutableList<CellRowInfo> = mutableListOf()
    //行信息的处理的开关，只能一次
    private var lineInfoInited = false

    //记录外设属性
    private var textBgColor: Int = 0
    private var textForeColor: Int = 0
    //行间距
    private var lineSpace by Delegates.notNull<Float>()
    //每行动画执行时间
    private var lineAnimTime by Delegates.notNull<Long>()
    //背景起始画笔
    private val textBgPaint: Paint by lazy {
        Paint().apply {
            color = textBgColor
            textSize = this@GradualShowTextView.textSize
            setStyle(Paint.Style.FILL)
        }
    }
    //渐变前景画笔
    private val textForePaint: Paint by lazy {
        Paint().apply {
            color = textForeColor
            textSize = this@GradualShowTextView.textSize
            setStyle(Paint.Style.FILL)
        }
    }
    //记录行高
    private val mLineHeight: Float by lazy {
        (layout.getLineDescent(0) - layout.getLineAscent(0)).toFloat()
    }
    //绘制文本需要偏移的base距离
    private val mBaseLineInCreSize:Float by lazy {
        val fontMetrics = textBgPaint.fontMetrics
        val space = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
        mLineHeight / 2 + space
    }

    private var lineTop: Float

    private val bgRect = Rect()

    private val foreRect = Rect()

    //记录当前的动画
    private var nowTextAnim: ValueAnimator? = null
    //记录当前行
    private var index = 0
    //记录是否被停止当前行的动画
    private var canceled = false
    //记录当前行动画的进度
    private var nowLineProgress = 0f
    //记录暂停播放比用于确定时间比例
    private var durationRatio = 1f
    //防止重复开始
    private var launched = false

    init {
        parseAttr(attr)
        lineTop = compoundPaddingTop.toFloat()
    }

    private fun parseAttr(attr: AttributeSet?) {
        if (attr == null) {
            Log.e(TAG,"attr is null!")
        }
        val typedArray: TypedArray = context.obtainStyledAttributes(attr, R.styleable.GradualShowTextView)
        textBgColor = typedArray.getColor(R.styleable.GradualShowTextView_background_text_color, Color.BLACK)
        textForeColor = typedArray.getColor(R.styleable.GradualShowTextView_foreground_text_color, Color.WHITE)
        lineSpace = typedArray.getDimension(R.styleable.GradualShowTextView_line_space,0f)
        lineAnimTime = typedArray.getInt(R.styleable.GradualShowTextView_line_animation_time, 3000).toLong()
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!lineInfoInited) {
            initAllLineData()
            lineInfoInited = true
        }
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val realHeight = if (heightMode == MeasureSpec.EXACTLY) heightSize else measuredHeight + (layout.lineCount - 1) * lineSpace
        setMeasuredDimension(measuredWidth, realHeight.toInt())
    }

    override fun onDraw(canvas: Canvas?) {
        lineTop = compoundPaddingTop.toFloat()
        for (i in cellRows.indices) {
            val cellRowInfo = cellRows[i]
            bgRect.apply {
                left = (cellRowInfo.factor * measuredWidth).toInt() + compoundPaddingLeft
                top = lineTop.toInt()
                right = measuredWidth - compoundPaddingRight
                bottom = (top + mLineHeight).toInt()
            }
            drawBgText(canvas, bgRect, cellRowInfo.text)

            foreRect.apply {
                left = compoundPaddingLeft
                top = lineTop.toInt()
                right = ((measuredWidth - compoundPaddingRight) * cellRowInfo.factor).toInt()
                bottom = (top + mLineHeight).toInt()
            }
            drawForeText(canvas, foreRect, cellRowInfo.text)
            lineTop += (mLineHeight + lineSpace)
        }
    }

    //绘制背景文字
    @SuppressLint("NewApi")
    private fun drawBgText(canvas: Canvas?, rect: Rect, content: String) {
        canvas?.save()
        canvas?.clipRect(rect)
        canvas?.drawText(content, compoundPaddingStart.toFloat(), rect.top + mBaseLineInCreSize, textBgPaint)
        canvas?.restore()
    }

    //绘制渐变前景文字
    @SuppressLint("NewApi")
    private fun drawForeText(canvas: Canvas?, rect: Rect, content: String) {
        canvas?.save()
        canvas?.clipRect(rect)
        canvas?.drawText(content, compoundPaddingStart.toFloat(), rect.top + mBaseLineInCreSize, textForePaint)
        canvas?.restore()
    }

    private fun playText() {
        if (index !in cellRows.indices) return
        nowTextAnim = ValueAnimator.ofFloat(nowLineProgress,1f).apply {
            duration = (lineAnimTime * durationRatio).toLong()
            addUpdateListener {
                if (index !in cellRows.indices) return@addUpdateListener
                cellRows[index].factor = it.animatedValue as Float
                nowLineProgress = it.animatedValue as Float
                invalidate()
            }

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator?) {
                    if (!canceled) {
                        index++
                        nowLineProgress = 0f
                        durationRatio = 1f
                        playText()
                    }
                }
                override fun onAnimationStart(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {
                        canceled = true
                }
                override fun onAnimationRepeat(animation: Animator?) {}
            })
        }.also { it.start() }
    }

    fun startAnim() {
        if (launched) return
        launched = true
        playText()
    }

    fun stopAnim() {
        if (index !in cellRows.indices || canceled) return
        nowTextAnim?.cancel()
    }

    fun continueAnim() {
        if (index !in cellRows.indices || !canceled) return
        canceled = false
        durationRatio = 1f - cellRows[index].factor
        playText()
    }

    fun resetAnim() {
        index = 0
        nowTextAnim?.removeAllListeners()
        nowTextAnim?.cancel()
        nowTextAnim = null
        durationRatio = 1f
        nowLineProgress = 0f
        launched = false
        canceled = false
        for (i in cellRows.indices) {
            cellRows[i].factor = 0f
        }
        invalidate()
    }

    private fun initAllLineData() {
        for (i in 0 until layout.lineCount) {
            val start = layout.getLineStart(i)
            val end = layout.getLineEnd(i)
            cellRows.add(CellRowInfo(text.substring(start, end), 0f))
        }
    }
    companion object {
        const val TAG = "GradualShowTextView"
    }
}