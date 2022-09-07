package org.opennms;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.ws.rs.client.ResponseProcessingException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.opennms.api.DescriptionEntity;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsIpInterfaceList;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsSnmpInterface;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EnlinkdDescriptions {

    @Option(required = false, name = "--url", aliases = "-u", usage = "URL of your OpenNMS installation.")
    private String url = "http://localhost:8980/opennms";

    @Option(required = false, name = "--username", aliases = "-U", usage = "Username for accessing OpenNMS ReST endpoints.")
    private String username = null;

    @Option(required = false, name = "--password", aliases = "-P", usage = "Password for accessing OpenNMS ReST endpoints.")
    private String password = null;

    @Option(required = false, name = "--provider", aliases = "-p", handler = StringArrayOptionHandler.class, usage = "List and precedence of providers (cdp, lldp, bridge) to use.")
    private List<String> providers = null;

    @Option(required = false, name = "--node", aliases = "-n", forbids = {"--ip-address"}, usage = "Node to generate configuration for.")
    private String node = null;

    @Option(required = false, name = "--ip-address", aliases = "-i", forbids = {"--node"}, usage = "IP-Address of node to generate configuration for.")
    private String ipAddress = null;

    @Option(required = false, name = "--format", aliases = "-f", usage = "Format of the description output. Use %h, %(h), %[h] for remote hostname and %p, %(p), %[p] for remote port.")
    private String format = "Link to %h %[p]";

    @Option(required = false, name = "--save", aliases = "-s", usage = "Add configuration command to persist configuration.")
    private Boolean save = false;

    EnlinkdDescriptions() {
    }

    Optional<Template> getTemplate(final String sysObjectId) throws IOException {
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(this.getClass(), "/templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);

        final Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/vendors.properties"));
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (sysObjectId.startsWith((String) entry.getKey())) {
                final Template template = configuration.getTemplate(((String) entry.getValue()).toLowerCase().replace(" ", "-") + ".ftlh");
                return Optional.of(template);
            }
        }

        return Optional.empty();
    }

    private Optional<OnmsNode> determineNode(final DescriptionService descriptionService) {
        if (node != null) {
            return Optional.of(descriptionService.getNode(node));
        } else {
            try {
                final OnmsIpInterfaceList onmsIpInterfaceList = descriptionService.getIpInterfacesForIpAddress(ipAddress);
                if (onmsIpInterfaceList == null || onmsIpInterfaceList.size() == 0) {
                    System.err.println("The query for IP-addresses return no interfaces.");
                    System.exit(1);
                } else {
                    if (onmsIpInterfaceList.size() == 1) {
                        return Optional.of(descriptionService.getNode(String.valueOf(onmsIpInterfaceList.get(0).getNodeId())));
                    } else {
                        System.err.println("The query for IP-addresses return more than one interface:");
                        for (final OnmsIpInterface onmsIpInterface : onmsIpInterfaceList) {
                            final OnmsNode nodeCandidate = descriptionService.getNode(onmsIpInterface.getNodeId());
                            System.err.println(nodeCandidate.getLabel() + ": nodeId=" + nodeCandidate.getNodeId() + ", FS:FID=" + nodeCandidate.getForeignSource() + ":" + nodeCandidate.getForeignId());
                        }
                        return Optional.empty();
                    }
                }
            } catch (ResponseProcessingException e) {
                System.err.println("Error processing IP interface list.");
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void execute(String[] args) {
        loadDefaults();

        final ParserProperties parserProperties = ParserProperties.defaults();
        parserProperties.withUsageWidth(80);
        final CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            if (node == null && ipAddress == null) {
                throw new CmdLineException(parser, "You need to specify a node by --node or --ip-address.");
            }

            if (providers == null) {
                providers = Lists.newArrayList("cdp", "lldp", "bridge");
            }

            providers = providers.stream().map(e -> e.toLowerCase()).collect(Collectors.toList());

            for (final String provider : providers) {
                if (!"cdp".equals(provider) && !"lldp".equals(provider) && !"bridge".equals(provider)) {
                    throw new CmdLineException(parser, "Unknown provider '" + provider + "'.");
                }
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: enlinkd-descriptions.sh [options...]\n");
            new CmdLineParser(new EnlinkdDescriptions()).printUsage(System.err);
            System.err.println();
            return;
        }

        while (username == null || "".equals(username)) {
            username = System.console().readLine("Username: ");
        }

        while (password == null || "".equals(password)) {
            password = new String(System.console().readPassword("Password: "));
        }

        final DescriptionService descriptionService = new DescriptionService(url, username, password);

        final Optional<OnmsNode> optionalOnmsNode = determineNode(descriptionService);

        if (optionalOnmsNode.isPresent()) {
            final OnmsNode onmsNode = optionalOnmsNode.get();

            System.out.println("Generating interface descriptions for node " + onmsNode.getLabel() + " using providers " + providers);

            final Map<OnmsSnmpInterface, Optional<DescriptionEntity>> descriptionMap = descriptionService.getDescriptionMap(onmsNode.getId(), providers, format);

            try {
                final Optional<Template> templateOptional = getTemplate(onmsNode.getSysObjectId());

                if (templateOptional.isPresent()) {
                    final Template template = templateOptional.get();
                    final Map<String, Object> model = new HashMap<>();

                    model.put("descriptions", descriptionMap.entrySet().stream().filter(e -> e.getValue().isPresent()).collect(Collectors.toSet()));
                    model.put("save", save);

                    final Writer out = new OutputStreamWriter(System.out);
                    System.out.println("--- CONFIG START ---");
                    template.process(model, out);
                    System.out.println("--- CONFIG END ---");
                } else {
                    System.err.println("No vendor template found for sysObjectId " + onmsNode.getSysObjectId());
                    System.exit(1);
                }
            } catch (IOException e) {
                System.err.println("Error retrieving template.");
                e.printStackTrace(System.err);
            } catch (TemplateException e) {
                System.err.println("Error processing template.");
                e.printStackTrace(System.err);
            }
        }
    }

    private void loadDefaults() {
        final Properties properties = new Properties();
        try {
            properties.load(new FileReader("enlinkd-descriptions.properties"));

            if (properties.containsKey("url")) {
                url = properties.getProperty("url");
            }

            if (properties.containsKey("username")) {
                username = properties.getProperty("username");
            }

            if (properties.containsKey("password")) {
                password = properties.getProperty("password");
            }

            if (properties.containsKey("ipAddress")) {
                ipAddress = properties.getProperty("ipAddress");
            }

            if (properties.containsKey("node")) {
                node = properties.getProperty("node");
            }

            if (properties.containsKey("providers")) {
                providers = Lists.newArrayList(properties.getProperty("providers").split(", "));
            }

            if (properties.containsKey("save")) {
                save = Boolean.valueOf(properties.getProperty("save"));
            }
        } catch (IOException e) {
            System.err.println("No enlinkd-descriptions.properties file for defaults found.");
        }
    }

    public static void main(String[] args) {
        new EnlinkdDescriptions().execute(args);
    }
}
