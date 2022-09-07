# Enhanced Linkd Port Descriptions

OpenNMS uses the service Enhanced Linkd to collect topology data from network devices.
This includes links reported by Cisco Discovery Protocol or Link Layer Discovery Protocol but also links that are computed out of the MAC address tables.

This utility uses this data to generate config snippets for interface port descriptions.

## Building

The project has some dependencies to OpenNMS artifacts and can be build with maven:

    $ mvn install

## Running

After building the tool can be invoked by the shell script:

    $ enlinkd-descriptions.sh
    You need to specify a node by --node or --ip-address.
    Usage: enlinkd-descriptions.sh [options...]

    --format (-f) VAL        : Format of the description output. Use %h, %(h),
                               %[h] for remote hostname and %p, %(p), %[p] for
                               remote port. (default: Link to %h %[p])
    --ip-address (-i) VAL    : IP-Address of node to generate configuration for.
    --node (-n) VAL          : Node to generate configuration for.
    --password (-P) VAL      : Password for accessing OpenNMS ReST endpoints.
    --provider (-p) STRING[] : List and precedence of providers (cdp, lldp,
                               bridge) to use.
    --save (-s)              : Add configuration command to persist configuration.
                               (default: false)
    --url (-u) VAL           : URL of your OpenNMS installation. (default:
                               http://localhost:8980/opennms)
    --username (-U) VAL      : Username for accessing OpenNMS ReST endpoints.

In order to connect to an OpenNMS instance you need to provide the URL and the credentials to query the ReST endpoints.
For querying a node for its links you have to provide at least a node criteria (nodeId or foreignSource:foreignId) or an IP-address.

    $ enlinkd-descriptions.sh --url https://opennms.somewhere.org/opennms --username johndoe --password secret --node Network:SwitchA

By default CDP, LLDP and the Bridge links will be used to determine the interface descriptions.
The first provider with corresponding data will define the interface's description.
You can enable/disable providers and define their precedence. 
The following example will use only LLDP and Bridge links to compute the interface descriptions:

    $ enlinkd-descriptions.sh --url https://opennms.somewhere.org/opennms --username johndoe --password secret --node Network:SwitchA --provider lldp bridge
  
By adding `--save` to your command line the generated configuration will include also the commands to persist the configuration on your network device.
Furthermore you can define the format of the description.
For this the following placeholder will be replaced in the output:

| Placeholder |                                        Replacement                                        |
|-------------|:-----------------------------------------------------------------------------------------:|
| %h          |                                      remote hostname                                      |
| %(h)        |    remote hostname in brackets, <br/>brackets will be ommitted if hostname is not set     |
| %[h]        | remote hostname in square brackets, <br/>brackets will be ommitted if hostname is not set |
| %p          |                                        remote port                                        |
| %(p)        |        remote port in brackets, </br>brackets will be ommitted if port is not set         |
| %[p]        |     remote port in square brackets, <br/>brackets will be ommitted if port is not set     |

The following example shows the output for a Cisco switch:

    $ enlinkd-descriptions.sh --url https://opennms.somewhere.org/opennms --username johndoe --password secret --node Network:SwitchB --save
    Generating interface descriptions for node Switch-Building-B using providers [cdp, lldp, bridge]
    --- CONFIG START ---
        configure terminal
            interface GigabitEthernet1/0/1
                # source CDP, type ethernetCsmacd
                description Link to Switch-B-1st-Floor [Gi1/0/42]
                exit
            interface TenGigabitEthernet1/1/3
                # source CDP, type ethernetCsmacd
                description Link to Switch-B-2nd-Floor [Te1/0/10]
                exit
            interface GigabitEthernet1/0/6
                # source Bridge, type ethernetCsmacd
                description Link to Web-Server-B
                exit
            interface GigabitEthernet1/0/7
                # source Bridge, type ethernetCsmacd
                description Link to Database-Server-B 
                exit
        copy running-config startup-config
    --- CONFIG END ---

You can also define common used command line parameters in a `enlinkd-descriptions.properties` file, for instance the URL and credentials:

    url=https://opennms.somewhere.org/opennms
    username=johndoe
    password=secret
    save=true
