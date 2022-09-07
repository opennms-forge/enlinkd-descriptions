package org.opennms.api;

public abstract class AbstractDescriptionProvider implements DescriptionProvider {
    protected final String name;

    public AbstractDescriptionProvider(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    protected String format(final String format, final String remoteHostname, final String remotePort) {
        String string = format;
        if (remoteHostname != null) {
            string = string.replaceAll("%\\(h\\)", "(" + remoteHostname + ")");
            string = string.replaceAll("%\\[h]", "[" + remoteHostname + "]");
            string = string.replaceAll("%h", remoteHostname);
        } else {
            string = string.replaceAll("%\\(h\\)", "");
            string = string.replaceAll("%\\[h]", "");
            string = string.replaceAll("%h", "");
        }
        if (remotePort != null) {
            string = string.replaceAll("%\\(p\\)", "(" + remotePort + ")");
            string = string.replaceAll("%\\[p]", "[" + remotePort + "]");
            string = string.replaceAll("%p", remotePort);
        } else {
            string = string.replaceAll("%\\(p\\)", "");
            string = string.replaceAll("%\\[p]", "");
            string = string.replaceAll("%p", "");
        }
        return string;
    }
}
