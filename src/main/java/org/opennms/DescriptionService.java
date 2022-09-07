package org.opennms;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.opennms.api.DescriptionEntity;
import org.opennms.api.DescriptionProvider;
import org.opennms.netmgt.model.OnmsIpInterfaceList;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.opennms.netmgt.model.OnmsSnmpInterfaceList;
import org.opennms.provider.BridgeProvider;
import org.opennms.provider.CdpProvider;
import org.opennms.provider.LldpProvider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.Lists;

public class DescriptionService {
    private Client client;
    private String url;
    private final Map<String, DescriptionProvider> descriptionProviderMap = new TreeMap<>();

    @Option(name = "-p", handler=StringArrayOptionHandler.class)
    private List<String> provider = Lists.newArrayList("CDP", "LLDP", "Bridge");

    public DescriptionService(final String url, final String username, final String password) {
        final HttpAuthenticationFeature httpAuthenticationFeature = HttpAuthenticationFeature.basic(username, password);
        final SslConfigurator sslConfigurator = SslConfigurator.newInstance();
        //final SSLContext sslContext = sslConfigurator.createSSLContext();

        final JacksonJsonProvider jacksonJsonProvider = new JacksonJaxbJsonProvider()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.client = ClientBuilder.newClient(
                new ClientConfig(jacksonJsonProvider)
        ).register(httpAuthenticationFeature);

        this.url = url;

        descriptionProviderMap.put("cdp", new CdpProvider(client, url));
        descriptionProviderMap.put("lldp", new LldpProvider(client, url));
        descriptionProviderMap.put("bridge", new BridgeProvider(client, url));
    }

    public OnmsNode getNode(final String nodeCriteria) {
        return client.target(url).path("/api/v2/nodes/" + nodeCriteria).request(MediaType.APPLICATION_JSON).get(OnmsNode.class);
    }

    public OnmsNode getNode(final int nodeId) {
        return getNode(String.valueOf(nodeId));
    }

    private OnmsSnmpInterfaceList getSnmpInterfaces(final int nodeId) {
        return client.target(url).path("/rest/nodes/" + nodeId + "/snmpinterfaces")
                .queryParam("limit", "0")
                .request(MediaType.APPLICATION_JSON)
                .get(OnmsSnmpInterfaceList.class);
    }

    public Map<OnmsSnmpInterface, Optional<DescriptionEntity>> getDescriptionMap(final int nodeId, final List<String> providers, final String format) {
        final OnmsSnmpInterfaceList onmsSnmpInterfaceList = getSnmpInterfaces(nodeId);
        final Map<OnmsSnmpInterface, Optional<DescriptionEntity>> descriptionMap = onmsSnmpInterfaceList.getObjects().stream()
                .collect(Collectors.toMap(Function.identity(), e -> Optional.empty()));

        final Map<String, OnmsSnmpInterface> ifIndexMap = descriptionMap.keySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getIfIndex()), Function.identity()));
        final Map<String, OnmsSnmpInterface> interfaceMap = descriptionMap.keySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getIfName()), Function.identity()));

        if (providers.contains("cdp")) {
            descriptionProviderMap.get("cdp").popuplateDescriptionMap(nodeId, descriptionMap, ifIndexMap, interfaceMap, format);
        }

        if (providers.contains("lldp")) {
            descriptionProviderMap.get("lldp").popuplateDescriptionMap(nodeId, descriptionMap, ifIndexMap, interfaceMap, format);
        }

        if (providers.contains("bridge")) {
            descriptionProviderMap.get("bridge").popuplateDescriptionMap(nodeId, descriptionMap, ifIndexMap, interfaceMap, format);
        }

        return descriptionMap;
    }

    public OnmsIpInterfaceList getIpInterfacesForIpAddress(final String ipAddress) {
        return client.target(url).path("/api/v2/ipinterfaces")
                .queryParam("_s", "ipAddress=="+ipAddress)
                .request(MediaType.APPLICATION_JSON).get(OnmsIpInterfaceList.class);
    }
}
