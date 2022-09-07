package org.opennms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import freemarker.template.Template;
import org.junit.Test;
import org.opennms.provider.LldpProvider;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnlinkdDescriptionsTest {
    @Test
    public void checkSysObjectId() throws Exception {
        final EnlinkdDescriptions enlinkdDescriptions = new EnlinkdDescriptions();
        Optional<Template> templateOptional;

        templateOptional = enlinkdDescriptions.getTemplate(".1.3.6.1.4.1.9.1.1");
        assertTrue(templateOptional.isPresent());
        assertEquals("cisco-ios.ftlh", templateOptional.get().getName());

        templateOptional = enlinkdDescriptions.getTemplate(".1.3.6.1.4.1.9.1.1.2.3");
        assertTrue(templateOptional.isPresent());
        assertEquals("cisco-ios.ftlh", templateOptional.get().getName());

        templateOptional = enlinkdDescriptions.getTemplate(".1.3.6.1.4.1.2636.1");
        assertTrue(templateOptional.isPresent());
        assertEquals("juniper-junos.ftlh", templateOptional.get().getName());

        templateOptional = enlinkdDescriptions.getTemplate(".1.3.6.1.4.1.2636.1.1.1.2.137");
        assertTrue(templateOptional.isPresent());
        assertEquals("juniper-junos.ftlh", templateOptional.get().getName());
    }

    @Test
    public void testLldpIfName() {
        assertEquals("Gi1/0/19", LldpProvider.getIfName("GigabitEthernet1/0/19(interfaceName:Gi1/0/19)"));
        assertEquals("xe-0/0/16", LldpProvider.getIfName("xe-0/0/16(ifindex:529)(local:529)"));
    }
}
