package com.almis.awe.samples.vetclinic.invoice;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The invoice PDF: a valid document that holds the data of the invoice and grows by pages with the lines
 */
class InvoicePdfRendererTest {

  private final InvoicePdfRenderer renderer = new InvoicePdfRenderer();

  @Test
  void rendersAPdfWithTheDataOfTheInvoice() throws IOException {
    byte[] pdf = renderer.render(invoice(3));

    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    PdfReader reader = new PdfReader(pdf);
    assertThat(reader.getNumberOfPages()).isEqualTo(1);
    String text = new PdfTextExtractor(reader).getTextFromPage(1);
    assertThat(text).contains("HP-000012", "Ana Torres", "Luna", "Line 1", "Line 3", "VET_PDF_SUBTOTAL", "VET_PDF_TOTAL");
    assertThat(text).contains("121,00").contains("21,00");
  }

  @Test
  void addsPagesWhenThereAreManyLines() throws IOException {
    PdfReader reader = new PdfReader(renderer.render(invoice(80)));

    assertThat(reader.getNumberOfPages()).isGreaterThan(1);
  }

  private InvoiceDocument invoice(int lineCount) {
    List<InvoiceDocument.Line> lines = new ArrayList<>();
    for (int index = 1; index <= lineCount; index++) {
      lines.add(new InvoiceDocument.Line("Line " + index, 1, new BigDecimal("1.00"), new BigDecimal("1.00")));
    }
    return new InvoiceDocument("HP-000012", LocalDate.of(2026, 11, 3), "PAID", "Paid", "Ana Torres", "Calle Mayor 5, Madrid",
      "ana@mail.example", "Luna", lines, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"),
      Locale.forLanguageTag("es-ES"), key -> key);
  }
}
