package com.example.snmp

import java.io.ByteArrayOutputStream

/**
 * Lightweight ASN.1 BER (Basic Encoding Rules) encoder and decoder for SNMP v1/v2c.
 * Pure Kotlin, zero-dependency, compatible with all Android SDKs (minSdk 24+).
 */
object Asn1Ber {
    const val TAG_INTEGER = 0x02
    const val TAG_OCTET_STRING = 0x04
    const val TAG_NULL = 0x05
    const val TAG_OID = 0x06
    const val TAG_SEQUENCE = 0x30

    // Application specific tags for SNMP
    const val TAG_IP_ADDRESS = 0x40
    const val TAG_COUNTER32 = 0x41
    const val TAG_GAUGE32 = 0x42
    const val TAG_TIMETICKS = 0x43
    const val TAG_OPAQUE = 0x44
    const val TAG_COUNTER64 = 0x46

    // Context specific PDU tags
    const val TAG_GET_REQUEST = 0xA0
    const val TAG_GET_NEXT_REQUEST = 0xA1
    const val TAG_GET_RESPONSE = 0xA2
    const val TAG_SET_REQUEST = 0xA3
    const val TAG_GET_BULK_REQUEST = 0xA5

    fun encodeLength(length: Int): ByteArray {
        return if (length < 128) {
            byteArrayOf(length.toByte())
        } else if (length < 256) {
            byteArrayOf(0x81.toByte(), length.toByte())
        } else {
            byteArrayOf(0x82.toByte(), ((length shr 8) and 0xFF).toByte(), (length and 0xFF).toByte())
        }
    }

    fun encodeTlv(tag: Int, value: ByteArray): ByteArray {
        val len = encodeLength(value.size)
        val out = ByteArray(1 + len.size + value.size)
        out[0] = tag.toByte()
        System.arraycopy(len, 0, out, 1, len.size)
        System.arraycopy(value, 0, out, 1 + len.size, value.size)
        return out
    }

    fun encodeInteger(value: Long): ByteArray {
        var v = value
        val bytes = mutableListOf<Byte>()
        do {
            bytes.add((v and 0xFF).toByte())
            v = v shr 8
        } while (v > 0 || (v == 0L && (bytes.last().toInt() and 0x80) != 0))
        bytes.reverse()
        return encodeTlv(TAG_INTEGER, bytes.toByteArray())
    }

    fun encodeOctetString(str: String): ByteArray {
        val bytes = str.toByteArray(Charsets.UTF_8)
        return encodeTlv(TAG_OCTET_STRING, bytes)
    }

    fun encodeNull(): ByteArray {
        return byteArrayOf(TAG_NULL.toByte(), 0x00)
    }

    fun encodeOid(oidStr: String): ByteArray {
        val clean = oidStr.trim().removePrefix(".")
        val parts = clean.split(".").mapNotNull { it.trim().toLongOrNull() }
        if (parts.size < 2) return byteArrayOf(TAG_OID.toByte(), 0x00)

        val bos = ByteArrayOutputStream()
        // First byte is (first * 40) + second
        val firstByte = (parts[0] * 40 + parts[1]).toByte()
        bos.write(firstByte.toInt())

        for (i in 2 until parts.size) {
            var v = parts[i]
            val subBytes = mutableListOf<Byte>()
            subBytes.add((v and 0x7F).toByte())
            v = v shr 7
            while (v > 0) {
                subBytes.add(((v and 0x7F) or 0x80).toByte())
                v = v shr 7
            }
            subBytes.reverse()
            subBytes.forEach { bos.write(it.toInt()) }
        }

        return encodeTlv(TAG_OID, bos.toByteArray())
    }

    fun encodeSequence(vararg elements: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        elements.forEach { bos.write(it) }
        return encodeTlv(TAG_SEQUENCE, bos.toByteArray())
    }

    data class BerValue(
        val tag: Int,
        val rawValue: ByteArray,
        val stringValue: String,
        val longValue: Long? = null
    )

    data class VarBind(
        val oid: String,
        val value: BerValue
    )

