package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.ui.tabs.EducationItem
import com.example.ui.tabs.ExperienceItem
import java.io.ByteArrayOutputStream

object CvPdfGenerator {
    fun generateCvPdf(
        fullName: String,
        phone: String,
        education: List<EducationItem>,
        experience: List<ExperienceItem>,
        skills: List<String>,
        templateId: String = "executive"
    ): ByteArray {
        val pdfDocument = PdfDocument()
        // A4 page size: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Setup paints
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            isAntiAlias = true
        }

        val namePaint = Paint().apply {
            color = Color.parseColor("#1F3FD4") // Primary Blue brand color
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val contactPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9.5f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#1F3FD4") // Primary Blue
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val italicPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#1F3FD4")
            strokeWidth = 1.5f
        }

        var y = 50f
        val marginX = 50f
        val contentWidth = 495f // 595 - 2*50

        if (templateId == "executive") {
            // Executive Template: Centered header
            namePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(fullName.uppercase(), 297.5f, y, namePaint)
            y += 20f

            contactPaint.textAlign = Paint.Align.CENTER
            val contactInfo = "Phone: $phone  |  Email: Available on Request"
            canvas.drawText(contactInfo, 297.5f, y, contactPaint)
            y += 25f
        } else {
            // Modern Template: Left-aligned bold header, right-aligned phone
            namePaint.textAlign = Paint.Align.LEFT
            canvas.drawText(fullName, marginX, y, namePaint)
            y += 18f

            contactPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Phone: $phone", marginX, y, contactPaint)
            y += 22f
        }

        // Draw horizontal line below header
        canvas.drawLine(marginX, y, marginX + contentWidth, y, linePaint)
        y += 20f

        // --- EDUCATION SECTION ---
        canvas.drawText("EDUCATION", marginX, y, headerPaint)
        y += 5f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f })
        y += 15f

        if (education.isEmpty()) {
            canvas.drawText("No education entries listed.", marginX, y, textPaint)
            y += 18f
        } else {
            education.forEach { edu ->
                canvas.drawText("${edu.degree}  -  ${edu.school}", marginX, y, boldPaint)
                val years = "${edu.startYear} - ${edu.endYear}"
                val yearsWidth = italicPaint.measureText(years)
                canvas.drawText(years, 595f - marginX - yearsWidth, y, italicPaint)
                y += 18f
            }
        }
        y += 10f

        // --- EXPERIENCE SECTION ---
        canvas.drawText("PROFESSIONAL EXPERIENCE", marginX, y, headerPaint)
        y += 5f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f })
        y += 15f

        if (experience.isEmpty()) {
            canvas.drawText("No professional experience entries listed.", marginX, y, textPaint)
            y += 18f
        } else {
            experience.forEach { exp ->
                canvas.drawText("${exp.role} at ${exp.company}", marginX, y, boldPaint)
                val years = "${exp.startYear} - ${exp.endYear}"
                val yearsWidth = italicPaint.measureText(years)
                canvas.drawText(years, 595f - marginX - yearsWidth, y, italicPaint)
                y += 15f

                // Draw Achievements (word wrapped simple)
                val achs = exp.achievements.split("\n")
                achs.forEach { line ->
                    if (line.trim().isNotEmpty()) {
                        val formattedLine = if (line.trim().startsWith("•")) line.trim() else "• ${line.trim()}"
                        // Simple wrapping for lines that are too long
                        val words = formattedLine.split(" ")
                        var currentLine = ""
                        words.forEach { word ->
                            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                            if (textPaint.measureText(testLine) < contentWidth - 15) {
                                currentLine = testLine
                            } else {
                                canvas.drawText(currentLine, marginX + 10f, y, textPaint)
                                y += 12f
                                currentLine = "  $word"
                            }
                        }
                        if (currentLine.isNotEmpty()) {
                            canvas.drawText(currentLine, marginX + 10f, y, textPaint)
                            y += 14f
                        }
                    }
                }
                y += 5f
            }
        }
        y += 10f

        // --- SKILLS SECTION ---
        canvas.drawText("CORE SKILLS & EXPERTISE", marginX, y, headerPaint)
        y += 5f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f })
        y += 15f

        if (skills.isEmpty()) {
            canvas.drawText("No core skills listed.", marginX, y, textPaint)
            y += 18f
        } else {
            val skillsLine = skills.joinToString("   •   ")
            val words = skillsLine.split(" ")
            var currentLine = ""
            words.forEach { word ->
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (textPaint.measureText(testLine) < contentWidth) {
                    currentLine = testLine
                } else {
                    canvas.drawText(currentLine, marginX, y, textPaint)
                    y += 14f
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, marginX, y, textPaint)
                y += 14f
            }
        }

        pdfDocument.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }
}
