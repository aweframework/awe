package com.almis.awe.developer.translators;

import com.almis.awe.developer.model.ITranslationResult;

import java.net.URISyntaxException;

public interface ITranslator {
  ITranslationResult translate(String text, String languageFrom, String languageTo) throws URISyntaxException;
}
