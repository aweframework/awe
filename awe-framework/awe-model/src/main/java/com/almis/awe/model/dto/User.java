package com.almis.awe.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * User bean class Mapping ope table
 *
 * @author pvidal
 */
@Data
@Accessors(chain = true)
public class User implements Serializable {

  // User ID (IdeOpe)
  private Integer userId;
  // User name (l1_nom)
  private String username;
  // User password (l1_pas)
  private String password;
  // Check if the user has ever connected (l1_con)
  private boolean connected;
  // User enabled (l1_act)
  private boolean enabled;
  // User profile (column Pro of AwePro table)
  private String profile;
  // Date of last login (l1_dat)
  private Date lastLogin;
  // Printer name (imp_nom)
  private String printer;
  // Update date (dat_mod)
  private Date updateDate;
  // Last changed password date (l1_psd)
  private Date lastChangedPasswordDate;
  // User language (l1_lan)
  private String language;
  // Email server (EmlSrv)
  private String emailServer;
  // Email (EmlAdr)
  private String email;
  // User full name (OpeNam)
  private String fullName;
  // Profile ID (IdePro)
  private Integer profileId;
  // User theme
  private String userTheme;
  // Profile theme
  private String profileTheme;
  // User initial screen
  private String userInitialScreen;
  // Screen init (ScrIni)
  private String profileInitialScreen;
  // User file restriction (res)
  private String userRestriction;
  // Profile file restriction (res)
  private String profileRestriction;
  // Password lock
  private boolean locked;
  // Login attempts count (numLog)
  private Integer loginAttempts;
  // Check if 2fa is enabled (enable2fa)
  private boolean enable2fa;
  // 2fa secret
  private String secret2fa;
}