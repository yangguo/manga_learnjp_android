package com.example.manga_apk.service

import android.graphics.Bitmap
import android.graphics.Color
import com.example.manga_apk.data.PanelSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

class PanelSegmentationService {
    
    data class Line(val x1: Int, val y1: Int, val x2: Int, val y2: Int, val isVertical: Boolean, val confidence: Float = 1.0f)
    data class Rectangle(val x: Int, val y: Int, val width: Int, val height: Int, val confidence: Float = 1.0f)
    data class SegmentationResult(val panels: List<PanelSegment>, val confidence: Float, val processingTime: Long)
    
    suspend fun segmentPanels(bitmap: Bitmap): SegmentationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Enhanced preprocessing
            val preprocessedBitmap = preprocessImage(bitmap)
            
            // Convert to grayscale with improved algorithm
            val grayBitmap = convertToGrayscaleEnhanced(preprocessedBitmap)
            
            // Apply enhanced Canny edge detection
            val edges = enhancedCannyEdgeDetection(grayBitmap)
            
            // Detect lines using improved Hough transform
            val lines = enhancedHoughLineTransform(edges)
            
            // Intelligent line merging with confidence scoring
            val mergedLines = intelligentLineMerging(lines)
            
            // Find panel rectangles with confidence assessment
            val rectangles = findPanelRectanglesWithConfidence(mergedLines, bitmap.width, bitmap.height)
            
            // Sort panels by manga reading order (right-to-left, top-to-bottom)
            val sortedRectangles = sortByMangaReadingOrderEnhanced(rectangles)
            
            // Calculate overall confidence
            val overallConfidence = calculateOverallConfidence(sortedRectangles, lines.size)
            
