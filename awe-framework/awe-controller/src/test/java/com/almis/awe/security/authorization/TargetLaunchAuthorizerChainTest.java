package com.almis.awe.security.authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the chain of authorizers that a client launch of a target goes through
 */
class TargetLaunchAuthorizerChainTest {

  private static final String TARGET = "UsrDel";

  private final Map<String, Object> beans = new LinkedHashMap<>();
  private final List<String> calls = new ArrayList<>();

  private TargetLaunchAuthorizerChain chain() {
    ObjectProvider<TargetLaunchAuthorizer> links = new StaticListableBeanFactory(beans)
      .getBeanProvider(TargetLaunchAuthorizer.class);
    TargetLaunchAuthorizerChain chain = new TargetLaunchAuthorizerChain(links);
    beans.put("chain", chain);
    return chain;
  }

  private TargetLaunchAuthorizer link(String name, boolean allows) {
    TargetLaunchAuthorizer link = (targetId, kind) -> {
      calls.add(name + ":" + kind + ":" + targetId);
      return allows;
    };
    beans.put(name, link);
    return link;
  }

  @Test
  @DisplayName("A launch goes on when every authorizer allows it")
  void everyLinkAllows() {
    link("screens", true);
    link("permissions", true);

    assertThat(chain().authorize(TARGET, TargetKind.MAINTAIN)).isTrue();
    assertThat(calls).containsExactly("screens:MAINTAIN:UsrDel", "permissions:MAINTAIN:UsrDel");
  }

  @Test
  @DisplayName("A launch is refused when one authorizer refuses it, and the later ones are not asked")
  void oneLinkRefuses() {
    link("screens", false);
    link("permissions", true);

    assertThat(chain().authorize(TARGET, TargetKind.QUERY)).isFalse();
    assertThat(calls).containsExactly("screens:QUERY:UsrDel");
  }

  @Test
  @DisplayName("Without authorizers a launch goes on, and the chain does not ask itself")
  void withoutLinks() {
    assertThat(chain().authorize(TARGET, TargetKind.MAINTAIN)).isTrue();
  }

  @Test
  @DisplayName("The authorizers are looked up on the first launch, not when the chain is built")
  void linksAreLookedUpLazily() {
    TargetLaunchAuthorizerChain chain = chain();
    link("screens", false);

    assertThat(chain.authorize(TARGET, TargetKind.MAINTAIN)).isFalse();
  }
}
