package com.example.manga_apk.service

import android.graphics.Bitmap
import android.graphics.Color
import com.example.manga_apk.data.PanelSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

class PanelSegmentationService {
    
    data class Line(val x1: Int, val y1: Int, val x2: Int, val y2: Int, val isVertical: Boolean)
    data class Rectangle(val x: Int, val y: Int, val width: Int, val height: Int)
    
    suspend fun segmentPanels(bitmap: Bitmap): List<PanelSegment> = withContext(Dispatchers.Default) {
        try {
            // Convert to grayscale
            val grayBitmap = convertToGrayscale(bitmap)
            
            // Apply Canny edge detection
            val edges = cannyEdgeDetection(grayBitmap)
            
            // Detect lines using Hough transform
            val lines = houghLineTransform(edges)
            
            // Merge similar lines
            val mergedLines = mergeLines(lines)
            
            // Find panel rectangles
            val rectangles = findPanelRectangles(mergedLines, bitmap.width, bitmap.height)
            
            // Sort panels by reading order (right-to-left, top-to-bottom for manga)
            val sortedRectangles = sortByMangaReadingOrder(rectangles)
            
            // Convert to PanelSegment objects
            sortedRectangles.mapIndexed { index, rect ->
                PanelSegment(
                    id = "panel_$index",
                    x = rect.x,
                    y = rect.y,
                    width = rect.width,
                    height = rect.height,
                    readingOrder = index + 1
                )
            }
        } catch (e: Exception) {
            // Fallback: return single panel covering entire image
            listOf(
                PanelSegment(
                    id = "panel_0",
                    x = 0,
                    y = 0,
                    width = bitmap.width,
                    height = bitmap.height,
                    readingOrder = 1
                )
            )
        }
    }
    
    private fun convertToGrayscale(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val grayArray = Array(height) { IntArray(width) }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                grayArray[y][x] = gray
            }
        }
        return grayArray
    }
    
    private fun cannyEdgeDetection(grayArray: Array<IntArray>): Array<BooleanArray> {
        val height = grayArray.size
        val width = grayArray[0].size
        val edges = Array(height) { BooleanArray(width) }
        
        // Simplified Canny edge detection
        // Apply Sobel operator for gradient calculation
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        
        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0
                var gy = 0
                
                for (i in -1..1) {
                    for (j in -1..1) {
                        val pixel = grayArray[y + i][x + j]
                        gx += pixel * sobelX[i + 1][j + 1]
                        gy += pixel * sobelY[i + 1][j + 1]
                    }
                }
                
                val magnitude = sqrt((gx * gx + gy * gy).toDouble())
                edges[y][x] = magnitude > 50 // Threshold for edge detection
            }
        }
        
        return edges
    }
    
    private fun houghLineTransform(edges: Array<BooleanArray>): List<Line> {
        val height = edges.size
        val width = edges[0].size
        val lines = mutableListOf<Line>()
        
        // Detect horizontal lines
        for (y in 0 until height) {
            var startX = -1
            var lineLength = 0
            
            for (x in 0 until width) {
                if (edges[y][x]) {
                    if (startX == -1) startX = x
                    lineLength++
                } else {
                    if (lineLength > width * 0.3) { // Line must be at least 30% of width
                        lines.add(Line(startX, y, startX + lineLength - 1, y, false))
                    }
                    startX = -1
                    lineLength = 0
                }
            }
            
            if (lineLength > width * 0.3) {
                lines.add(Line(startX, y, startX + lineLength - 1, y, false))
            }
        }
        
        // Detect vertical lines
        for (x in 0 until width) {
            var startY = -1
            var lineLength = 0
            
            for (y in 0 until height) {
                if (edges[y][x]) {
                    if (startY == -1) startY = y
                    lineLength++
                } else {
                    if (lineLength > height * 0.3) { // Line must be at least 30% of height
                        lines.add(Line(x, startY, x, startY + lineLength - 1, true))
                    }
                    startY = -1
                    lineLength = 0
                }
            }
            
            if (lineLength > height * 0.3) {
                lines.add(Line(x, startY, x, startY + lineLength - 1, true))
            }
        }
        
        return lines
    }
    
    private fun mergeLines(lines: List<Line>): List<Line> {
        val mergedLines = mutableListOf<Line>()
        val horizontalLines = lines.filter { !it.isVertical }.sortedBy { it.y1 }
        val verticalLines = lines.filter { it.isVertical }.sortedBy { it.x1 }
        
        // Merge horizontal lines
        var i = 0
        while (i < horizontalLines.size) {
            var currentLine = horizontalLines[i]
            var j = i + 1
            
            while (j < horizontalLines.size && abs(horizontalLines[j].y1 - currentLine.y1) <= 5) {
                // Merge lines that are close together
                currentLine = Line(
                    min(currentLine.x1, horizontalLines[j].x1),
                    currentLine.y1,
                    max(currentLine.x2, horizontalLines[j].x2),
                    currentLine.y2,
                    false
                )
                j++
            }
            
            mergedLines.add(currentLine)
            i = j
        }
        
        // Merge vertical lines
        i = 0
        while (i < verticalLines.size) {
            var currentLine = verticalLines[i]
            var j = i + 1
            
            while (j < verticalLines.size && abs(verticalLines[j].x1 - currentLine.x1) <= 5) {
                currentLine = Line(
                    currentLine.x1,
                    min(currentLine.y1, verticalLines[j].y1),
                    currentLine.x2,
                    max(currentLine.y2, verticalLines[j].y2),
                    true
                )
                j++
            }
            
            mergedLines.add(currentLine)
            i = j
        }
        
        return mergedLines
    }
    
    private fun findPanelRectangles(lines: List<Line>, imageWidth: Int, imageHeight: Int): List<Rectangle> {
        val horizontalLines = lines.filter { !it.isVertical }.sortedBy { it.y1 }
        val verticalLines = lines.filter { it.isVertical }.sortedBy { it.x1 }
        
        val rectangles = mutableListOf<Rectangle>()
        
        // Add image boundaries as lines
        val allHorizontalLines = (horizontalLines + listOf(
            Line(0, 0, imageWidth, 0, false),
            Line(0, imageHeight, imageWidth, imageHeight, false)
        )).sortedBy { it.y1 }
        
        val allVerticalLines = (verticalLines + listOf(
            Line(0, 0, 0, imageHeight, true),
            Line(imageWidth, 0, imageWidth, imageHeight, true)
        )).sortedBy { it.x1 }
        
        // Find rectangles formed by line intersections
        for (i in 0 until allHorizontalLines.size - 1) {
            for (j in 0 until allVerticalLines.size - 1) {
                val top = allHorizontalLines[i].y1
                val bottom = allHorizontalLines[i + 1].y1
                val left = allVerticalLines[j].x1
                val right = allVerticalLines[j + 1].x1
                
                val width = right - left
                val height = bottom - top
                
                // Filter out panels that are too small
                if (width > imageWidth * 0.1 && height > imageHeight * 0.1) {
                    rectangles.add(Rectangle(left, top, width, height))
                }
            }
        }
        
        return rectangles.distinctBy { "${it.x},${it.y},${it.width},${it.height}" }
    }
    
    private fun sortByMangaReadingOrder(rectangles: List<Rectangle>): List<Rectangle> {
        // Sort by manga reading order: right-to-left, top-to-bottom
        return rectangles.sortedWith { a, b ->
            val rowDiff = a.y.compareTo(b.y)
            if (abs(rowDiff) < 50) { // Same row (within 50px)
                b.x.compareTo(a.x) // Right to left
            } else {
                rowDiff // Top to bottom
            }
        }
    }
}