    configure terminal
<#list descriptions as entry>
        interface ${entry.key.getIfDescr()}
            # source ${entry.getValue().get().getSource().getName()}, type ${entry.getValue().get().getIfType()}
            description ${entry.getValue().get().getDescription()}
            exit
</#list>
        exit
<#if save>
    copy running-config startup-config
</#if>
