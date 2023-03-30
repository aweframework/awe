package com.almis.awe.rest.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class JwtTokenInfo {
  private String token;
  private String username;
  private String issuer;
  private Date expiredAt;
}
