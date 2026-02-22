package com.cvgen.service;

import com.cvgen.model.CV;
import com.cvgen.model.Experience;
import com.cvgen.model.Skill;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import jakarta.enterprise.context.ApplicationScoped;
import java.awt.Color;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for generating professional PDF resumes via OpenPDF (LibrePDF).
 *
 * Layout:
 * - Header: name, job title, contact line
 * - Professional Summary
 * - Work Experience (chronological, with company/dates/description)
 * - Skills (grouped with a visual level indicator)
 */
@ApplicationScoped
public class PDFGeneratorService {

    private static final Logger LOG = Logger.getLogger(PDFGeneratorService.class.getName());

    // ─── Color palette ───────────────────────────────────────────────
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235); // #2563eb (blue)
    private static final Color COLOR_DARK = new Color(17, 24, 39); // #111827
    private static final Color COLOR_GRAY = new Color(107, 114, 128);// #6b7280
    private static final Color COLOR_LIGHT_GRAY = new Color(243, 244, 246);// #f3f4f6
    private static final Color COLOR_WHITE = Color.WHITE;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    // ─── Font definitions ────────────────────────────────────────────
    private static Font fontNameHeader() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, COLOR_WHITE);
    }

    private static Font fontJobTitle() {
        return FontFactory.getFont(FontFactory.HELVETICA, 13, COLOR_LIGHT_GRAY);
    }

    private static Font fontSectionHead() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY);
    }

    private static Font fontJobTitleExp() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_DARK);
    }

    private static Font fontCompany() {
        return FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_GRAY);
    }

    private static Font fontBody() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_DARK);
    }

    private static Font fontSmall() {
        return FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_GRAY);
    }

    /**
     * Generates a PDF for the given CV and writes it to the output stream.
     *
     * @param cv     the fully loaded CV entity
     * @param output the servlet response output stream
     */
    public void generatePDF(CV cv, OutputStream output) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, output);
            doc.open();

            // ── Header block ─────────────────────────────────────────
            addHeader(doc, cv);

            // ── Summary ──────────────────────────────────────────────
            if (cv.getSummary() != null && !cv.getSummary().isBlank()) {
                addSection(doc, "PROFESSIONAL SUMMARY");
                Paragraph summary = new Paragraph(cv.getSummary(), fontBody());
                summary.setSpacingAfter(10f);
                doc.add(summary);
            }

            // ── Experience ───────────────────────────────────────────
            if (!cv.getExperiences().isEmpty()) {
                addSection(doc, "WORK EXPERIENCE");
                for (Experience exp : cv.getExperiences()) {
                    addExperienceEntry(doc, exp);
                }
            }

            // ── Skills ───────────────────────────────────────────────
            if (!cv.getSkills().isEmpty()) {
                addSection(doc, "SKILLS");
                addSkillsTable(doc, cv);
            }

            doc.close();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error generating PDF for CV id=" + cv.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ─── Internal builders ───────────────────────────────────────────

    private void addHeader(Document doc, CV cv) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(COLOR_PRIMARY);
        headerCell.setPadding(20);
        headerCell.setBorder(Rectangle.NO_BORDER);

        // Name
        Paragraph name = new Paragraph(cv.getFullName(), fontNameHeader());
        name.setAlignment(Element.ALIGN_LEFT);
        headerCell.addElement(name);

        // Job title
        if (cv.getJobTitle() != null && !cv.getJobTitle().isBlank()) {
            Paragraph jt = new Paragraph(cv.getJobTitle(), fontJobTitle());
            jt.setSpacingBefore(3f);
            headerCell.addElement(jt);
        }

        // Contact line: email | phone | location
        StringBuilder contactLine = new StringBuilder();
        if (cv.getEmail() != null)
            contactLine.append(cv.getEmail()).append("  |  ");
        if (cv.getPhone() != null)
            contactLine.append(cv.getPhone()).append("  |  ");
        if (cv.getLocation() != null)
            contactLine.append(cv.getLocation());

        String contact = contactLine.toString().replaceAll("\\s*\\|\\s*$", "");
        if (!contact.isBlank()) {
            Paragraph contactPara = new Paragraph(contact, fontSmall());
            contactPara.setSpacingBefore(6f);
            headerCell.addElement(contactPara);
        }

        headerTable.addCell(headerCell);
        doc.add(headerTable);
        doc.add(Chunk.NEWLINE);
    }

    private void addSection(Document doc, String title) throws DocumentException {
        Paragraph sectionTitle = new Paragraph(title, fontSectionHead());
        sectionTitle.setSpacingBefore(14f);
        sectionTitle.setSpacingAfter(4f);
        doc.add(sectionTitle);

        // Horizontal rule
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(COLOR_PRIMARY);
        lineCell.setFixedHeight(2f);
        lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        doc.add(line);
        doc.add(new Paragraph(" "));
    }

    private void addExperienceEntry(Document doc, Experience exp) throws DocumentException {
        // Title + Date range on same line
        PdfPTable row = new PdfPTable(new float[] { 3f, 1f });
        row.setWidthPercentage(100);
        row.setSpacingAfter(2f);

        PdfPCell titleCell = new PdfPCell(new Phrase(exp.getJobTitle(), fontJobTitleExp()));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(0);

        PdfPCell dateCell = new PdfPCell(new Phrase(exp.getDateRange(), fontSmall()));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setPadding(0);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        row.addCell(titleCell);
        row.addCell(dateCell);
        doc.add(row);

        // Company + location
        Paragraph company = new Paragraph(exp.getCompany()
                + (exp.getLocation() != null ? " — " + exp.getLocation() : ""), fontCompany());
        company.setSpacingAfter(3f);
        doc.add(company);

        // Description
        if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
            Paragraph desc = new Paragraph(exp.getDescription(), fontBody());
            desc.setSpacingAfter(8f);
            doc.add(desc);
        }
    }

    private void addSkillsTable(Document doc, CV cv) throws DocumentException {
        int cols = 2;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8f);

        for (Skill skill : cv.getSkills()) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingBottom(5f);

            Paragraph skillName = new Paragraph(skill.getName(), fontJobTitleExp());
            cell.addElement(skillName);

            // Level bar using nested table
            PdfPTable barTable = new PdfPTable(new float[] { skill.getLevelPercent(), 100 - skill.getLevelPercent() });
            barTable.setWidthPercentage(80);
            barTable.setSpacingBefore(2f);

            PdfPCell filled = new PdfPCell();
            filled.setBackgroundColor(COLOR_PRIMARY);
            filled.setFixedHeight(4f);
            filled.setBorder(Rectangle.NO_BORDER);
            barTable.addCell(filled);

            PdfPCell empty = new PdfPCell();
            empty.setBackgroundColor(COLOR_LIGHT_GRAY);
            empty.setFixedHeight(4f);
            empty.setBorder(Rectangle.NO_BORDER);
            barTable.addCell(empty);

            cell.addElement(barTable);

            Paragraph levelText = new Paragraph(skill.getLevel().name(), fontSmall());
            cell.addElement(levelText);

            table.addCell(cell);
        }

        // Fill last row if odd number of skills
        if (cv.getSkills().size() % cols != 0) {
            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(emptyCell);
        }

        doc.add(table);
    }
}
