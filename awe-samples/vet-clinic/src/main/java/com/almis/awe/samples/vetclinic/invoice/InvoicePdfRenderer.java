package com.almis.awe.samples.vetclinic.invoice;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;

/**
 * Draws the invoice of the Happy Paws clinic as an A4 PDF with OpenPDF (the PDF library JasperReports already brings to
 * the AWE classpath): brand header with the paw mark, billing and invoice details, table of lines, totals and footer.
 * The colours are the ones of the clinic logo files (<code>static/images/logo</code>): a PDF has no CSS tokens
 */
public class InvoicePdfRenderer {

  private static final Color ORANGE = new Color(0xc2, 0x41, 0x0c);
  private static final Color ORANGE_SOFT = new Color(0xff, 0xf1, 0xe8);
  private static final Color INK = new Color(0x0f, 0x17, 0x2a);
  private static final Color MUTED = new Color(0x64, 0x74, 0x8b);
  private static final Color RULE = new Color(0xe2, 0xe8, 0xf0);
  private static final Color ZEBRA = new Color(0xf8, 0xfa, 0xfc);
  private static final Color WHITE = Color.WHITE;
  private static final Color PAID = new Color(0x15, 0x80, 0x3d);
  private static final Color PENDING = new Color(0xb4, 0x53, 0x09);
  private static final Color OVERDUE = new Color(0xb9, 0x1c, 0x1c);

  private static final float MARGIN = 48f;
  private static final float PAW_SIZE = 40f;

