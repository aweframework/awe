package com.almis.awe.model;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ResponseWrapper;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.data.builder.DataListBuilder;

import java.util.Arrays;

public class Postman implements ResponseWrapper {

  private Boolean gzipped;
  private PostmanHeaders headers;
  private String method;

  @Override
  public ServiceData toServiceData() throws AWException {
    DataList dataList = new DataListBuilder()
            .addColumn("gzipped", Arrays.asList(getGzipped()), "BOOLEAN")
            .addColumn("accept", Arrays.asList(getHeaders().getAccept()), "STRING")
            .addColumn("acceptEncoding", Arrays.asList(getHeaders().getAcceptEncoding()), "STRING")
            .addColumn("acceptLanguage", Arrays.asList(getHeaders().getAcceptLanguage()), "STRING")
            .addColumn("cacheControl", Arrays.asList(getHeaders().getCacheControl()), "STRING")
            .addColumn("cookie", Arrays.asList(getHeaders().getCookie()), "STRING")
            .addColumn("postmanToken", Arrays.asList(getHeaders().getPostmanToken()), "STRING")
            .addColumn("userAgent", Arrays.asList(getHeaders().getUserAgent()), "STRING")
            .addColumn("method", Arrays.asList(getMethod()), "STRING")
            .build();
    return new ServiceData().setDataList(dataList);
  }

  public Boolean getGzipped() {
    return gzipped;
  }

  public Postman setGzipped(Boolean gzipped) {
    this.gzipped = gzipped;
    return this;
  }

  public PostmanHeaders getHeaders() {
    return headers;
  }

  public Postman setHeaders(PostmanHeaders headers) {
    this.headers = headers;
    return this;
  }

  public String getMethod() {
    return method;
  }

  public Postman setMethod(String method) {
    this.method = method;
    return this;
  }
}
