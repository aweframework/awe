package com.almis.awe.security.authorization;

import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Chain of the {@link TargetLaunchAuthorizer} beans of the application: a client launch goes on only when every one of
 * them allows it, in their order, and the first refusal ends the check. Without authorizers every launch goes on.
 *
 * <p>The authorizers are looked up on the first launch, not when the chain is built: they depend on services (such as
 * the menu) that depend on the query and maintain services that use the chain.</p>
 */
public class TargetLaunchAuthorizerChain implements TargetLaunchAuthorizer {

  private final ObjectProvider<TargetLaunchAuthorizer> authorizerProvider;
  private final AtomicReference<List<TargetLaunchAuthorizer>> authorizers = new AtomicReference<>();

  /**
   * Autowired constructor
   *
   * @param authorizerProvider Provider of the authorizers of the application
   */
  public TargetLaunchAuthorizerChain(ObjectProvider<TargetLaunchAuthorizer> authorizerProvider) {
    this.authorizerProvider = authorizerProvider;
  }

  @Override
  public boolean authorize(String targetId, TargetKind kind) {
    for (TargetLaunchAuthorizer authorizer : getAuthorizers()) {
      if (!authorizer.authorize(targetId, kind)) {
        return false;
      }
    }
    return true;
  }

  private List<TargetLaunchAuthorizer> getAuthorizers() {
    List<TargetLaunchAuthorizer> current = authorizers.get();
    if (current == null) {
      // Two first launches at the same time may both look the authorizers up: the lists are equal and immutable
      current = authorizerProvider.orderedStream()
        .filter(authorizer -> !(authorizer instanceof TargetLaunchAuthorizerChain))
        .toList();
      authorizers.compareAndSet(null, current);
    }
    return current;
  }
}
