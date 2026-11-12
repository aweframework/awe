package com.almis.awe.samples.vetclinic.invoice;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Produces the PDF of an invoice to download. The invoice is never read from the database here: it comes from the
 * AWE queries of the profile (<code>VetInvHdr</code> for the staff, <code>VetMyInvHdr</code> for the pet owners), so the
 * permission and the "only the invoices of the connected owner" filter are the ones declared in <code>Queries.xml</code>
 * and a number of another owner finds no invoice
 */
@Service
public class InvoiceService extends ServiceConfig {

  private final QueryService queryService;
  private final InvoicePdfRenderer renderer = new InvoicePdfRenderer();

  /**
   * Autowired constructor
   *
   * @param queryService Query service
   */
  public InvoiceService(QueryService queryService) {
    this.queryService = queryService;
  }

  /**
   * PDF of any invoice (staff)
   *
   * @param invoiceId Invoice identifier
   * @return File to download
   * @throws AWException The invoice does not exist or the user cannot read it
   */
  public ServiceData staffInvoicePdf(Integer invoiceId) throws AWException {
    return invoicePdf(invoiceId, "VetInvHdr", "VetInvLinLst");
  }

  /**
   * PDF of an invoice of the connected pet owner
   *
   * @param invoiceId Invoice identifier
   * @return File to download
   * @throws AWException The invoice does not exist or is not one of the connected owner
   */
  public ServiceData ownerInvoicePdf(Integer invoiceId) throws AWException {
    return invoicePdf(invoiceId, "VetMyInvHdr", "VetMyInvLinLst");
  }

  private ServiceData invoicePdf(Integer invoiceId, String headerQuery, String linesQuery) throws AWException {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("IdeInv", invoiceId);
    DataList header = queryService.launchQuery(headerQuery, parameters).getDataList();
    DataList lines = queryService.launchQuery(linesQuery, parameters).getDataList();
    InvoiceDocument document = toDocument(header, lines, languageLocale(), this::getLocale);

    byte[] pdf = renderer.render(document);
    FileData file = new FileData("invoice-" + document.number() + ".pdf", (long) pdf.length, MediaType.APPLICATION_PDF_VALUE);
    file.setFileStream(new ByteArrayInputStream(pdf));
    return new ServiceData().setData(file);
  }

  private Locale languageLocale() {
    return Locale.forLanguageTag(getElements().getLanguage().replace('_', '-'));
  }

  /**
   * Build the document from the rows of the invoice queries
   *
   * @param header Row of the invoice
   * @param lines  Rows of the lines
   * @param locale Language of the numbers and dates
   * @param text   Translation of a locale key
   * @return Document to print
   * @throws AWException There is no invoice
   */
  static InvoiceDocument toDocument(DataList header, DataList lines, Locale locale, UnaryOperator<String> text) throws AWException {
    if (header == null || header.getRows() == null || header.getRows().isEmpty()) {
      throw new AWException(text.apply("VET_PDF_NOT_FOUND_TITLE"), text.apply("VET_PDF_NOT_FOUND_MESSAGE"));
    }
    String address = join(DataListUtil.getData(header, 0, "OwnAdr"), DataListUtil.getData(header, 0, "OwnCit"));
    List<InvoiceDocument.Line> documentLines = new ArrayList<>();
    int count = lines == null ? 0 : lines.getRows().size();
    for (int row = 0; row < count; row++) {
      documentLines.add(new InvoiceDocument.Line(DataListUtil.getData(lines, row, "LinCon"),
        Integer.parseInt(DataListUtil.getData(lines, row, "LinQty")), amount(lines, row, "LinPrc"), amount(lines, row, "LinAmt")));
    }
    return new InvoiceDocument(DataListUtil.getData(header, 0, "InvNum"), LocalDate.parse(DataListUtil.getData(header, 0, "InvIso")),
      DataListUtil.getData(header, 0, "Sta"), DataListUtil.getData(header, 0, "StaTxt"), DataListUtil.getData(header, 0, "OwnNam"),
      address, DataListUtil.getData(header, 0, "OwnEml"), DataListUtil.getData(header, 0, "PetNam"), documentLines,
      amount(header, 0, "Sub"), amount(header, 0, "Tax"), amount(header, 0, "Amt"), locale, text);
  }

  private static BigDecimal amount(DataList list, int row, String column) {
    return new BigDecimal(DataListUtil.getData(list, row, column));
  }

  private static String join(String first, String second) {
    if (first == null || first.isBlank()) {
      return second == null ? "" : second;
    }
    return second == null || second.isBlank() ? first : first + ", " + second;
  }
}
