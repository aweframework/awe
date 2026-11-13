package com.almis.awe.service.access;

import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Computes the effective access of a subject (a profile or a user) to every option of a menu, with the layer that
 * decides it. It follows the order of the real menu of a session ({@code MenuProvider.getMenuWithAllRestrictions} with
 * {@code ScreenRestrictionGenerator}): the module restriction first, then the global rules (the base profile file, then
 * the database rules without user nor profile), the profile rules and the user rules. The last rule wins and a rule
 * applies to the option and all its descendants.
 */
public final class AccessCascade {

  private AccessCascade() {
  }

  /**
   * Layer of a rule, in the order of the cascade
   */
  public enum Layer {
    NONE, MODULE, BASE_PROFILE, GLOBAL, PROFILE, USER
  }

  /**
   * Warning of an option
   */
  public enum Warning {
    // Allowed while an ancestor is denied: it is not reachable from the menu
    PARENT_DENIED,
    // The own rule of the subject does not change the access, without module nor in any module of the menu
    REDUNDANT
  }

  /**
   * A rule of a layer on an option
   *
   * @param layer      Layer
   * @param option     Option name
   * @param restricted Restricted (deny) or not (allow)
   */
  public record Rule(Layer layer, String option, boolean restricted) {
  }

  /**
   * Effective access of an option
   *
   * @param option       Option
   * @param parent       Parent option (null on the first level)
   * @param allowed      The option is allowed
   * @param layer        Layer that decides (NONE when no rule applies: allowed by default)
   * @param ruleOption   Option that carries the deciding rule (itself, or an ancestor when it is inherited)
   * @param moduleOption Option whose module hides this one (itself or an ancestor), null when the module does not hide it
   * @param ownRule      Own rule of the subject on the option: "A", "R" or ""
   * @param warning      Warning, null when there is none
   */
  public record OptionAccess(Option option, Option parent, boolean allowed, Layer layer, Option ruleOption,
                             Option moduleOption, String ownRule, Warning warning) {

    /**
     * The deciding rule is on an ancestor
     *
     * @return Inherited
     */
    public boolean inherited() {
      return ruleOption != null && ruleOption != option;
    }
  }

  /**
   * Result of the cascade
   *
   * @param options Effective access of every option, parents before their children
   * @param rules   Rules of the cascade
   */
  public record Result(List<OptionAccess> options, List<Rule> rules) {