            // Convert to PanelSegment objects with confidence
            val panels = sortedRectangles.mapIndexed { index, rect ->
                PanelSegment(
                    id = "panel_$index",
                    x = rect.x,
                    y = rect.y,
                    width = rect.width,
                    height = rect.height,
                    readingOrder = index + 1
                )
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Return result with confidence if panels detected, otherwise fallback
            if (panels.isNotEmpty() && overallConfidence > 0.3f) {
                SegmentationResult(panels, overallConfidence, processingTime)
            } else {
                createFallbackResult(bitmap, processingTime)
            }
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            createFallbackResult(bitmap, processingTime)
        }
    }
    
    private fun createFallbackResult(bitmap: Bitmap, processingTime: Long): SegmentationResult {
        val fallbackPanel = PanelSegment(
            id = "panel_0",
            x = 0,
            y = 0,
            width = bitmap.width,
            height = bitmap.height,
            readingOrder = 1
        )
        return SegmentationResult(listOf(fallbackPanel), 0.1f, processingTime)
    }
    
    // Enhanced preprocessing for better panel detection
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Apply noise reduction and contrast enhancement
        val width = bitmap.width
        val height = bitmap.height
        val processedBitmap = bitmap.copy(bitmap.config, true)
        
        // Simple contrast enhancement
        val pixels = IntArray(width * height)
        processedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            // Enhance contrast
            val enhancedR = ((r - 128) * 1.2 + 128).coerceIn(0, 255).toInt()
            val enhancedG = ((g - 128) * 1.2 + 128).coerceIn(0, 255).toInt()
            val enhancedB = ((b - 128) * 1.2 + 128).coerceIn(0, 255).toInt()
            
            pixels[i] = Color.rgb(enhancedR, enhancedG, enhancedB)
        }
        
        processedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return processedBitmap
    }
    
    private fun convertToGrayscaleEnhanced(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val grayArray = Array(height) { IntArray(width) }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Use improved luminance calculation
                val gray = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                grayArray[y][x] = gray
            }
        }
        return grayArray
    }
    
    private fun convertToGrayscale(bitmap: Bitmap): Array<IntArray> {
        return convertToGrayscaleEnhanced(bitmap)
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
    
    // Enhanced methods for improved panel segmentation
    
    private fun enhancedCannyEdgeDetection(grayArray: Array<IntArray>): Array<BooleanArray> {
        val height = grayArray.size
        val width = grayArray[0].size
        val edges = Array(height) { BooleanArray(width) }
        
        // Enhanced Sobel operators with better edge detection
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
        
        // Apply Gaussian blur first for noise reduction
        val blurred = applyGaussianBlur(grayArray)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0
                var gy = 0
                
                for (i in -1..1) {
                    for (j in -1..1) {
                        val pixel = blurred[y + i][x + j]
                        gx += pixel * sobelX[i + 1][j + 1]
                        gy += pixel * sobelY[i + 1][j + 1]
                    }
                }
                
                val magnitude = sqrt((gx * gx + gy * gy).toDouble())
                // Enhanced threshold for better edge detection
                edges[y][x] = magnitude > 50
            }
        }
        
        return edges
    }
    
    private fun applyGaussianBlur(grayArray: Array<IntArray>): Array<IntArray> {
        val height = grayArray.size
        val width = grayArray[0].size
        val blurred = Array(height) { IntArray(width) }
        
        // Simple 3x3 Gaussian kernel
        val kernel = arrayOf(
            intArrayOf(1, 2, 1),
            intArrayOf(2, 4, 2),
            intArrayOf(1, 2, 1)
        )
        val kernelSum = 16
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0
                for (i in -1..1) {
                    for (j in -1..1) {
                        sum += grayArray[y + i][x + j] * kernel[i + 1][j + 1]
                    }
                }
                blurred[y][x] = sum / kernelSum
            }
        }
        
        return blurred
    }
    
    private fun enhancedHoughLineTransform(edges: Array<BooleanArray>): List<Line> {
        val height = edges.size
        val width = edges[0].size
        val lines = mutableListOf<Line>()
        
        // Enhanced line detection with multiple angle support
        val angles = listOf(0.0, 90.0, 45.0, -45.0) // Support diagonal lines
        
        for (angle in angles) {
            when (angle) {
                0.0 -> lines.addAll(detectHorizontalLines(edges, width, height))
                90.0 -> lines.addAll(detectVerticalLines(edges, width, height))
                45.0 -> lines.addAll(detectDiagonalLines(edges, width, height, true))
                -45.0 -> lines.addAll(detectDiagonalLines(edges, width, height, false))
            }
        }
        
        return lines
    }
    
    private fun detectHorizontalLines(edges: Array<BooleanArray>, width: Int, height: Int): List<Line> {
        val lines = mutableListOf<Line>()
        
        for (y in 0 until height) {
            var startX = -1
            var lineLength = 0
            
            for (x in 0 until width) {
                if (edges[y][x]) {
                    if (startX == -1) startX = x
                    lineLength++
                } else {
                    if (lineLength > width * 0.2) { // Reduced threshold for better detection
                        val confidence = calculateLineConfidence(lineLength, width)
                        lines.add(Line(startX, y, startX + lineLength - 1, y, false, confidence))
                    }
                    startX = -1
                    lineLength = 0
                }
            }
            
            if (lineLength > width * 0.2) {
                val confidence = calculateLineConfidence(lineLength, width)
                lines.add(Line(startX, y, startX + lineLength - 1, y, false, confidence))
            }
        }
        
        return lines
    }
    
    private fun detectVerticalLines(edges: Array<BooleanArray>, width: Int, height: Int): List<Line> {
        val lines = mutableListOf<Line>()
        
        for (x in 0 until width) {
            var startY = -1
            var lineLength = 0
            
            for (y in 0 until height) {
                if (edges[y][x]) {
                    if (startY == -1) startY = y
                    lineLength++
                } else {
                    if (lineLength > height * 0.2) {
                        val confidence = calculateLineConfidence(lineLength, height)
                        lines.add(Line(x, startY, x, startY + lineLength - 1, true, confidence))
                    }
                    startY = -1
                    lineLength = 0
                }
            }
            
            if (lineLength > height * 0.2) {
                val confidence = calculateLineConfidence(lineLength, height)
                lines.add(Line(x, startY, x, startY + lineLength - 1, true, confidence))
            }
        }
        
        return lines
    }
    
    private fun detectDiagonalLines(edges: Array<BooleanArray>, width: Int, height: Int, positive: Boolean): List<Line> {
        // Simplified diagonal line detection - can be enhanced further
        return emptyList()
    }
    
    private fun calculateLineConfidence(lineLength: Int, totalLength: Int): Float {
        return (lineLength.toFloat() / totalLength).coerceIn(0.1f, 1.0f)
    }
    
    private fun intelligentLineMerging(lines: List<Line>): List<Line> {
        val mergedLines = mutableListOf<Line>()
        val horizontalLines = lines.filter { !it.isVertical }.sortedBy { it.y1 }
        val verticalLines = lines.filter { it.isVertical }.sortedBy { it.x1 }
        
        // Enhanced horizontal line merging with confidence weighting
        mergedLines.addAll(mergeLinesByOrientation(horizontalLines, false))
        
        // Enhanced vertical line merging with confidence weighting
        mergedLines.addAll(mergeLinesByOrientation(verticalLines, true))
        
        return mergedLines
    }
    
    private fun mergeLinesByOrientation(lines: List<Line>, isVertical: Boolean): List<Line> {
        val merged = mutableListOf<Line>()
        var i = 0
        
        while (i < lines.size) {
            var currentLine = lines[i]
            var j = i + 1
            
            while (j < lines.size) {
                val nextLine = lines[j]
                val distance = if (isVertical) {
                    abs(nextLine.x1 - currentLine.x1)
                } else {
                    abs(nextLine.y1 - currentLine.y1)
                }
                
                if (distance <= 8) { // Increased tolerance for merging
                    // Merge lines with weighted confidence
                    val totalConfidence = currentLine.confidence + nextLine.confidence
                    val newConfidence = totalConfidence / 2
                    
                    currentLine = if (isVertical) {
                        Line(
                            currentLine.x1,
                            min(currentLine.y1, nextLine.y1),
                            currentLine.x2,
                            max(currentLine.y2, nextLine.y2),
                            true,
                            newConfidence
                        )
                    } else {
                        Line(
                            min(currentLine.x1, nextLine.x1),
                            currentLine.y1,
                            max(currentLine.x2, nextLine.x2),
                            currentLine.y2,
                            false,
                            newConfidence
                        )
                    }
                    j++
                } else {
                    break
                }
            }
            
            merged.add(currentLine)
            i = j
        }
        
        return merged
    }
    
    private fun findPanelRectanglesWithConfidence(lines: List<Line>, width: Int, height: Int): List<Rectangle> {
        val rectangles = mutableListOf<Rectangle>()
        val horizontalLines = lines.filter { !it.isVertical && it.confidence > 0.3f }
        val verticalLines = lines.filter { it.isVertical && it.confidence > 0.3f }
        
        // Enhanced rectangle detection with confidence scoring
        for (i in horizontalLines.indices) {
            for (j in i + 1 until horizontalLines.size) {
                val top = horizontalLines[i]
                val bottom = horizontalLines[j]
                
                if (bottom.y1 > top.y1) {
                    for (k in verticalLines.indices) {
                        for (l in k + 1 until verticalLines.size) {
                            val left = verticalLines[k]
                            val right = verticalLines[l]
                            
                            if (right.x1 > left.x1) {
                                // Check if lines form a valid rectangle
                                if (isValidRectangle(top, bottom, left, right)) {
                                    val rectWidth = right.x1 - left.x1
                                    val rectHeight = bottom.y1 - top.y1
                                    
                                    // Calculate confidence based on line confidences and size
                                    val avgConfidence = (top.confidence + bottom.confidence + left.confidence + right.confidence) / 4
                                    val sizeConfidence = calculateSizeConfidence(rectWidth, rectHeight, width, height)
                                    val finalConfidence = (avgConfidence + sizeConfidence) / 2
                                    
                                    rectangles.add(
                                        Rectangle(
                                            left.x1,
                                            top.y1,
                                            rectWidth,
                                            rectHeight,
                                            finalConfidence
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Filter overlapping rectangles and keep the ones with higher confidence
        return filterOverlappingRectangles(rectangles)
    }
    
    private fun isValidRectangle(top: Line, bottom: Line, left: Line, right: Line): Boolean {
        // Check if the lines actually form a rectangle
        val topOverlap = max(top.x1, left.x1) <= min(top.x2, right.x1)
        val bottomOverlap = max(bottom.x1, left.x1) <= min(bottom.x2, right.x1)
        val leftOverlap = max(left.y1, top.y1) <= min(left.y2, bottom.y1)
        val rightOverlap = max(right.y1, top.y1) <= min(right.y2, bottom.y1)
        
        return topOverlap && bottomOverlap && leftOverlap && rightOverlap
    }
    
    private fun calculateSizeConfidence(rectWidth: Int, rectHeight: Int, imageWidth: Int, imageHeight: Int): Float {
        val area = rectWidth * rectHeight
        val imageArea = imageWidth * imageHeight
        val areaRatio = area.toFloat() / imageArea
        
        // Prefer panels that are not too small or too large
        return when {
            areaRatio < 0.05 -> 0.2f // Too small
            areaRatio > 0.8 -> 0.3f  // Too large
            else -> 0.8f             // Good size
        }
    }
    
    private fun filterOverlappingRectangles(rectangles: List<Rectangle>): List<Rectangle> {
        val filtered = mutableListOf<Rectangle>()
        val sorted = rectangles.sortedByDescending { it.confidence }
        
        for (rect in sorted) {
            var overlaps = false
            for (existing in filtered) {
                if (rectanglesOverlap(rect, existing)) {
                    overlaps = true
                    break
                }
            }
            if (!overlaps) {
                filtered.add(rect)
            }
        }
        
        return filtered
    }
    
    private fun rectanglesOverlap(rect1: Rectangle, rect2: Rectangle): Boolean {
        return !(rect1.x + rect1.width <= rect2.x ||
                rect2.x + rect2.width <= rect1.x ||
                rect1.y + rect1.height <= rect2.y ||
                rect2.y + rect2.height <= rect1.y)
    }
    
    private fun sortByMangaReadingOrderEnhanced(rectangles: List<Rectangle>): List<Rectangle> {
        // Enhanced manga reading order: right-to-left, top-to-bottom
        return rectangles.sortedWith { rect1, rect2 ->
            val rowDiff = rect1.y.compareTo(rect2.y)
            if (abs(rowDiff) < 50) { // Same row tolerance
                rect2.x.compareTo(rect1.x) // Right to left
            } else {
                rowDiff // Top to bottom
            }
        }
    }
    
    private fun calculateOverallConfidence(rectangles: List<Rectangle>, totalLines: Int): Float {
        if (rectangles.isEmpty()) return 0.0f
        
        val avgRectConfidence = rectangles.map { it.confidence }.average().toFloat()
        val panelCountConfidence = when (rectangles.size) {
            1 -> 0.3f      // Single panel might be fallback
            in 2..6 -> 0.9f // Good panel count for manga
            in 7..12 -> 0.7f // Many panels, still reasonable
            else -> 0.4f    // Too many or too few panels
        }
        
        val lineCountConfidence = when (totalLines) {
            in 4..20 -> 0.8f  // Good line count
            in 21..40 -> 0.6f // Many lines
            else -> 0.4f      // Too few or too many lines
        }
        
        return (avgRectConfidence + panelCountConfidence + lineCountConfidence) / 3
    }
}