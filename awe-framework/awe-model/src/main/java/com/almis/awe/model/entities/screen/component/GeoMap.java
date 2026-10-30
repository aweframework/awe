package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.util.data.ListUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Map tag class
 *
 * Used to parse the map tag with XStream. It is named GeoMap because Map collides with java.util.Map.
 * Each data row of the component is a marker on an OpenStreetMap map
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@XStreamAlias("map")
@Accessors(chain = true)
public class GeoMap extends Component {

  @Serial
  private static final long serialVersionUID = 4172950386115207469L;

  // Name of the column with the marker latitude
  @XStreamAlias("latitude")
  @XStreamAsAttribute
  private String latitude;

  // Name of the column with the marker longitude
  @XStreamAlias("longitude")
  @XStreamAsAttribute
  private String longitude;

  // Name of the column shown in the marker popup
  @XStreamAlias("marker-label")
  @XStreamAsAttribute
  private String markerLabel;

  // Initial zoom level
  @XStreamAlias("zoom")
  @XStreamAsAttribute
  private Integer zoom;

  // Initial center latitude
  @XStreamAlias("center-latitude")
  @XStreamAsAttribute
  private Double centerLatitude;

  // Initial center longitude
  @XStreamAlias("center-longitude")
  @XStreamAsAttribute
  private Double centerLongitude;

  @Override
  public GeoMap copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Retrieve component tag: the AngularJS client draws it with the aweGeoMap directive
   *
   * @return <code>geo-map</code> tag
   */
  @Override
  public String getComponentTag() {
    return "geo-map";
  }
}
