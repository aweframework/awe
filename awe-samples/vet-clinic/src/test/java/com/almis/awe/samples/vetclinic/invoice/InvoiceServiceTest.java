package com.almis.awe.samples.vetclinic.invoice;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.util.data.DataListUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The invoice service builds the document from the rows of the invoice queries, and prints nothing when a query finds
 * no invoice: for a pet owner that is the answer for the invoice of another owner
 */
class InvoiceServiceTest {

  @Test
  void buildsTheDocumentFromTheRowsOfTheQueries() throws AWException {
    InvoiceDocument document = InvoiceService.toDocument(header(), lines(), Locale.ENGLISH, key -> key);

    assertThat(document.number()).isEqualTo("HP-000012");
    assertThat(document.date()).isEqualTo(LocalDate.of(2026, 11, 3));
    assertThat(document.statusCode()).isEqualTo("PAID");
    assertThat(document.ownerName()).isEqualTo("Ana Torres");
    assertThat(document.ownerAddress()).isEqualTo("Calle Mayor 5, Madrid");
    assertThat(document.petName()).isEqualTo("Luna");
    assertThat(document.total()).isEqualByComparingTo(new BigDecimal("121.00"));
    assertThat(document.lines()).hasSize(2);
    assertThat(document.lines().get(1)).isEqualTo(new InvoiceDocument.Line("Materials", 2, new BigDecimal("10.00"), new BigDecimal("20.00")));
  }

  @Test
  void failsWhenTheInvoiceIsNotFound() {
    DataList noHeader = new DataList();
    DataList lines = lines();
    assertThatThrownBy(() -> InvoiceService.toDocument(noHeader, lines, Locale.ENGLISH, key -> key))
      .isInstanceOf(AWException.class);
  }

  private DataList header() {
    DataList list = new DataList();
    DataListUtil.addColumn(list, "InvNum", List.of("HP-000012"));
    DataListUtil.addColumn(list, "InvIso", List.of("2026-11-03"));
    DataListUtil.addColumn(list, "Sta", List.of("PAID"));
    DataListUtil.addColumn(list, "StaTxt", List.of("Paid"));
    DataListUtil.addColumn(list, "OwnNam", List.of("Ana Torres"));
    DataListUtil.addColumn(list, "OwnAdr", List.of("Calle Mayor 5"));
    DataListUtil.addColumn(list, "OwnCit", List.of("Madrid"));
    DataListUtil.addColumn(list, "OwnEml", List.of("ana@mail.example"));
    DataListUtil.addColumn(list, "PetNam", List.of("Luna"));
    DataListUtil.addColumn(list, "Sub", List.of("100.00"));
    DataListUtil.addColumn(list, "Tax", List.of("21.00"));
    DataListUtil.addColumn(list, "Amt", List.of("121.00"));
    list.setRecords(1);
    return list;
  }

  private DataList lines() {
    DataList list = new DataList();
    DataListUtil.addColumn(list, "LinCon", List.of("Check-up", "Materials"));
    DataListUtil.addColumn(list, "LinQty", List.of("1", "2"));
    DataListUtil.addColumn(list, "LinPrc", List.of("80.00", "10.00"));
    DataListUtil.addColumn(list, "LinAmt", List.of("80.00", "20.00"));
    list.setRecords(2);
    return list;
  }
}
