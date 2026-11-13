package com.almis.awe.samples.fleetlogistics;

import com.almis.awe.testing.descriptor.AbstractApplicationDescriptorTest;

import java.nio.file.Path;
import java.util.Set;

/**
 * Descriptors of the fleet application: they parse, every locale key exists in English, Spanish and French, every
 * target of a screen exists, and only the intended targets are public
 */
class FleetLogisticsDescriptorTest extends AbstractApplicationDescriptorTest {

  @Override
  protected Path moduleDirectory() {
    return Path.of("src/main/resources/application/fleet");
  }

  /**
   * Public targets:
   * <ul>
   *   <li>FltRegPub: the regions of the quote form.</li>
   *   <li>FltTrackPub: the tracking timeline of one shipment; it needs the tracking code and the postal code.</li>
   *   <li>FltTrackRoute: the numbered points of the tracking map; its service only reads the points through the private query
   *   FltTrackPts, which needs the tracking code and the postal code like FltTrackPub.</li>
   *   <li>FltQuoNew: the quote request, which only inserts a pending request.</li>
   *   <li>FltFlagLate and FltCloseRoute: the targets of the scheduled tasks. The scheduler runs without a session, so
   *   they are public; they take no input and are idempotent, so launching them again is harmless.</li>
   * </ul>
   */
  @Override
  protected Set<String> publicTargets() {
    return Set.of("FltRegPub", "FltTrackPub", "FltTrackRoute", "FltQuoNew", "FltFlagLate", "FltCloseRoute");
  }
}
