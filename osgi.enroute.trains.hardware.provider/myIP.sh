#!/bin/sh
# get local IP address
#       on machine with multiple network interfaces,
#       get IP address of default interface

defaultNetif() {
    case $(uname -s) in
    Darwin)
#      Destination    Gateway        Flags    Refs      Use   Netif Expire
#      default        192.168.2.1    UGSc       37        0     en0
#      127            127.0.0.1      UCS         0        0     lo0
        netstat -rn | perl -ne 'print $1 if (/^default.* ([[:alpha:]]\S+)/);'
        ;;
    *)
#    Kernel IP routing table
#    Destination   Gateway       Genmask       Flags MSS Window  irtt Iface
#    0.0.0.0       192.168.2.1   0.0.0.0       UG      0 0          0 wlan0
#    172.17.0.0    0.0.0.0       255.255.0.0   U       0 0          0 docker0
        netstat -rn | perl -ne 'print $1 if (/^0.0.0.0.* ([[:alpha:]]\S+)/);'
        ;;
    esac
}

ifconfig $(defaultNetif) |
  perl -ne 'print $1 if (/\s*inet (?:addr:)*(\S*).*/);'
#
