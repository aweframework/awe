package com.almis.awe.samples.vetclinic.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Everything the invoice PDF prints: the data of the invoice and the way to translate its texts
 *
 * @param number       Invoice number
 * @param date         Invoice date
 * @param statusCode   Status code (PAID, PENDING, OVERDUE), it sets the colour of the status
 * @param statusText   Status, translated
 * @param ownerName    Name of the owner
 * @param ownerAddress Address of the owner (may be empty)
 * @param ownerEmail   Email of the owner (may be empty)
 * @param petName      Name of the pet (may be empty)
 * @param lines        Lines of the invoice
 * @param subtotal     Total before tax
 * @param tax          Tax amount
 * @param total        Total to pay
 * @param locale       Language of the numbers and dates
 * @param text         Translation of a locale key
 */
public record InvoiceDocument(String number, LocalDate date, String statusCode, String statusText, String ownerName,
                              String ownerAddress, String ownerEmail, String petName, List<Line> lines,
                              BigDecimal subtotal, BigDecimal tax, BigDecimal total, Locale locale,
                              UnaryOperator<String> text) {

  /**
   * Line of an invoice
   *
   * @param concept   Concept, translated
   * @param quantity  Quantity
   * @param unitPrice Price of one unit
   * @param amount    Amount of the line
   */
  public record Line(String concept, int quantity, BigDecimal unitPrice, BigDecimal amount) {
  }
}
