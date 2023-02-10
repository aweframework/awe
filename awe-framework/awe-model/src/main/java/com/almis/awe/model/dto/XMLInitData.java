package com.almis.awe.model.dto;

import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.screen.Screen;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Data
@Accessors(chain = true)
public class XMLInitData {
  List<Future<String>> general = new ArrayList<>();
  List<Future<Map<String, Profile>>> profileResults = new ArrayList<>();
  List<Future<Map<String, Screen>>> screenResults = new ArrayList<>();
  List<Map<String, Future<Map<String, String>>>> localeResults = new ArrayList<>();
}