  /**
   * Render an invoice
   *
   * @param invoice Invoice to print
   * @return Bytes of the PDF
   */
  public byte[] render(InvoiceDocument invoice) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, 80f);
    try {
      PdfWriter writer = PdfWriter.getInstance(document, output);
      writer.setPageEvent(new Footer(invoice));
      document.addTitle(invoice.text().apply("VET_PDF_INVOICE") + " " + invoice.number());
      document.addAuthor(invoice.text().apply("VET_PDF_CLINIC_NAME"));
      document.open();
      drawHeader(writer.getDirectContent(), document, invoice);
      document.add(spacer(14f));
      document.add(parties(invoice));
      document.add(spacer(22f));
      document.add(linesTable(invoice));
      document.add(spacer(14f));
      document.add(totals(invoice));
      document.close();
    } catch (DocumentException | IOException exc) {
      throw new IllegalStateException("The invoice " + invoice.number() + " could not be rendered", exc);
    }
    return output.toByteArray();
  }

  // ---- Header: paw mark, clinic name, title, number and status

  private void drawHeader(PdfContentByte canvas, Document document, InvoiceDocument invoice) throws DocumentException, IOException {
    float top = document.getPageSize().getHeight() - MARGIN;
    float pawBottom = top - PAW_SIZE;
    drawPaw(canvas, MARGIN, pawBottom, PAW_SIZE, ORANGE);

    BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
    BaseFont regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
    canvas.beginText();
    canvas.setColorFill(INK);
    canvas.setFontAndSize(bold, 22f);
    canvas.setTextMatrix(MARGIN + PAW_SIZE + 12f, pawBottom + 20f);
    canvas.showText(invoice.text().apply("VET_PDF_CLINIC_NAME"));
    canvas.setColorFill(MUTED);
    canvas.setFontAndSize(regular, 9.5f);
    canvas.setTextMatrix(MARGIN + PAW_SIZE + 12f, pawBottom + 6f);
    canvas.showText(invoice.text().apply("VET_PDF_CLINIC_SUBTITLE"));
    canvas.endText();

    // Title, number and status, aligned to the right margin
    float right = document.getPageSize().getWidth() - MARGIN;
    canvas.beginText();
    canvas.setColorFill(INK);
    canvas.setFontAndSize(bold, 26f);
    canvas.showTextAligned(Element.ALIGN_RIGHT, invoice.text().apply("VET_PDF_INVOICE"), right, top - 22f, 0);
    canvas.setColorFill(ORANGE);
    canvas.setFontAndSize(bold, 11f);
    canvas.showTextAligned(Element.ALIGN_RIGHT, invoice.number(), right, top - 40f, 0);
    canvas.endText();

    // Orange rule below the header
    float ruleY = pawBottom - 16f;
    canvas.setColorFill(ORANGE);
    canvas.roundRectangle(MARGIN, ruleY, document.getPageSize().getWidth() - 2 * MARGIN, 3f, 1.5f);
    canvas.fill();
    drawStatusPill(canvas, invoice, right, ruleY - 28f, bold);

    // Move the text cursor below the header
    float cursorTarget = ruleY - 36f;
    document.add(spacer(document.getPageSize().getHeight() - MARGIN - cursorTarget - 1f));
  }

  private void drawStatusPill(PdfContentByte canvas, InvoiceDocument invoice, float right, float bottom, BaseFont bold) {
    String label = invoice.statusText() == null ? "" : invoice.statusText();
    float fontSize = 9f;
    float width = bold.getWidthPoint(label, fontSize) + 22f;
    Color tone = statusTone(invoice.statusCode());
    canvas.setColorFill(tone);
    canvas.roundRectangle(right - width, bottom, width, 18f, 9f);
    canvas.fill();
    canvas.beginText();
    canvas.setColorFill(WHITE);
    canvas.setFontAndSize(bold, fontSize);
    canvas.showTextAligned(Element.ALIGN_CENTER, label, right - width / 2f, bottom + 5.5f, 0);
    canvas.endText();
  }

  private static Color statusTone(String code) {
    if ("PAID".equals(code)) {
      return PAID;
    }
    return "OVERDUE".equals(code) ? OVERDUE : PENDING;
  }

  /**
   * Draw the paw mark of the clinic (the shapes of <code>paw-mark.svg</code>, 64 x 64 units, y axis down)
   */
  static void drawPaw(PdfContentByte canvas, float x, float y, float size, Color color) {
    float scale = size / 64f;
    canvas.saveState();
    canvas.setColorFill(color);
    // The SVG y axis points down: flip it and move the origin to the bottom left corner of the mark
    canvas.concatCTM(scale, 0, 0, -scale, x, y + size);
    toe(canvas, 12, 26, -20);
    toe(canvas, 25, 13, 0);
    toe(canvas, 39, 13, 0);
    toe(canvas, 52, 26, 20);
    canvas.moveTo(32, 30);
    canvas.curveTo(20, 30, 11, 41, 11, 51);
    canvas.curveTo(11, 59, 18, 62, 24, 61);
    canvas.curveTo(29, 60, 29, 58, 32, 58);
    canvas.curveTo(35, 58, 35, 60, 40, 61);
    canvas.curveTo(46, 62, 53, 59, 53, 51);
    canvas.curveTo(53, 41, 44, 30, 32, 30);
    canvas.closePath();
    canvas.fill();
    canvas.restoreState();
  }

  private static void toe(PdfContentByte canvas, float centerX, float centerY, float degrees) {
    canvas.saveState();
    double radians = Math.toRadians(degrees);
    float cos = (float) Math.cos(radians);
    float sin = (float) Math.sin(radians);
    // Rotation around the centre of the toe
    canvas.concatCTM(cos, sin, -sin, cos, centerX - centerX * cos + centerY * sin, centerY - centerX * sin - centerY * cos);
    canvas.ellipse(centerX - 7, centerY - 10, centerX + 7, centerY + 10);
    canvas.fill();
    canvas.restoreState();
  }

  // ---- Billing and invoice details

  private PdfPTable parties(InvoiceDocument invoice) throws DocumentException, IOException {
    PdfPTable table = new PdfPTable(new float[]{1f, 1f});
    table.setWidthPercentage(100f);
    table.addCell(partyCell(invoice.text().apply("VET_PDF_BILLED_TO"), new String[]{
      invoice.ownerName(), invoice.ownerAddress(), invoice.ownerEmail()}, true));
    table.addCell(partyCell(invoice.text().apply("VET_PDF_DETAILS"), new String[]{
      invoice.text().apply("VET_PDF_DATE") + ": " + formatDate(invoice),
      invoice.petName() == null || invoice.petName().isBlank() ? "" : invoice.text().apply("VET_PDF_PATIENT") + ": " + invoice.petName(),
      invoice.text().apply("VET_PDF_STATUS") + ": " + invoice.statusText()}, false));
    return table;
  }

  private PdfPCell partyCell(String title, String[] lines, boolean first) throws DocumentException, IOException {
    Paragraph content = new Paragraph();
    content.add(new Phrase(title.toUpperCase(Locale.ROOT) + "\n", font(BaseFont.HELVETICA_BOLD, 8f, ORANGE)));
    boolean head = first;
    for (String line : lines) {
      if (line != null && !line.isBlank()) {
        content.add(new Phrase(line + "\n", head ? font(BaseFont.HELVETICA_BOLD, 12f, INK) : font(BaseFont.HELVETICA, 10f, INK)));
        head = false;
      }
    }
    content.setLeading(15f);
    PdfPCell cell = new PdfPCell(content);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPadding(0f);
    cell.setPaddingRight(16f);
    return cell;
  }

  // ---- Lines

  private PdfPTable linesTable(InvoiceDocument invoice) throws DocumentException, IOException {
    PdfPTable table = new PdfPTable(new float[]{5.2f, 1.2f, 2f, 2f});
    table.setWidthPercentage(100f);
    table.setHeaderRows(1);
    table.setSplitLate(false);
    addHeaderCell(table, invoice.text().apply("VET_PDF_CONCEPT"), Element.ALIGN_LEFT);
    addHeaderCell(table, invoice.text().apply("VET_PDF_QUANTITY"), Element.ALIGN_RIGHT);
    addHeaderCell(table, invoice.text().apply("VET_PDF_UNIT_PRICE"), Element.ALIGN_RIGHT);
    addHeaderCell(table, invoice.text().apply("VET_PDF_AMOUNT"), Element.ALIGN_RIGHT);
    int row = 0;
    for (InvoiceDocument.Line line : invoice.lines()) {
      Color background = row++ % 2 == 0 ? WHITE : ZEBRA;
      addBodyCell(table, line.concept(), Element.ALIGN_LEFT, background);
      addBodyCell(table, String.valueOf(line.quantity()), Element.ALIGN_RIGHT, background);
      addBodyCell(table, money(invoice, line.unitPrice()), Element.ALIGN_RIGHT, background);
      addBodyCell(table, money(invoice, line.amount()), Element.ALIGN_RIGHT, background);
    }
    return table;
  }

  private void addHeaderCell(PdfPTable table, String text, int alignment) throws DocumentException, IOException {
    PdfPCell cell = new PdfPCell(new Phrase(text, font(BaseFont.HELVETICA_BOLD, 9f, WHITE)));
    cell.setBackgroundColor(INK);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setHorizontalAlignment(alignment);
    cell.setPadding(8f);
    cell.setPaddingLeft(10f);
    cell.setPaddingRight(10f);
    table.addCell(cell);
  }

  private void addBodyCell(PdfPTable table, String text, int alignment, Color background) throws DocumentException, IOException {
    PdfPCell cell = new PdfPCell(new Phrase(text, font(BaseFont.HELVETICA, 10f, INK)));
    cell.setBackgroundColor(background);
    cell.setBorder(Rectangle.BOTTOM);
    cell.setBorderColor(RULE);
    cell.setBorderWidth(0.75f);
    cell.setHorizontalAlignment(alignment);
    cell.setPadding(8f);
    cell.setPaddingLeft(10f);
    cell.setPaddingRight(10f);
    table.addCell(cell);
  }

  // ---- Totals

  private PdfPTable totals(InvoiceDocument invoice) throws DocumentException, IOException {
    PdfPTable table = new PdfPTable(new float[]{1.1f, 1f});
    table.setWidthPercentage(48f);
    table.setHorizontalAlignment(Element.ALIGN_RIGHT);
    addTotalRow(table, invoice.text().apply("VET_PDF_SUBTOTAL"), money(invoice, invoice.subtotal()));
    addTotalRow(table, invoice.text().apply("VET_PDF_VAT"), money(invoice, invoice.tax()));

    PdfPCell label = new PdfPCell(new Phrase(invoice.text().apply("VET_PDF_TOTAL"), font(BaseFont.HELVETICA_BOLD, 12f, WHITE)));
    PdfPCell value = new PdfPCell(new Phrase(money(invoice, invoice.total()), font(BaseFont.HELVETICA_BOLD, 14f, WHITE)));
    for (PdfPCell cell : new PdfPCell[]{label, value}) {
      cell.setBackgroundColor(ORANGE);
      cell.setBorder(Rectangle.NO_BORDER);
      cell.setPadding(10f);
      cell.setPaddingTop(11f);
      cell.setPaddingBottom(11f);
    }
    value.setHorizontalAlignment(Element.ALIGN_RIGHT);
    PdfPCell gap = new PdfPCell(new Phrase(" ", font(BaseFont.HELVETICA, 4f, INK)));
    gap.setColspan(2);
    gap.setBorder(Rectangle.NO_BORDER);
    table.addCell(gap);
    table.addCell(label);
    table.addCell(value);
    table.setKeepTogether(true);
    return table;
  }

  private void addTotalRow(PdfPTable table, String label, String value) throws DocumentException, IOException {
    PdfPCell labelCell = new PdfPCell(new Phrase(label, font(BaseFont.HELVETICA, 10f, MUTED)));
    PdfPCell valueCell = new PdfPCell(new Phrase(value, font(BaseFont.HELVETICA, 10f, INK)));
    for (PdfPCell cell : new PdfPCell[]{labelCell, valueCell}) {
      cell.setBorder(Rectangle.NO_BORDER);
      cell.setPadding(6f);
      cell.setPaddingLeft(10f);
      cell.setPaddingRight(10f);
    }
    valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(labelCell);
    table.addCell(valueCell);
  }

  // ---- Helpers

  private static Paragraph spacer(float height) {
    Paragraph spacer = new Paragraph(" ");
    spacer.setLeading(Math.max(height, 1f));
    spacer.getFont().setSize(1f);
    return spacer;
  }

  private static Font font(String name, float size, Color color) throws DocumentException, IOException {
    return new Font(BaseFont.createFont(name, BaseFont.WINANSI, false), size, Font.NORMAL, color);
  }

  private static String money(InvoiceDocument invoice, BigDecimal amount) {
    NumberFormat format = NumberFormat.getCurrencyInstance(invoice.locale());
    format.setCurrency(Currency.getInstance("EUR"));
    return format.format(amount);
  }

  private static String formatDate(InvoiceDocument invoice) {
    return invoice.date().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(invoice.locale()));
  }

  /**
   * Footer of every page: clinic contact, thanks and "page x of y"
   */
  private static final class Footer extends PdfPageEventHelper {
    private final InvoiceDocument invoice;
    private PdfTemplate total;
    private BaseFont font;

    private Footer(InvoiceDocument invoice) {
      this.invoice = invoice;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
      total = writer.getDirectContent().createTemplate(30f, 12f);
      try {
        font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
      } catch (DocumentException | IOException exc) {
        throw new IllegalStateException("The footer font is not available", exc);
      }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      PdfContentByte canvas = writer.getDirectContent();
      float width = document.getPageSize().getWidth();
      float right = width - MARGIN;
      canvas.setColorFill(RULE);
      canvas.rectangle(MARGIN, 58f, width - 2 * MARGIN, 0.75f);
      canvas.fill();
      Font small = new Font(font, 8f, Font.NORMAL, MUTED);
      ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(invoice.text().apply("VET_PDF_THANKS"),
        new Font(font, 9f, Font.NORMAL, ORANGE)), MARGIN, 42f, 0);
      ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(invoice.text().apply("VET_CONTACT_ADDRESS") + "  |  "
        + invoice.text().apply("VET_CONTACT_PHONE") + "  |  " + invoice.text().apply("VET_CONTACT_EMAIL"), small), MARGIN, 28f, 0);
      String page = invoice.text().apply("VET_PDF_PAGE") + " " + writer.getPageNumber() + " / ";
      float pageWidth = font.getWidthPoint(page, 8f);
      ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(page, small), right - pageWidth - 12f, 28f, 0);
      canvas.addTemplate(total, right - 12f, 28f);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
      total.beginText();
      total.setFontAndSize(font, 8f);
      total.setColorFill(MUTED);
      total.setTextMatrix(0, 0);
      total.showText(String.valueOf(writer.getPageNumber() - 1));
      total.endText();
    }
  }
}
