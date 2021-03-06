package com.siziksu.bluetooth.ui.view.custom

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.siziksu.bluetooth.R
import com.siziksu.bluetooth.common.extension.toRadians
import kotlin.math.cos
import kotlin.math.sin


class RoundedView : View {

    companion object {

        private const val PADDING = 0f
        private const val START_ANGLE = 120f
        private const val SWEEP_ANGLE = 300f
    }

    @JvmField
    var listener = {}

    private var layoutHeight = 0f
    private var layoutWidth = 0f

    private var barBounds = RectF()
    private val barPaint = Paint()
    private var barColor = 0xFFFFD900.toInt()

    private var rimBounds = RectF()
    private val rimPaint = Paint()
    private var rimWidth = 20f
    private var rimColor = 0x80FFFFFF.toInt()

    private val whitePaint = Paint()
    private val whiteColor = -0x1

    private val textPaint = Paint()
    private var textX = 0f
    private var textY = 0f
    private var textMargin = 0f
    private var textSize = 30f
    private var textStyle = Typeface.NORMAL
    private var textColor = 0xFFFFFFFF.toInt()
    private var text = "0"

    private val textBackPaint = Paint()
    private var textBackBounds = RectF()
    private var textBackColor = 0xaa000000.toInt()

    private var potNumber = 0

    private var canMove = false

    private val currentPositionPaint = Paint()
    private var currentPositionColor = 0x80FFFFFF.toInt()
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var halfRadius = 0f

    private var currentPointXa = 0f
    private var currentPointYa = 0f
    private var currentPointXb = 0f
    private var currentPointYb = 0f

    private var validMidiValue = 0
    private var lastValidMidiValueSent = 0

    private val roundedViewMaths by lazy { RoundedViewMaths() }

