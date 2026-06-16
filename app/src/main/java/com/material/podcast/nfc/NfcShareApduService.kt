package com.material.podcast.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Host Card Emulation service that makes this phone look like a passive NFC Forum
 * Type-4 NDEF tag while a share is in flight.
 *
 * It implements the minimal Type-4 Tag NDEF emulation state machine: the reader
 * selects our NDEF application by AID, then selects the Capability Container (CC)
 * file or the NDEF file, then issues READ BINARY commands to stream the file out.
 *
 * The NDEF message itself (a single URI "U" record carrying the `echoes://share?…`
 * deep link) is provided by [NfcShareController.currentNdefMessage]. When no share is
 * active we serve an empty message (NLEN = 0).
 *
 * Command/response data are standard ISO 7816-4 APDUs. Nothing here is podcast-specific
 * beyond the bytes returned for the NDEF file.
 */
class NfcShareApduService : HostApduService() {

    /** Which file the reader has currently SELECTed, so READ BINARY knows what to serve. */
    private enum class Selected { NONE, CC, NDEF }

    private var selected = Selected.NONE

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val apdu = commandApdu ?: return SW_NOT_FOUND

        // SELECT by name (AID): 00 A4 04 00 <len> <AID..> [Le]
        if (apdu.size >= 5 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x04.toByte() &&
            apdu[3] == 0x00.toByte()
        ) {
            val aidLen = apdu[4].toInt() and 0xFF
            if (apdu.size >= 5 + aidLen) {
                val aid = apdu.copyOfRange(5, 5 + aidLen)
                if (aid.contentEquals(NDEF_AID)) {
                    selected = Selected.NONE
                    return SW_OK
                }
            }
            return SW_NOT_FOUND
        }

        // SELECT by file ID: 00 A4 00 0C 02 <fileId hi> <fileId lo>
        if (apdu.size >= 7 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x00.toByte() &&
            apdu[3] == 0x0C.toByte() &&
            (apdu[4].toInt() and 0xFF) == 0x02
        ) {
            val fileId = byteArrayOf(apdu[5], apdu[6])
            return when {
                fileId.contentEquals(CC_FILE_ID) -> {
                    selected = Selected.CC
                    SW_OK
                }
                fileId.contentEquals(NDEF_FILE_ID) -> {
                    selected = Selected.NDEF
                    SW_OK
                }
                else -> SW_NOT_FOUND
            }
        }

        // READ BINARY: 00 B0 <offset hi> <offset lo> <Le>
        if (apdu.size >= 5 &&
            apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xB0.toByte()
        ) {
            val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
            val le = apdu[4].toInt() and 0xFF

            val file: ByteArray = when (selected) {
                Selected.CC -> CC_FILE
                Selected.NDEF -> ndefFile()
                Selected.NONE -> return SW_NOT_FOUND
            }

            if (offset > file.size) return SW_WRONG_OFFSET
            val end = minOf(offset + le, file.size)
            val slice = file.copyOfRange(offset, end)
            return slice + SW_OK
        }

        return SW_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        // Reader gone (or link lost): forget which file was selected.
        selected = Selected.NONE
    }

    /**
     * The NDEF file as seen by a Type-4 reader: a 2-byte big-endian NLEN length field
     * followed by the NDEF message bytes. When no share is active, NLEN = 0.
     */
    private fun ndefFile(): ByteArray {
        val message = NfcShareController.currentNdefMessage() ?: ByteArray(0)
        val nlen = message.size
        val out = ByteArray(2 + message.size)
        out[0] = ((nlen shr 8) and 0xFF).toByte()
        out[1] = (nlen and 0xFF).toByte()
        System.arraycopy(message, 0, out, 2, message.size)
        return out
    }

    companion object {
        /** NFC Forum Type-4 NDEF application identifier. */
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
        )

        /** Capability Container file id (E103) and NDEF file id (E104). */
        private val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03)
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04)

        /**
         * Capability Container (15 bytes), per NFC Forum Type-4 Tag spec:
         *  - CCLEN          = 00 0F (15)
         *  - Mapping ver.   = 20 (v2.0)
         *  - MLe            = 00 3B (max bytes read in one ReadBinary)
         *  - MLc            = 00 34 (max bytes in one UpdateBinary)
         *  - NDEF File TLV  = 04 06 E104 0FFF FF
         *      T = 04, L = 06, file id = E104, max NDEF size = 0FFF,
         *      read access = 00 (granted), write access = FF (read-only)
         */
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0x3B,
            0x00, 0x34,
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            0x0F, 0xFF.toByte(),
            0x00,
            0xFF.toByte(),
        )

        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        private val SW_WRONG_OFFSET = byteArrayOf(0x6B, 0x00)
    }
}
