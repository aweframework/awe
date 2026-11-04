package com.almis.awe.service.screen;

import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.TagListBuilder;
import com.almis.awe.builder.screen.WindowBuilder;
import com.almis.awe.builder.screen.button.ButtonActionBuilder;
import com.almis.awe.builder.screen.button.ButtonBuilder;
import com.almis.awe.builder.screen.context.ContextButtonBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.builder.screen.dependency.DependencyActionBuilder;
import com.almis.awe.builder.screen.dependency.DependencyBuilder;
import com.almis.awe.builder.screen.dependency.DependencyElementBuilder;
import com.almis.awe.builder.screen.grid.GridBuilder;
import com.almis.awe.builder.screen.grid.TextColumnBuilder;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.entities.screen.component.grid.RowDetail;
import com.almis.awe.model.entities.screen.data.ScreenData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The structure of the screen data is a skeleton: each component travels once, in its controller, so its node in the
 * structure only says what it is and where it is (#909)
 */
class ScreenStructureSkeletonTest {

  private static final String ELEMENT_LIST = "elementList";
  private static final String ELEMENT_TYPE = "elementType";
  private static final String CENTER = "center";
  private static final String WINDOW = "Window";
  private final JsonMapper mapper = new JsonMapper();

  private JsonNode structureOf(Screen screen) {
    return mapper.valueToTree(new ScreenData().setStructure(screen)).get("structure");
  }

  private static List<JsonNode> nodes(JsonNode node) {
    List<JsonNode> list = new ArrayList<>();
    list.add(node);
    for (JsonNode child : node.path(ELEMENT_LIST)) {
      list.addAll(nodes(child));
    }
    return list;
  }

  private static JsonNode find(JsonNode structure, String id) {
    return nodes(structure).stream().filter(node -> id.equals(node.path("id").asString(null))).findFirst().orElseThrow();
  }

  private static Set<String> fieldNames(JsonNode node) {
    return new HashSet<>(node.propertyNames());
  }

  private static Screen screenOf(TagBuilder tag) throws Exception {
    return new ScreenBuilder().setId("sk").addTag(tag).build();
  }

  private static TagBuilder centerOf(WindowBuilder window) {
    return new TagBuilder().setSource(CENTER).setType("div").setStyle("p-3").addWindow(window);
  }

  @Test
  void aComponentNodeOnlyKeepsItsIdentityAndItsChildren() throws Exception {
    WindowBuilder window = new WindowBuilder().setId("Win").setLabel(WINDOW).setStyle("big").setHelp("HELP")
      .addCriteria(new TextCriteriaBuilder().setId("Crt").setLabel("Criterion").setStyle("small"));

    JsonNode structure = structureOf(screenOf(centerOf(window)));

    JsonNode win = find(structure, "Win");
    assertThat(fieldNames(win)).containsExactlyInAnyOrder(ELEMENT_TYPE, "id", ELEMENT_LIST);
    assertThat(win.get(ELEMENT_TYPE).asString()).isEqualTo(WINDOW);
    JsonNode criterion = find(structure, "Crt");
    assertThat(fieldNames(criterion)).containsExactlyInAnyOrder(ELEMENT_TYPE, "id", ELEMENT_LIST);
    assertThat(criterion.get(ELEMENT_TYPE).asString()).isEqualTo("Criteria");
    assertThat(win.get(ELEMENT_LIST)).hasSize(1);
    assertThat(structure.toString()).doesNotContain("Criterion").doesNotContain("\"label\"").doesNotContain("\"help\"");
  }

  @Test
  void aTreegridKeepsItsFlagAndAPlainGridDoesNot() throws Exception {
    WindowBuilder window = new WindowBuilder().setId("Win")
      .addGrid(new GridBuilder().setId("Tree").setTreeGrid(true), new GridBuilder().setId("Flat"));

    JsonNode structure = structureOf(screenOf(centerOf(window)));

    assertThat(fieldNames(find(structure, "Tree"))).containsExactlyInAnyOrder(ELEMENT_TYPE, "id", "treegrid", ELEMENT_LIST);
    assertThat(find(structure, "Tree").get("treegrid").asBoolean()).isTrue();
    assertThat(find(structure, "Tree").get(ELEMENT_TYPE).asString()).isEqualTo("Grid");
    assertThat(fieldNames(find(structure, "Flat"))).containsExactlyInAnyOrder(ELEMENT_TYPE, "id", ELEMENT_LIST);
  }

  @Test
  void aPlainElementKeepsItsAttributes() throws Exception {
    Screen screen = screenOf(centerOf(new WindowBuilder().setId("Win")));

    JsonNode structure = structureOf(screen);

    JsonNode tag = structure.get(ELEMENT_LIST).get(0);
    assertThat(tag.get(ELEMENT_TYPE).asString()).isEqualTo("Tag");
    assertThat(tag.get("source").asString()).isEqualTo(CENTER);
    assertThat(tag.get("type").asString()).isEqualTo("div");
    assertThat(tag.get("style").asString()).isEqualTo("p-3");
    assertThat(structure.get("id").asString()).isEqualTo("sk");
    assertThat(structure.get(ELEMENT_TYPE).asString()).isEqualTo("Screen");
    // The attributes of a plain element are the ones it always had
    ObjectNode plain = ((ObjectNode) mapper.valueToTree(screen)).deepCopy();
    plain.remove(ELEMENT_LIST);
    ObjectNode sent = ((ObjectNode) structure).deepCopy();
    sent.remove(ELEMENT_LIST);
    assertThat(sent).isEqualTo(plain);
  }

  @Test
  void aTagListKeepsItsWholeSubtree() throws Exception {
    TagListBuilder tagList = new TagListBuilder().setId("List").setLabel("List").addTag(new TagBuilder().setType("div")
      .addCriteria(new TextCriteriaBuilder().setId("Row").setLabel("Row criterion").setStyle("small"))
      .addButton(new ButtonBuilder().setId("RowBtn").setLabel("Row button").addButtonAction(new ButtonActionBuilder())));
    Screen screen = screenOf(new TagBuilder().setSource(CENTER).setType("div").addTagList(tagList));

    JsonNode node = find(structureOf(screen), "List");

    JsonNode expected = mapper.valueToTree(screen.getElementsByType(com.almis.awe.model.entities.screen.component.TagList.class).get(0));
    assertThat(node).isEqualTo(expected);
    assertThat(node.toString()).contains("Row criterion").contains("ButtonAction");
  }

  @Test
  void aRowDetailKeepsItsWholeSubtree() throws Exception {
    Grid grid = new GridBuilder().setId("Master").addColumn(new TextColumnBuilder().setId("Mc").setName("Mc")).build();
    Grid detailGrid = new GridBuilder().setId("Detail").addColumn(new TextColumnBuilder().setId("Dc").setName("Dc").setLabel("Detail column")).build();
    RowDetail rowDetail = new RowDetail();
    rowDetail.addElement(detailGrid);
    grid.addElement(rowDetail);
    Tag tag = new TagBuilder().setSource(CENTER).setType("div").build();
    tag.addElement(grid);
    Screen screen = new ScreenBuilder().setId("sk").build();
    screen.addElement(tag);

    JsonNode node = find(structureOf(screen), "Master");

    JsonNode detail = nodes(node).stream().filter(n -> "RowDetail".equals(n.path(ELEMENT_TYPE).asString(null))).findFirst().orElseThrow();
    assertThat(detail).isEqualTo(mapper.valueToTree(rowDetail));
    assertThat(detail.toString()).contains("Detail column").contains("Column");
    // The master grid only keeps its own columns out
    assertThat(nodes(node).stream().filter(n -> "Mc".equals(n.path("id").asString(null)))).isEmpty();
  }

  @Test
  void columnsDependenciesAndActionsAreNotSent() throws Exception {
    WindowBuilder window = new WindowBuilder().setId("Win")
      .addGrid(new GridBuilder().setId("Grd").addColumn(new TextColumnBuilder().setId("Col").setName("Col"))
        .addButton(new ButtonBuilder().setId("Btn").addButtonAction(new ButtonActionBuilder())
          .addDependency(new DependencyBuilder().addDependencyElement(new DependencyElementBuilder().setId("Win"))
            .addDependencyAction(new DependencyActionBuilder()))));

    JsonNode structure = structureOf(screenOf(centerOf(window)));

    Set<String> types = new HashSet<>();
    nodes(structure).forEach(node -> types.add(node.get(ELEMENT_TYPE).asString()));
    assertThat(types).contains("Grid", "Button").doesNotContain("Column", "Dependency", "DependencyElement", "DependencyAction", "ButtonAction");
    assertThat(find(structure, "Grd").get(ELEMENT_LIST)).hasSize(1);
    assertThat(find(structure, "Btn").get(ELEMENT_LIST)).isEmpty();
  }

  @Test
  void contextButtonsStayAsSkeletonNodes() throws Exception {
    WindowBuilder window = new WindowBuilder().setId("Win").addGrid(new GridBuilder().setId("Grd")
      .addContextButton(new ContextButtonBuilder().setId("Ctx").setLabel("Context label")));

    JsonNode structure = structureOf(screenOf(centerOf(window)));

    JsonNode context = find(structure, "Ctx");
    assertThat(fieldNames(context)).containsExactlyInAnyOrder(ELEMENT_TYPE, "id", ELEMENT_LIST);
    assertThat(context.get(ELEMENT_TYPE).asString()).isEqualTo("ContextButton");
  }

  @Test
  void theJavaScreenKeepsEveryNodeAndAttribute() throws Exception {
    Screen screen = screenOf(centerOf(new WindowBuilder().setId("Win").setLabel(WINDOW)
      .addGrid(new GridBuilder().setId("Grd").addColumn(new TextColumnBuilder().setId("Col").setName("Col")))));

    structureOf(screen);

    assertThat(screen.getElementsByType(Column.class)).hasSize(1);
    assertThat(screen.getElementsByType(Element.class)).isNotEmpty();
    // Only the structure of the screen data is a skeleton: the screen itself still serializes as always
    assertThat(mapper.writeValueAsString(screen)).contains("\"label\":\"Window\"").contains("\"Column\"");
  }

  @Test
  void aScreenDataWithoutStructureHasNoStructure() {
    ScreenData data = new ScreenData().setTemplate("<div/>");

    JsonNode json = mapper.valueToTree(data);

    assertThat(json.has("structure")).isFalse();
    assertThat(json.get("template").asString()).isEqualTo("<div/>");
  }
}