    private var drawable: Drawable? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            parseAttributes(context.obtainStyledAttributes(attrs, R.styleable.RoundedView))
        }
        drawable = ContextCompat.getDrawable(getContext(), R.drawable.hud)
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT & WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutWidth = w.toFloat()
        layoutHeight = h.toFloat()
        setupBounds()
        setupPaints()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> if (!canMove) {
                canMove = true
                roundedViewMaths.onActionDown(event.x - centerX, event.y - centerY)
            }
            MotionEvent.ACTION_MOVE -> if (canMove) {
                roundedViewMaths.onActionMove(event.x - centerX, event.y - centerY)
                setText(roundedViewMaths.value())
                invalidate()
            }
            MotionEvent.ACTION_UP -> canMove = false
            else -> {
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        drawable?.draw(canvas)
        // Draw the rim
        if (rimWidth > 0) {
            canvas.drawArc(rimBounds, START_ANGLE, SWEEP_ANGLE, false, rimPaint)
        }
        // Draw the bar
        canvas.drawArc(barBounds, START_ANGLE, roundedViewMaths.value(), false, barPaint)
        // Draw current value line
        calculateLinePointsForCurrentValue(roundedViewMaths.value())
        canvas.drawLine(currentPointXb, currentPointYb, currentPointXa, currentPointYa, currentPositionPaint)
        // Draw horizontal line
        canvas.drawLine((centerX - halfRadius), centerY, (centerX + halfRadius), centerY, whitePaint)
        // Draw black background for text
        canvas.drawArc(textBackBounds, START_ANGLE, SWEEP_ANGLE, false, textBackPaint)
        // Draw the text below horizontal line
        calculateTextCoordinates()
        canvas.drawText(text, textX, textY, textPaint)
        if (potNumber != 0) {
            // Draw pot number
            calculatePotTextCoordinates()
            canvas.drawText("Pot $potNumber", textX, textY, textPaint)
        }
    }

    fun getValue(): Int {
        return text.toInt()
    }

    fun setValue(value: Int) {
        text = value.toString()
        roundedViewMaths.update(value)
    }

    private fun setupBounds() {
        textX = 0f
        textY = 0f
        centerX = (measuredWidth / 2).toFloat()
        centerY = (measuredHeight / 2).toFloat()

        // Width should equal to Height, find the min value to setup the circle
        val minWidthHeightValue = minOf(layoutWidth, layoutHeight)

        radius = minWidthHeightValue / 2
        halfRadius = radius / 2

        // To compensate the values when the view is not a square
        val h: Float = (if (layoutHeight < layoutWidth) layoutWidth - layoutHeight else 0f) / 2
        val v: Float = (if (layoutWidth < layoutHeight) layoutHeight - layoutWidth else 0f) / 2

        var left: Float
        var top: Float
        var right: Float
        var bottom: Float

        left = (h + PADDING + rimWidth / 2)
        top = (v + PADDING + rimWidth / 2)
        right = (h - PADDING - rimWidth / 2 + minWidthHeightValue)
        bottom = (v - PADDING - rimWidth / 2 + minWidthHeightValue)
        rimBounds = RectF(left, top, right, bottom)

        left = (h + PADDING + rimWidth + halfRadius)
        top = (v + PADDING + rimWidth + halfRadius)
        right = (h - PADDING - rimWidth - halfRadius) + minWidthHeightValue
        bottom = (v - PADDING - rimWidth - halfRadius) + minWidthHeightValue
        barBounds = RectF(left, top, right, bottom)

        left = (h + PADDING + halfRadius)
        top = (v + PADDING + halfRadius)
        right = (h - PADDING - halfRadius) + minWidthHeightValue
        bottom = (v - PADDING - halfRadius) + minWidthHeightValue
        textBackBounds = RectF(left, top, right, bottom)

        left = h + PADDING
        top = v + PADDING
        right = (h - PADDING) + minWidthHeightValue
        bottom = (v - PADDING) + minWidthHeightValue
        drawable?.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    private fun setupPaints() {
        barPaint.isAntiAlias = true
        barPaint.color = barColor
        barPaint.strokeWidth = halfRadius
        barPaint.style = Paint.Style.STROKE

        if (rimWidth > 0) {
            rimPaint.isAntiAlias = true
            rimPaint.color = rimColor
            rimPaint.strokeWidth = rimWidth
            rimPaint.style = Paint.Style.STROKE
        }

        textPaint.isAntiAlias = true
        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, textStyle)
        textPaint.textAlign = Paint.Align.CENTER

        whitePaint.isAntiAlias = true
        whitePaint.color = whiteColor
        whitePaint.textSize = textSize
        whitePaint.strokeWidth = 2f
        whitePaint.typeface = Typeface.create(Typeface.DEFAULT, textStyle)
        whitePaint.textAlign = Paint.Align.CENTER

        currentPositionPaint.isAntiAlias = true
        currentPositionPaint.color = currentPositionColor
        currentPositionPaint.strokeWidth = 6f
        currentPositionPaint.style = Paint.Style.STROKE

        textBackPaint.isAntiAlias = true
        textBackPaint.color = textBackColor
        textBackPaint.style = Paint.Style.FILL
    }

    private fun calculateTextCoordinates() {
        textX = centerX
        textY = (centerY - (textPaint.descent() + textPaint.ascent()) / 2 + textMargin)
    }

    private fun calculatePotTextCoordinates() {
        textY = (centerY - (textPaint.descent() + textPaint.ascent()) / 2 - textMargin)
    }

    private fun calculateLinePointsForCurrentValue(value: Float) {
        currentPointXa = centerX + ((radius - rimWidth) * cos((value + START_ANGLE).toRadians()))
        currentPointYa = centerY + ((radius - rimWidth) * sin((value + START_ANGLE).toRadians()))
        currentPointXb = centerX // + ((radius - halfRadius) * cos((value + START_ANGLE).toRadians()))
        currentPointYb = centerY // + ((radius - halfRadius) * sin((value + START_ANGLE).toRadians()))
    }

    private fun setText(value: Float) {
        validMidiValue = value.toInt()
        validMidiValue = (validMidiValue * 0.4234f).toInt()
        text = if (validMidiValue in 0..127) validMidiValue.toString() else text
        if (lastValidMidiValueSent != validMidiValue) {
            listener()
            lastValidMidiValueSent = validMidiValue
        }
    }

    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param attributes the attributes to parse
     */
    private fun parseAttributes(attributes: TypedArray) {
        barColor = attributes.getColor(R.styleable.RoundedView_barColor, barColor)
        rimWidth = attributes.getDimension(R.styleable.RoundedView_rimWidth, rimWidth)
        textMargin = attributes.getDimension(R.styleable.RoundedView_textMargin, textMargin)
        textColor = attributes.getColor(R.styleable.RoundedView_android_textColor, textColor)
        textSize = attributes.getDimension(R.styleable.RoundedView_android_textSize, textSize)
        textStyle = attributes.getInt(R.styleable.RoundedView_android_textStyle, textStyle)
        potNumber = attributes.getInt(R.styleable.RoundedView_potNumber, potNumber)
        attributes.recycle()
    }
}