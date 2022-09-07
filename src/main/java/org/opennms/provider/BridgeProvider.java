package org.opennms.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.opennms.api.AbstractDescriptionProvider;
import org.opennms.api.DescriptionEntity;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.opennms.web.rest.model.v2.BridgeLinkNodeDTO;
import org.opennms.web.rest.model.v2.BridgeLinkRemoteNodeDTO;

public class BridgeProvider extends AbstractDescriptionProvider {
    protected final static Pattern BRIDGE_LOCAL_PORT_EXTRACT_IFINDEX = Pattern.compile("^.*\\(ifindex:(.*?)\\)\\(.*\\)$");
    protected final static Pattern BRIDGE_REMOTE_EXTRACT_HOSTNAME = Pattern.compile("^(.*?)\\(mac:.*\\)$");
    protected final static Pattern BRIDGE_REMOTE_PORT_EXTRACT_PORT = Pattern.compile("^(.*?)\\(.*\\)\\(.*\\)$");
    protected final Client client;
    protected final String url;

    public BridgeProvider(final Client client, final String url) {
        super("Bridge");
        this.client = client;
        this.url = url;
    }
    @Override
    public void popuplateDescriptionMap(int nodeId, Map<OnmsSnmpInterface, Optional<DescriptionEntity>> descriptionMap, Map<String, OnmsSnmpInterface> ifIndexMap, Map<String, OnmsSnmpInterface> interfaceMap, final String format) {
        final List<BridgeLinkNodeDTO> bridgeLinkNodes = client.target(url)
                .path("/api/v2/enlinkd/bridge_links/" + nodeId)
                .queryParam("limit", "0")
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<BridgeLinkNodeDTO>>() {
                });

        for (final BridgeLinkNodeDTO bridgeLinkNodeDTO : bridgeLinkNodes) {
            final Matcher ifIndexMatcher = BRIDGE_LOCAL_PORT_EXTRACT_IFINDEX.matcher(bridgeLinkNodeDTO.getBridgeLocalPort());
            if (ifIndexMatcher.find()) {
                final String ifIndex = ifIndexMatcher.group(1);
                final OnmsSnmpInterface onmsSnmpInterface = ifIndexMap.get(ifIndex);
                if (onmsSnmpInterface != null && descriptionMap.get(onmsSnmpInterface).isEmpty()) {
                    final List<BridgeLinkRemoteNodeDTO> bridgeLinkRemoteNodes = bridgeLinkNodeDTO.getBridgeLinkRemoteNodes();

                    if (bridgeLinkRemoteNodes.size() == 1) {
                        if (bridgeLinkRemoteNodes.get(0).getBridgeRemote() == null || bridgeLinkRemoteNodes.get(0).getBridgeRemotePort() == null) {
                            continue;
                        }

                        final Matcher remoteHostnameMatcher = BRIDGE_REMOTE_EXTRACT_HOSTNAME.matcher(bridgeLinkRemoteNodes.get(0).getBridgeRemote());
                        final String remoteHostname;

                        if (remoteHostnameMatcher.find()) {
                            remoteHostname = remoteHostnameMatcher.group(1);
                        } else {
                            continue;
                        }

                        final Matcher remotePortMatcher = BRIDGE_REMOTE_PORT_EXTRACT_PORT.matcher(bridgeLinkRemoteNodes.get(0).getBridgeRemotePort());
                        final String remotePort;
                        if (remotePortMatcher.find()) {
                            remotePort = remotePortMatcher.group(1);
                        } else {
                            remotePort = null;
                        }
                        descriptionMap.put(onmsSnmpInterface, Optional.of(new DescriptionEntity(format(format, remoteHostname, remotePort), onmsSnmpInterface.getIfType(), this)));
                    }
                }
            }
        }
    }
}
