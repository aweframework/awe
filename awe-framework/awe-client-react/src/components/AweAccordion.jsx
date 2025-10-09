import React, {useEffect} from "react";
import {Accordion, AccordionTab} from "primereact/accordion";
import {translateLabel} from "../utilities";
import {Components} from "../utilities/structure";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import {usePanelable} from "../hooks/usePanelable";
import PropTypes from "prop-types";

function AweAccordion(props) {
  const { id, elementList = [] } = props;
  const { model = {values: []}, address, attributes = {} } = useSelector(state => ({
    model: state.components[id]?.model,
    address: state.components[id]?.address,
    attributes: state.components[id]?.attributes,
  }));
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const updateModelWithDependencies = (addr, payload) => dispatch(updateThunk(addr, payload));
  const { autocollapse, style } = attributes;

  // Initialize values from AccordionItem list on mount and ensure selection state exists
  useEffect(() => {
    const items = elementList.filter(node => node.elementType === "AccordionItem");
    if (items.length > 0) {
      updateModelWithDependencies(address, {
        values: items.map(item => ({value: item.id, label: item.id, selected: false}))
      });
    }
  // run only once on mount
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const { activeIndex, selectIndex } = usePanelable(model, address, {
    multi: autocollapse === false,
    defaultFirst: autocollapse !== false
  });

  const onChange = (e) => {
    selectIndex(e.index);
  };

  const getHeader = (node) => {
    const {label} = node;
    if (label) {
      return <span className={"window-header"}>{translateLabel(label, t)}</span>
    }
    return null;
  };

  return <Accordion className={style}
                    activeIndex={activeIndex}
                    onTabChange={onChange}
                    multiple={autocollapse === false}>
    {elementList
      .filter(node => node.elementType === "AccordionItem")
      .map((node, index) => <AccordionTab key={node.id || `accordion-${index}`} header={getHeader(node)}
                                          children={node.elementList.map((subnode, subindex) => Components(subnode, subindex))}/>)}
  </Accordion>;
}

AweAccordion.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweAccordion;