    data class SnmpPdu(
        val requestId: Int,
        val errorStatus: Int,
        val errorIndex: Int,
        val varBinds: List<VarBind>
    )

    data class SnmpMessage(
        val version: Int,
        val community: String,
        val pdu: SnmpPdu
    )

    class BerReader(private val data: ByteArray, private var offset: Int = 0) {
        fun hasRemaining(): Boolean = offset < data.size

        fun readTag(): Int {
            if (!hasRemaining()) return -1
            return data[offset++].toInt() and 0xFF
        }

        fun readLength(): Int {
            if (!hasRemaining()) return 0
            val b = data[offset++].toInt() and 0xFF
            if ((b and 0x80) == 0) {
                return b
            }
            val numBytes = b and 0x7F
            var len = 0
            for (i in 0 until numBytes) {
                if (!hasRemaining()) break
                len = (len shl 8) or (data[offset++].toInt() and 0xFF)
            }
            return len
        }

        fun readBytes(length: Int): ByteArray {
            val actualLen = length.coerceAtMost(data.size - offset)
            val bytes = ByteArray(actualLen)
            System.arraycopy(data, offset, bytes, 0, actualLen)
            offset += actualLen
            return bytes
        }

        fun decodeOid(bytes: ByteArray): String {
            if (bytes.isEmpty()) return ""
            val sb = StringBuilder()
            val first = bytes[0].toInt() and 0xFF
            val node1 = first / 40
            val node2 = first % 40
            sb.append(".").append(node1).append(".").append(node2)

            var value = 0L
            for (i in 1 until bytes.size) {
                val b = bytes[i].toInt() and 0xFF
                value = (value shl 7) or (b and 0x7F).toLong()
                if ((b and 0x80) == 0) {
                    sb.append(".").append(value)
                    value = 0L
                }
            }
            return sb.toString()
        }

        fun decodeInteger(bytes: ByteArray): Long {
            var value = 0L
            var signExtended = false
            for (i in bytes.indices) {
                val b = bytes[i].toInt() and 0xFF
                if (i == 0 && (b and 0x80) != 0) {
                    signExtended = true
                    value = -1L
                }
                value = (value shl 8) or b.toLong()
            }
            return value
        }

        fun decodeUnsigned(bytes: ByteArray): Long {
            var value = 0L
            for (b in bytes) {
                value = (value shl 8) or (b.toInt() and 0xFF).toLong()
            }
            return value
        }

        fun parseVarBind(): VarBind? {
            val tag = readTag()
            if (tag != TAG_SEQUENCE) return null
            val len = readLength()
            val endOffset = offset + len

            val oidTag = readTag()
            val oidLen = readLength()
            val oidBytes = readBytes(oidLen)
            val oid = decodeOid(oidBytes)

            val valTag = readTag()
            val valLen = readLength()
            val valBytes = readBytes(valLen)

            val berVal = when (valTag) {
                TAG_INTEGER -> BerValue(valTag, valBytes, decodeInteger(valBytes).toString(), decodeInteger(valBytes))
                TAG_OCTET_STRING -> {
                    val str = String(valBytes, Charsets.UTF_8).filter { it.code in 32..126 || it == '\n' }
                    BerValue(valTag, valBytes, str)
                }
                TAG_OID -> BerValue(valTag, valBytes, decodeOid(valBytes))
                TAG_TIMETICKS -> {
                    val ticks = decodeUnsigned(valBytes)
                    BerValue(valTag, valBytes, "$ticks ticks", ticks)
                }
                TAG_COUNTER32, TAG_GAUGE32, TAG_COUNTER64 -> {
                    val num = decodeUnsigned(valBytes)
                    BerValue(valTag, valBytes, num.toString(), num)
                }
                TAG_IP_ADDRESS -> {
                    val ip = valBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                    BerValue(valTag, valBytes, ip)
                }
                TAG_NULL -> BerValue(valTag, valBytes, "NULL")
                else -> BerValue(valTag, valBytes, "raw[${valBytes.size}b]")
            }

            // advance to end of varbind if needed
            if (offset < endOffset) offset = endOffset

            return VarBind(oid, berVal)
        }

