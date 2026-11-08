package com.almis.awe.exception;

import com.almis.awe.model.entities.actions.ClientAction;
import lombok.Getter;

import java.io.Serial;
import java.util.List;

/**
 * A client launch refused by the authorization. The client actions built for the refusal travel with it, so the caller
 * answers the same body it answered while the refusal was a plain action error, with the status that HTTP defines for
 * a refusal.
 * <p>
 * It is a runtime exception on purpose: it is not an application error to be handled by the code that launches an
 * action, but the signal of the answer of the request, handled by the controller.
 */
@Getter
public class AWEClientLaunchRefusedException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -5889203362778549821L;

  /**
   * Client actions of the refusal (end-load and the access denied message, as the default error builds them)
   */
  private final transient List<ClientAction> clientActions;

  /**
   * Constructs an instance of <code>AWEClientLaunchRefusedException</code>
   *
   * @param cause         Refusal that ended the client launch
   * @param clientActions Client actions to answer
   */
  public AWEClientLaunchRefusedException(AWEUnauthorizedException cause, List<ClientAction> clientActions) {
    super(cause.getMessage(), cause);
    this.clientActions = clientActions == null ? List.of() : List.copyOf(clientActions);
  }
}
