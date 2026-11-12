package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * PrinterService Class
 * Provides server printer information
 *
 * @author Aitor UGARTE - 04/JUN/2013
 */
public class PrinterService extends ServiceConfig {

  /**
   * Time the printer list of the server is kept before looking the printers up again
   */
  static final Duration PRINTER_LIST_TTL = Duration.ofMinutes(1);

  private final Supplier<List<String>> printerLookup;
  private final LongSupplier nanoClock;
  private final AtomicReference<PrinterList> printerList = new AtomicReference<>();

  /**
   * Constructor: looks up the printers of the server
   */
  public PrinterService() {
    this(PrinterService::lookupPrinters, System::nanoTime);
  }

  /**
   * Constructor
   *
   * @param printerLookup Printer names lookup
   * @param nanoClock     Clock in nanoseconds
   */
  PrinterService(Supplier<List<String>> printerLookup, LongSupplier nanoClock) {
    this.printerLookup = printerLookup;
    this.nanoClock = nanoClock;
  }

  /**
   * Provides printer List
   *
   * @return Service Output Messages
   * @throws AWException Error retrieving printers
   */
  public ServiceData getPrinterList() throws AWException {
    ServiceData serviceData = new ServiceData();
    try {
      List<String> printers = getPrinters();
      DataList dataList = new DataList();
      DataListUtil.addColumn(dataList, "printer", printers);
      dataList.setRecords(printers.size());
      serviceData.setDataList(dataList);
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_GET_PRN"), getLocale("ERROR_MESSAGE_GET_PRN"), exc);
    }
    return serviceData;
  }

  private List<String> getPrinters() {
    long now = nanoClock.getAsLong();
    PrinterList current = printerList.get();
    if (current == null || now - current.lookedUpAt() >= PRINTER_LIST_TTL.toNanos()) {
      current = new PrinterList(List.copyOf(printerLookup.get()), now);
      printerList.set(current);
    }
    return current.printers();
  }

  private static List<String> lookupPrinters() {
    return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null)).map(PrintService::getName).toList();
  }

  private record PrinterList(List<String> printers, long lookedUpAt) {
  }
}