        fun parseMessage(): SnmpMessage? {
            try {
                val seqTag = readTag()
                if (seqTag != TAG_SEQUENCE) return null
                readLength()

                // 1. Version
                val verTag = readTag()
                val verLen = readLength()
                val verBytes = readBytes(verLen)
                val version = decodeInteger(verBytes).toInt()

                // 2. Community
                val commTag = readTag()
                val commLen = readLength()
                val commBytes = readBytes(commLen)
                val community = String(commBytes, Charsets.UTF_8)

                // 3. PDU
                val pduTag = readTag()
                readLength()

                val reqTag = readTag()
                val reqLen = readLength()
                val reqBytes = readBytes(reqLen)
                val reqId = decodeInteger(reqBytes).toInt()

                val errTag = readTag()
                val errLen = readLength()
                val errBytes = readBytes(errLen)
                val errStatus = decodeInteger(errBytes).toInt()

                val errIdxTag = readTag()
                val errIdxLen = readLength()
                val errIdxBytes = readBytes(errIdxLen)
                val errIndex = decodeInteger(errIdxBytes).toInt()

                val vbSeqTag = readTag()
                val vbSeqLen = readLength()
                val vbEnd = offset + vbSeqLen

                val varBinds = mutableListOf<VarBind>()
                while (offset < vbEnd && hasRemaining()) {
                    val vb = parseVarBind()
                    if (vb != null) varBinds.add(vb) else break
                }

                return SnmpMessage(
                    version = version,
                    community = community,
                    pdu = SnmpPdu(reqId, errStatus, errIndex, varBinds)
                )
            } catch (e: Exception) {
                return null
            }
        }
    }

    /**
     * Build an SNMP GetRequest packet for a given list of OIDs
     */
    fun buildGetRequest(version: Int, community: String, requestId: Int, oids: List<String>): ByteArray {
        val verBytes = encodeInteger(version.toLong())
        val commBytes = encodeOctetString(community)

        val reqIdBytes = encodeInteger(requestId.toLong())
        val errStatus = encodeInteger(0)
        val errIndex = encodeInteger(0)

        val vbListBytes = ByteArrayOutputStream()
        for (oid in oids) {
            val encodedOid = encodeOid(oid)
            val nullVal = encodeNull()
            val vb = encodeSequence(encodedOid, nullVal)
            vbListBytes.write(vb)
        }
        val varBindSeq = encodeTlv(TAG_SEQUENCE, vbListBytes.toByteArray())

        val pduContent = ByteArrayOutputStream()
        pduContent.write(reqIdBytes)
        pduContent.write(errStatus)
        pduContent.write(errIndex)
        pduContent.write(varBindSeq)

        val pdu = encodeTlv(TAG_GET_REQUEST, pduContent.toByteArray())
        return encodeSequence(verBytes, commBytes, pdu)
    }

    /**
     * Build an SNMP GetNextRequest packet
     */
    fun buildGetNextRequest(version: Int, community: String, requestId: Int, oid: String): ByteArray {
        val verBytes = encodeInteger(version.toLong())
        val commBytes = encodeOctetString(community)

        val reqIdBytes = encodeInteger(requestId.toLong())
        val errStatus = encodeInteger(0)
        val errIndex = encodeInteger(0)

        val encodedOid = encodeOid(oid)
        val nullVal = encodeNull()
        val vb = encodeSequence(encodedOid, nullVal)
        val varBindSeq = encodeTlv(TAG_SEQUENCE, vb)

        val pduContent = ByteArrayOutputStream()
        pduContent.write(reqIdBytes)
        pduContent.write(errStatus)
        pduContent.write(errIndex)
        pduContent.write(varBindSeq)

        val pdu = encodeTlv(TAG_GET_NEXT_REQUEST, pduContent.toByteArray())
        return encodeSequence(verBytes, commBytes, pdu)
    }
}
