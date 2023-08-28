@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "UseJBColor")
package org.jetbrains.nyanprogressbar

import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import sun.swing.SwingUtilities2
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingConstants
import javax.swing.plaf.basic.BasicProgressBarUI

private val ONE_OVER_SEVEN = 1f / 7
private val VIOLET = Color(90, 0, 157)

class NyanProgressBarUi: BasicProgressBarUI() {
    companion object {
        @JvmStatic
        fun createUI(c: JComponent?): NyanProgressBarUi {
            c?.border = JBUI.Borders.empty().asUIResource()
            return NyanProgressBarUi()
        }
    }

    override fun getPreferredSize(c: JComponent?): Dimension {
        return Dimension(super.getPreferredSize(c).width, JBUI.scale(20))
    }

    override fun installListeners() {
        super.installListeners()
        progressBar.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) {
                super.componentShown(e)
            }

            override fun componentHidden(e: ComponentEvent) {
                super.componentHidden(e)
            }
        })
    }

    @Volatile
    private var offset = 0

    @Volatile
    private var offset2 = 0

    @Volatile
    private var velocity = 1
    override fun paintIndeterminate(g2d: Graphics?, c: JComponent) {
        if (g2d !is Graphics2D) {
            return
        }
        val g = g2d
        val b = progressBar.insets // area for border
        val barRectWidth = progressBar.width - (b.right + b.left)
        val barRectHeight = progressBar.height - (b.top + b.bottom)
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return
        }
        //boxRect = getBox(boxRect);
        g.color = JBColor(Gray._240.withAlpha(50), Gray._128.withAlpha(50))
        val w = c.width
        var h = c.preferredSize.height
        if (!isEven(c.height - h)) h++
        val baseRainbowPaint = LinearGradientPaint(
            0f,
            JBUI.scale(2).toFloat(),
            0f,
            (h - JBUI.scale(6)).toFloat(),
            floatArrayOf(
                ONE_OVER_SEVEN * 1,
                ONE_OVER_SEVEN * 2,
                ONE_OVER_SEVEN * 3,
                ONE_OVER_SEVEN * 4,
                ONE_OVER_SEVEN * 5,
                ONE_OVER_SEVEN * 6,
                ONE_OVER_SEVEN * 7
            ),
            arrayOf(
                Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.cyan, Color.blue, VIOLET
            )
        )
        g.paint = baseRainbowPaint
        if (c.isOpaque) {
            g.fillRect(0, (c.height - h) / 2, w, h)
        }
        g.color = JBColor(Gray._165.withAlpha(50), Gray._88.withAlpha(50))
        val config = GraphicsUtil.setupAAPainting(g)
        g.translate(0, (c.height - h) / 2)
        val x = -offset

        val old = g.paint
        g.paint = baseRainbowPaint
        val R = JBUI.scale(8f)
        val R2 = JBUI.scale(9f)
        val containingRoundRect = Area(RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, R, R))
        g.fill(containingRoundRect)

        g.paint = old
        offset = (offset + 1) % getPeriodLength()
        offset2 += velocity
        if (offset2 <= 2) {
            offset2 = 2
            velocity = 1
        } else if (offset2 >= w - JBUI.scale(15)) {
            offset2 = w - JBUI.scale(15)
            velocity = -1
        }
        //        offset2 = (offset2 + 1) % (w - 3);
        val area = Area(Rectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat()))
        area.subtract(Area(RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, R, R)))
        g.paint = Gray._128

        if (c.isOpaque) {
            g.fill(area)
        }
        area.subtract(Area(RoundRectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat(), R2, R2)))
        val parent = c.parent
        val background = if (parent != null) parent.background else UIUtil.getPanelBackground()
        g.paint = background
        //        g.setPaint(baseRainbowPaint);
        if (c.isOpaque) {
            g.fill(area)
        }

        val scaledIcon: Icon = if (velocity > 0) NyanIcons.CAT_ICON else (NyanIcons.RCAT_ICON as ScalableIcon)

        scaledIcon.paintIcon(progressBar, g, offset2 - JBUI.scale(10), -JBUI.scale(6))
        g.draw(RoundRectangle2D.Float(1f, 1f, w - 2f - 1f, h - 2f - 1f, R, R))
        g.translate(0, -(c.height - h) / 2)

        // Deal with possible text painting
        if (progressBar.isStringPainted) {
            if (progressBar.orientation == SwingConstants.HORIZONTAL) {
                paintString(g, b.left, b.top, barRectWidth, barRectHeight, boxRect.x, boxRect.width)
            } else {
                paintString(g, b.left, b.top, barRectWidth, barRectHeight, boxRect.y, boxRect.height)
            }
        }
        config.restore()
    }

    override fun paintDeterminate(g: Graphics, c: JComponent) {
        if (g !is Graphics2D) {
            return
        }
        if (progressBar.orientation != SwingConstants.HORIZONTAL || !c.componentOrientation.isLeftToRight) {
            super.paintDeterminate(g, c)
            return
        }
        val config = GraphicsUtil.setupAAPainting(g)
        val b = progressBar.insets // area for border
        val w = progressBar.width
        var h = progressBar.preferredSize.height
        if (!isEven(c.height - h)) h++
        val barRectWidth = w - (b.right + b.left)
        val barRectHeight = h - (b.top + b.bottom)
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return
        }
        val amountFull = getAmountFull(b, barRectWidth, barRectHeight)
        val parent = c.parent
        val background = if (parent != null) parent.background else UIUtil.getPanelBackground()
        g.setColor(background)
        val g2 = g
        if (c.isOpaque) {
            g.fillRect(0, 0, w, h)
        }
        val R = JBUI.scale(8f)
        val R2 = JBUI.scale(9f)
        val off = JBUI.scale(1f)
        g2.translate(0, (c.height - h) / 2)
        g2.color = progressBar.foreground
        g2.fill(RoundRectangle2D.Float(0f, 0f, w - off, h - off, R2, R2))
        g2.color = background
        g2.fill(RoundRectangle2D.Float(off, off, w - 2f * off - off, h - 2f * off - off, R, R))
        //        g2.setColor(progressBar.getForeground());
        g2.paint = LinearGradientPaint(
            0f,
            JBUI.scale(2).toFloat(),
            0f,
            (h - JBUI.scale(6)).toFloat(),
            floatArrayOf(
                ONE_OVER_SEVEN * 1,
                ONE_OVER_SEVEN * 2,
                ONE_OVER_SEVEN * 3,
                ONE_OVER_SEVEN * 4,
                ONE_OVER_SEVEN * 5,
                ONE_OVER_SEVEN * 6,
                ONE_OVER_SEVEN * 7
            ),
            arrayOf(
                Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.cyan, Color.blue, VIOLET
            )
        )
        NyanIcons.CAT_ICON.paintIcon(progressBar, g2, amountFull - JBUI.scale(10), -JBUI.scale(6))
        g2.fill(
            RoundRectangle2D.Float(
                2f * off,
                2f * off,
                amountFull - JBUI.scale(5f),
                h - JBUI.scale(5f),
                JBUI.scale(7f),
                JBUI.scale(7f)
            )
        )
        g2.translate(0, -(c.height - h) / 2)

        // Deal with possible text painting
        if (progressBar.isStringPainted) {
            paintString(
                g, b.left, b.top,
                barRectWidth, barRectHeight,
                amountFull, b
            )
        }
        config.restore()
    }

    private fun paintString(g: Graphics, x: Int, y: Int, w: Int, h: Int, fillStart: Int, amountFull: Int) {
        if (g !is Graphics2D) {
            return
        }
        val g2 = g
        val progressString = progressBar.string
        g2.font = progressBar.font
        var renderLocation = getStringPlacement(
            g2, progressString,
            x, y, w, h
        )
        val oldClip = g2.clipBounds
        if (progressBar.orientation == SwingConstants.HORIZONTAL) {
            g2.color = selectionBackground
            SwingUtilities2.drawString(
                progressBar, g2, progressString,
                renderLocation.x, renderLocation.y
            )
            g2.color = selectionForeground
            g2.clipRect(fillStart, y, amountFull, h)
            SwingUtilities2.drawString(
                progressBar, g2, progressString,
                renderLocation.x, renderLocation.y
            )
        } else { // VERTICAL
            g2.color = selectionBackground
            val rotate = AffineTransform.getRotateInstance(Math.PI / 2)
            g2.font = progressBar.font.deriveFont(rotate)
            renderLocation = getStringPlacement(
                g2, progressString,
                x, y, w, h
            )
            SwingUtilities2.drawString(
                progressBar, g2, progressString,
                renderLocation.x, renderLocation.y
            )
            g2.color = selectionForeground
            g2.clipRect(x, fillStart, w, amountFull)
            SwingUtilities2.drawString(
                progressBar, g2, progressString,
                renderLocation.x, renderLocation.y
            )
        }
        g2.clip = oldClip
    }

    override fun getBoxLength(availableLength: Int, otherDimension: Int): Int {
        return availableLength
    }

    private fun getPeriodLength(): Int {
        return JBUI.scale(16)
    }

    private fun isEven(value: Int): Boolean {
        return value % 2 == 0
    }
}