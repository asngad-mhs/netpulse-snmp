package com.example.snmp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

/**
 * High performance, non-blocking UDP SNMP client for Android.
 * Performs real SNMP v1 / v2c queries to network hardware (MikroTik, Cisco, Ruijie, OpenWrt).
 */
class SnmpClient(
    private val timeoutMs: Int = 2000
) {
    private val reqIdGen = AtomicInteger(1000)

    data class SnmpResult(
        val success: Boolean,
        val latencyMs: Long,
        val varBinds: List<Asn1Ber.VarBind> = emptyList(),
        val errorMessage: String? = null
    )

    /**
     * Send an SNMP GET request for one or more OIDs
     */
    suspend fun snmpGet(
        host: String,
        port: Int = 161,
        community: String = "public",
        version: Int = 1, // 0 = SNMPv1, 1 = SNMPv2c
        oids: List<String>
    ): SnmpResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: DatagramSocket? = null
        try {
            val reqId = reqIdGen.incrementAndGet()
            val requestBytes = Asn1Ber.buildGetRequest(version, community, reqId, oids)
            val address = InetAddress.getByName(host)

            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            val outPacket = DatagramPacket(requestBytes, requestBytes.size, address, port)
            socket.send(outPacket)

            val inBuf = ByteArray(4096)
            val inPacket = DatagramPacket(inBuf, inBuf.size)
            socket.receive(inPacket)

            val latency = System.currentTimeMillis() - startTime
            val receivedBytes = inPacket.data.copyOfRange(0, inPacket.length)
            val reader = Asn1Ber.BerReader(receivedBytes)
            val msg = reader.parseMessage()

            if (msg != null && msg.pdu.errorStatus == 0) {
                SnmpResult(
                    success = true,
                    latencyMs = latency,
                    varBinds = msg.pdu.varBinds
                )
            } else if (msg != null) {
                SnmpResult(
                    success = false,
                    latencyMs = latency,
                    errorMessage = "SNMP Error status code ${msg.pdu.errorStatus}"
                )
            } else {
                SnmpResult(
                    success = false,
                    latencyMs = latency,
                    errorMessage = "Failed to parse SNMP response"
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            SnmpResult(
                success = false,
                latencyMs = latency,
                errorMessage = e.message ?: "Connection timeout / unreachable host"
            )
        } finally {
            socket?.close()
        }
    }

    /**
     * Perform an SNMP WALK (successive GETNEXT) starting at rootOid
     */
    suspend fun snmpWalk(
        host: String,
        port: Int = 161,
        community: String = "public",
        version: Int = 1,
        rootOid: String,
        maxRepetitions: Int = 30
    ): SnmpResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: DatagramSocket? = null
        val accumulated = mutableListOf<Asn1Ber.VarBind>()
        var currentOid = rootOid

        try {
            val address = InetAddress.getByName(host)
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            for (i in 0 until maxRepetitions) {
                val reqId = reqIdGen.incrementAndGet()
                val requestBytes = Asn1Ber.buildGetNextRequest(version, community, reqId, currentOid)
                val outPacket = DatagramPacket(requestBytes, requestBytes.size, address, port)
                socket.send(outPacket)

                val inBuf = ByteArray(4096)
                val inPacket = DatagramPacket(inBuf, inBuf.size)
                socket.receive(inPacket)

                val receivedBytes = inPacket.data.copyOfRange(0, inPacket.length)
                val reader = Asn1Ber.BerReader(receivedBytes)
                val msg = reader.parseMessage() ?: break

                if (msg.pdu.errorStatus != 0 || msg.pdu.varBinds.isEmpty()) break

                val vb = msg.pdu.varBinds[0]
                if (!vb.oid.startsWith(rootOid.removePrefix(".")) && !vb.oid.contains(rootOid.removePrefix("."))) {
                    break // out of subtree
                }

                accumulated.add(vb)
                currentOid = vb.oid
            }

            val latency = System.currentTimeMillis() - startTime
            SnmpResult(
                success = accumulated.isNotEmpty(),
                latencyMs = latency,
                varBinds = accumulated,
                errorMessage = if (accumulated.isEmpty()) "No MIB entries found under $rootOid" else null
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            SnmpResult(
                success = accumulated.isNotEmpty(),
                latencyMs = latency,
                varBinds = accumulated,
                errorMessage = e.message ?: "SNMP Walk timed out"
            )
        } finally {
            socket?.close()
        }
    }
}
