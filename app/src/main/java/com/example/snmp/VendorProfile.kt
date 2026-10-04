package com.example.snmp

enum class NetworkVendor(val displayName: String, val defaultPort: Int, val description: String) {
    MIKROTIK("MikroTik", 161, "RouterOS v6/v7 (CCR, RB, Hex, CHR)"),
    CISCO("Cisco", 161, "Cisco IOS / IOS-XE / Catalyst / ISR"),
    RUIJIE("Ruijie", 161, "Ruijie Networks RGOS / Reyee Series"),
    OPENWRT("OpenWrt", 161, "OpenWrt Linux / Net-SNMP Embedded"),
    GENERIC("Generic SNMP", 161, "Standard MIB-II RFC 1213 Device")
}

data class VendorOidSpec(
    val vendor: NetworkVendor,
    val cpuLoadOid: String,
    val memoryUsageOid: String,
    val uptimeOid: String = ".1.3.6.1.2.1.1.3.0",
    val sysDescrOid: String = ".1.3.6.1.2.1.1.1.0",
    val sysNameOid: String = ".1.3.6.1.2.1.1.5.0",
    val clientCountOid: String,
    val vlanTableOid: String
)

object VendorOidRegistry {
    val MIKROTIK_SPEC = VendorOidSpec(
        vendor = NetworkVendor.MIKROTIK,
        cpuLoadOid = ".1.3.6.1.4.1.14988.1.1.1.3.1.5.1", // mtxrHlProcessorUsage or hrProcessorLoad
        memoryUsageOid = ".1.3.6.1.2.1.25.2.3.1.6.65536",
        clientCountOid = ".1.3.6.1.4.1.14988.1.1.1.2.1.1", // wireless reg table or arp
        vlanTableOid = ".1.3.6.1.2.1.17.7.1.4.3.1.1" // dot1qVlanStaticName
    )

    val CISCO_SPEC = VendorOidSpec(
        vendor = NetworkVendor.CISCO,
        cpuLoadOid = ".1.3.6.1.4.1.9.9.109.1.1.1.1.3.1", // cpmCPUTotal5secRev
        memoryUsageOid = ".1.3.6.1.4.1.9.9.48.1.1.1.6.1", // ciscoMemoryPoolFree
        clientCountOid = ".1.3.6.1.2.1.4.22.1.2", // ARP entries
        vlanTableOid = ".1.3.6.1.4.1.9.9.46.1.3.1.1.4.1" // vtpVlanName
    )

    val RUIJIE_SPEC = VendorOidSpec(
        vendor = NetworkVendor.RUIJIE,
        cpuLoadOid = ".1.3.6.1.4.1.4881.1.1.10.2.35.1.1.1.2.0", // ruijieCpuRate
        memoryUsageOid = ".1.3.6.1.4.1.4881.1.1.10.2.35.1.1.1.3.0", // ruijieMemoryRate
        clientCountOid = ".1.3.6.1.4.1.4881.1.1.10.2.79.1.1.1.4", // Ruijie AP associated clients
        vlanTableOid = ".1.3.6.1.4.1.4881.1.1.10.2.10.1.1.1.2" // ruijieVlanName
    )

    val OPENWRT_SPEC = VendorOidSpec(
        vendor = NetworkVendor.OPENWRT,
        cpuLoadOid = ".1.3.6.1.2.1.25.3.3.1.2.1", // hrProcessorLoad
        memoryUsageOid = ".1.3.6.1.4.1.2021.4.6.0", // memAvailReal
        clientCountOid = ".1.3.6.1.2.1.4.22.1.2", // ARP table client count
        vlanTableOid = ".1.3.6.1.2.1.17.7.1.4.3.1.1"
    )

    val GENERIC_SPEC = VendorOidSpec(
        vendor = NetworkVendor.GENERIC,
        cpuLoadOid = ".1.3.6.1.2.1.25.3.3.1.2.1",
        memoryUsageOid = ".1.3.6.1.2.1.25.2.3.1.6.1",
        clientCountOid = ".1.3.6.1.2.1.4.22.1.2",
        vlanTableOid = ".1.3.6.1.2.1.17.7.1.4.3.1.1"
    )

    fun getSpec(vendor: NetworkVendor): VendorOidSpec {
        return when (vendor) {
            NetworkVendor.MIKROTIK -> MIKROTIK_SPEC
            NetworkVendor.CISCO -> CISCO_SPEC
            NetworkVendor.RUIJIE -> RUIJIE_SPEC
            NetworkVendor.OPENWRT -> OPENWRT_SPEC
            NetworkVendor.GENERIC -> GENERIC_SPEC
        }
    }
}