    /**
     * Find the access of an option by name
     *
     * @param name Option name
     * @return Option access
     */
    public Optional<OptionAccess> find(String name) {
      return options.stream().filter(access -> access.option().getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Rule of a layer on an option (the last one of the layer)
     *
     * @param layer  Layer
     * @param option Option name
     * @return "A", "R" or "" when the layer has no rule on the option
     */
    public String ruleOf(Layer layer, String option) {
      return AccessCascade.ruleOf(rules, layer, option);
    }
  }

  /**
   * Evaluate the cascade
   *
   * @param menu         Menu (not modified)
   * @param module       Selected module (null for none)
   * @param rules        Rules (any order of layers; the order inside a layer is kept)
   * @param subjectLayer Layer of the subject: PROFILE or USER
   * @return Result
   */
  public static Result evaluate(Menu menu, String module, List<Rule> rules, Layer subjectLayer) {
    List<Rule> ordered = order(rules);
    List<Node> nodes = new ArrayList<>();
    collect(menu.getElementList(), null, nodes);
    Map<Option, State> states = simulate(nodes, module, ordered);
    List<String> contexts = getContexts(nodes, module);

    Map<String, Boolean> redundantByName = new HashMap<>();
    List<OptionAccess> options = new ArrayList<>();
    for (Node node : nodes) {
      State state = states.get(node.option());
      String ownRule = ruleOf(ordered, subjectLayer, node.option().getName());
      Warning warning = null;
      if (!state.restricted && hasDeniedAncestor(node, states)) {
        warning = Warning.PARENT_DENIED;
      } else if (!ownRule.isEmpty() && redundantByName.computeIfAbsent(node.option().getName().toLowerCase(),
        name -> isRedundant(nodes, contexts, ordered, subjectLayer, name))) {
        warning = Warning.REDUNDANT;
      }
      options.add(new OptionAccess(node.option(), node.parent() == null ? null : node.parent().option(),
        !state.restricted, state.layer, state.ruleOption, state.moduleOption, ownRule, warning));
    }
    return new Result(List.copyOf(options), ordered);
  }

  /**
   * Rules after a change of the own rules of a layer on some options, as the restriction maintains store it: a rule of
   * the layer on the option changes in its place (update-restriction keeps its row), a new one goes last in its layer
   * (new-restriction) and a removal drops it (delete-restriction). Nothing is stored
   *
   * @param rules      Rules
   * @param layer      Layer of the own rules: PROFILE or USER
   * @param options    Option names
   * @param restricted Deny (true), allow (false) or remove the rule (null)
   * @return Rules after the change
   */
  public static List<Rule> change(List<Rule> rules, Layer layer, Collection<String> options, Boolean restricted) {
    Map<String, String> pending = new LinkedHashMap<>();
    options.forEach(option -> pending.putIfAbsent(option.toLowerCase(), option));
    Set<String> changing = Set.copyOf(pending.keySet());
    List<Rule> changed = new ArrayList<>();
    for (Rule rule : rules) {
      String name = rule.option() == null ? null : rule.option().toLowerCase();
      if (rule.layer() != layer || !changing.contains(name)) {
        changed.add(rule);
      } else {
        pending.remove(name);
        if (restricted != null) {
          changed.add(new Rule(layer, rule.option(), restricted));
        }
      }
    }
    if (restricted != null) {
      pending.values().forEach(option -> changed.add(new Rule(layer, option, restricted)));
    }
    return changed;
  }

  /**
   * Rules in the order of the cascade: base profile file, database global rules, profile and user rules, keeping the
   * order inside each layer as ScreenRestrictionGenerator does (getScreenRestrictions lists the base profile rules first)
   */
  private static List<Rule> order(List<Rule> rules) {
    return rules.stream()
      .filter(rule -> rule.layer() != Layer.NONE && rule.layer() != Layer.MODULE)
      .sorted(Comparator.comparingInt(AccessCascade::group))
      .toList();
  }

  private static int group(Rule rule) {
    return switch (rule.layer()) {
      case GLOBAL -> 1;
      case PROFILE -> 2;
      case USER -> 3;
      default -> 0;
    };
  }

  private static String ruleOf(List<Rule> rules, Layer layer, String option) {
    String rule = "";
    for (Rule candidate : rules) {
      if (candidate.layer() == layer && candidate.option() != null && candidate.option().equalsIgnoreCase(option)) {
        rule = candidate.restricted() ? "R" : "A";
      }
    }
    return rule;
  }

  private static void collect(List<Option> options, Node parent, List<Node> nodes) {
    for (Option option : options) {
      Node node = new Node(option, parent);
      nodes.add(node);
      collect(option.getOptions(), node, nodes);
    }
  }

  private static Map<Option, State> simulate(List<Node> nodes, String module, List<Rule> rules) {
    Map<Option, State> states = new IdentityHashMap<>();
    nodes.forEach(node -> states.put(node.option(), new State()));

    // Module restriction: options of another module, with their descendants
    if (module != null) {
      nodes.stream()
        .map(Node::option)
        .filter(option -> option.getModule() != null && !module.equalsIgnoreCase(option.getModule()))
        .forEach(option -> apply(option, states, state -> {
          state.restricted = true;
          state.layer = Layer.MODULE;
          state.ruleOption = option;
          state.moduleOption = option;
        }));
    }

    // Rules: the last one wins and applies to the descendants
    for (Rule rule : rules) {
      nodes.stream()
        .map(Node::option)
        .filter(option -> rule.option() != null && rule.option().equalsIgnoreCase(option.getName()))
        .forEach(option -> apply(option, states, state -> {
          state.restricted = rule.restricted();
          state.layer = rule.layer();
          state.ruleOption = option;
        }));
    }
    return states;
  }

  private static void apply(Option option, Map<Option, State> states, Consumer<State> change) {
    change.accept(states.get(option));
    option.getOptions().forEach(child -> apply(child, states, change));
  }

  private static boolean hasDeniedAncestor(Node node, Map<Option, State> states) {
    for (Node ancestor = node.parent(); ancestor != null; ancestor = ancestor.parent()) {
      if (states.get(ancestor.option()).restricted) {
        return true;
      }
    }
    return false;
  }

  /**
   * Modules the real menu can be evaluated in: none, the selected one and the module of each option. A rule wins over the
   * module restriction, so a rule that changes nothing without module can show an option of another module
   */
  private static List<String> getContexts(List<Node> nodes, String module) {
    Map<String, String> modules = new LinkedHashMap<>();
    if (module != null) {
      modules.put(module.toLowerCase(), module);
    }
    nodes.stream()
      .map(node -> node.option().getModule())
      .filter(Objects::nonNull)
      .forEach(optionModule -> modules.putIfAbsent(optionModule.toLowerCase(), optionModule));
    List<String> contexts = new ArrayList<>();
    contexts.add(null);
    contexts.addAll(modules.values());
    return contexts;
  }

  /**
   * The own rules of the subject on an option are redundant when the access of every option is the same without them,
   * without module and in every module of the menu: removing them changes nothing in any menu of the subject
   */
  private static boolean isRedundant(List<Node> nodes, List<String> contexts, List<Rule> rules, Layer subjectLayer,
                                     String optionName) {
    List<Rule> withoutOwnRule = rules.stream()
      .filter(rule -> rule.layer() != subjectLayer || !optionName.equalsIgnoreCase(rule.option()))
      .toList();
    for (String context : contexts) {
      Map<Option, State> states = simulate(nodes, context, rules);
      Map<Option, State> alternative = simulate(nodes, context, withoutOwnRule);
      if (!nodes.stream()
        .allMatch(node -> states.get(node.option()).restricted == alternative.get(node.option()).restricted)) {
        return false;
      }
    }
    return true;
  }

  private record Node(Option option, Node parent) {
  }

  private static final class State {
    private boolean restricted;
    private Layer layer = Layer.NONE;
    private Option ruleOption;
    private Option moduleOption;
  }
}
