package org.opennms.api;

import java.util.Map;
import java.util.Optional;

import org.opennms.netmgt.model.OnmsSnmpInterface;

public interface DescriptionProvider {
    void popuplateDescriptionMap(final int nodeId, final Map<OnmsSnmpInterface, Optional<DescriptionEntity>> descriptionMap, final Map<String, OnmsSnmpInterface> ifIndexMap, final Map<String, OnmsSnmpInterface> interfaceMap, final String format);

    String getName();
}
