/*
 * gongty [at] tongji [dot] edu [dot] cn
 * Created on 2024.12.7 at Jiangchuan, Minhang, Shanghai
 */


package cn.edu.sjtu.ddst.uefiserver

import java.nio.ByteBuffer

class AppServerProtocols private constructor() {
    open class Base : VesperProtocol() {
        companion object {
            const val MAGIC_STR = "dDSt"
            const val typeCode = VesperProtocol.typeCode
        }

        override val magicStr get() = MAGIC_STR
        override val type get() = typeCode
    }


    class Response : Base() {
        companion object {
            const val typeCode = 0xA001u
        }

        override val type get() = typeCode

        override val bodyLength get() = (UInt.SIZE_BYTES * 2 + msg.size).toULong()

        var code = 0
        var msg: ByteArray = ByteArray(0)
        val msgString: String
            get() = String(msg)
        val msgAsString: String
            get() = msgString
        val msgAsInt: Int
            get() = ByteBuffer.wrap(msg).getInt()
        val msgAsLong: Long
            get() = ByteBuffer.wrap(msg).getLong()

        override fun decodeBody(data: ByteBuffer): Int {

            if (data.remaining() < UInt.SIZE_BYTES * 2) {
                println("[AppServerProtocols] length ${data.remaining()} is too few for body")
                return 1
            }

            code = data.getInt()
            val msgLen = data.getInt()

            if (data.remaining() < msgLen) {
                println("[AppServerProtocols] length ${data.remaining()} is less than msg-len $msgLen")
                return 2
            }

            msg = ByteArray(msgLen)
            data.get(msg)
            return 0
        }


        override fun encodeBody(container: ArrayList<Byte>) {
            // code
            container.addAll(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(code).array().toList())

            // msg len
            container.addAll(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(msg.size).array().toList())

            // msg
            container.addAll(msg.toList())

        }

    }


    class ListFiles : Base() {
        companion object {
            const val typeCode = 0x0101u
        }

        override val type get() = typeCode
        override val bodyLength get() = 0uL
    }


    class FetchFile() : Base() {

        companion object {
            const val typeCode = 0x0102u
        }


        override val type get() = typeCode
        override val bodyLength get() = ULong.SIZE_BYTES.toULong()


        var fileId = 0uL

        override fun decodeBody(data: ByteBuffer): Int {
            if (data.remaining() < ULong.SIZE_BYTES) {
                println("[DecodeBody of FetchFile] data size ${data.remaining()} is smaller than ${ULong.SIZE_BYTES}")
                return -1
            }

            val buf = ByteBuffer.allocate(ULong.SIZE_BYTES)

            for (i in 0 until ULong.SIZE_BYTES)
                buf.put(data.get())

            buf.flip()
            fileId = buf.getLong().toULong()

            return 0
        }
    }

}
