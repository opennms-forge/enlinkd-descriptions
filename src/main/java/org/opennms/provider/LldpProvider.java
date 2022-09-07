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
import org.opennms.web.rest.model.v2.LldpLinkNodeDTO;

import com.google.common.base.Strings;

public class LldpProvider extends AbstractDescriptionProvider {
    protected final static Pattern[] LLDP_LOCAL_PORT_EXTRACT_IFNAME = new Pattern[]{
            Pattern.compile("^.*\\(interfaceName:(.*?)\\)$"),
            Pattern.compile("^(.*?)\\(.*$")
    };
    protected final static Pattern LLDP_REM_CHASSIS_ID_EXTRACT_HOSTNAME = Pattern.compile("^(.*?)\\(.*\\)$");
    protected final static Pattern LLDP_REM_PORT_EXTRACT_PORT = Pattern.compile("^(.*?)\\(.*\\)$");
    protected final Client client;
    protected final String url;

    public LldpProvider(final Client client, final String url) {
        super("LLDP");
        this.client = client;
        this.url = url;
    }

    public static String getIfName(final String string) {
        for (final Pattern pattern : LLDP_LOCAL_PORT_EXTRACT_IFNAME) {
            final Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    @Override
    public void popuplateDescriptionMap(int nodeId, Map<OnmsSnmpInterface, Optional<DescriptionEntity>> descriptionMap, Map<String, OnmsSnmpInterface> ifIndexMap, Map<String, OnmsSnmpInterface> interfaceMap, final String format) {
        final List<LldpLinkNodeDTO> lldpLinkNodes = client.target(url)
                .path("/api/v2/enlinkd/lldp_links/" + nodeId)
                .queryParam("limit", "0")
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<LldpLinkNodeDTO>>() {
                });

        for (final LldpLinkNodeDTO lldpLinkNodeDTO : lldpLinkNodes) {
            final String ifName = getIfName(lldpLinkNodeDTO.getLldpLocalPort());

            if (ifName != null) {
                final OnmsSnmpInterface onmsSnmpInterface = interfaceMap.get(ifName);
                if (onmsSnmpInterface != null && descriptionMap.get(onmsSnmpInterface).isEmpty()) {

                    final Matcher remoteHostnameMatcher = LLDP_REM_CHASSIS_ID_EXTRACT_HOSTNAME.matcher(lldpLinkNodeDTO.getLldpRemChassisId());
                    final String remoteHostname;

                    if (remoteHostnameMatcher.find()) {
                        remoteHostname = remoteHostnameMatcher.group(1);
                    } else {
                        continue;
                    }

                    final Matcher remotePortMatcher = LLDP_REM_PORT_EXTRACT_PORT.matcher(lldpLinkNodeDTO.getLdpRemPort());
                    final String remotePort;
                    if (remotePortMatcher.find()) {
                        remotePort = remotePortMatcher.group(1);
                    } else {
                        continue;
                    }

                    if (!Strings.isNullOrEmpty(remoteHostname) && !Strings.isNullOrEmpty(remotePort)) {
                        descriptionMap.put(onmsSnmpInterface, Optional.of(new DescriptionEntity(format(format, remoteHostname, remotePort), onmsSnmpInterface.getIfType(), this)));
                    }
                }
            }
        }
    }
}
