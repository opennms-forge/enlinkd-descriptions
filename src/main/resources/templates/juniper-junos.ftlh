    configure
<#list descriptions as entry>
        # source ${entry.getValue().get().getSource().getName()}, type ${entry.getValue().get().getIfType()}
        set interface ${entry.key.getIfDescr()} description ${entry.getValue().get().getDescription()}
</#list>
    exit
<#if save>
    commit
</#if>
