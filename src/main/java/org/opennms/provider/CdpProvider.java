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
import org.opennms.web.rest.model.v2.CdpLinkNodeDTO;

public class CdpProvider extends AbstractDescriptionProvider {
    protected final static Pattern CDP_LOCAL_PORT_EXTRACT_IFINDEX = Pattern.compile("^.*\\(ifindex:(.*?)\\)$");
    protected final static Pattern CDP_CACHE_DEVICE_EXTRACT_HOSTNAME = Pattern.compile("^(.*?)\\(.*\\)$");
    protected final static Pattern CDP_CACHE_DEVICE_PORT_EXTRACT_PORT = Pattern.compile("^(.*?)\\(\\).*$");
    protected final Client client;
    protected final String url;

    public CdpProvider(final Client client, final String url) {
        super("CDP");
        this.client = client;
        this.url = url;
    }

    @Override
    public void popuplateDescriptionMap(int nodeId, Map<OnmsSnmpInterface, Optional<DescriptionEntity>> descriptionMap, Map<String, OnmsSnmpInterface> ifIndexMap, Map<String, OnmsSnmpInterface> interfaceMap, final String format) {
        final List<CdpLinkNodeDTO> cdpLinkNodes = client.target(url)
                .path("/api/v2/enlinkd/cdp_links/" + nodeId)
                .queryParam("limit", "0")
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<CdpLinkNodeDTO>>() {
                });

        for (final CdpLinkNodeDTO cdpLinkNodeDTO : cdpLinkNodes) {
            final Matcher ifIndexMatcher = CDP_LOCAL_PORT_EXTRACT_IFINDEX.matcher(cdpLinkNodeDTO.getCdpLocalPort());
            if (ifIndexMatcher.find()) {
                final String ifIndex = ifIndexMatcher.group(1);
                final OnmsSnmpInterface onmsSnmpInterface = ifIndexMap.get(ifIndex);
                if (onmsSnmpInterface != null && descriptionMap.get(onmsSnmpInterface).isEmpty()) {
                    final Matcher remoteHostnameMatcher = CDP_CACHE_DEVICE_EXTRACT_HOSTNAME.matcher(cdpLinkNodeDTO.getCdpCacheDevice());
                    final String remoteHostname;

                    if (remoteHostnameMatcher.find()) {
                        remoteHostname = remoteHostnameMatcher.group(1);
                    } else {
                        continue;
                    }

                    final Matcher remotePortMatcher = CDP_CACHE_DEVICE_PORT_EXTRACT_PORT.matcher(cdpLinkNodeDTO.getCdpCacheDevicePort());
                    final String remotePort;
                    if (remotePortMatcher.find()) {
                        remotePort = remotePortMatcher.group(1);
                    } else {
                        continue;
                    }

                    descriptionMap.put(onmsSnmpInterface, Optional.of(new DescriptionEntity(format(format, remoteHostname, remotePort), onmsSnmpInterface.getIfType(), this)));
                }
            }
        }
    }
}
