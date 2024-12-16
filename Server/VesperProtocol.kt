/*
 * vesper protocols
 *
 * gongty [at] tongji [dot] edu [dot] cn
 * Created on 2024.3.8 at Anting, Jiading, Shanghai
 */

package cn.edu.sjtu.ddst.uefiserver


import java.nio.ByteBuffer


open class VesperProtocol {

    companion object {
        const val MAGIC_STR = "...."
        const val typeCode = 0x0000u
        const val HEADER_LEN = 16

        /**
         * data should point to the first byte after header . caller should ensure body length is correct.
         */
        fun decode(data: ByteBuffer, magic: String, type: UInt): VesperProtocol? {

            // construct object by magic and type.

            val p = when (magic) {
                AppServerProtocols.Base.MAGIC_STR -> when (type) {
                    AppServerProtocols.Response.typeCode -> AppServerProtocols.Response()
                    AppServerProtocols.ListFiles.typeCode -> AppServerProtocols.ListFiles()
                    AppServerProtocols.FetchFile.typeCode -> AppServerProtocols.FetchFile()
                    else -> {
                        println("[Vesper Protocol] Type $type is unknown!")
                        return null
                    }
                }
                else -> {
                    println("[Vesper Protocol] Magic $magic is unknown!")
                    return null
                }

            }

            // Decode body and return.

            return when (p.decodeBody(data)) {
                0 -> p
                else -> null
            }
        } // fun decode
    } // companion object of interface VesperProtocol

    open val magicStr get() = MAGIC_STR // should be overridden
    open val type get() = typeCode // should be overridden

    fun encode(container: ArrayList<Byte>) {
        container.clear()

        /* header */

        // magic

        magicStr.forEach { ch -> container.add(ch.code.toByte()) }

        // type

        ByteBuffer.allocate(Int.SIZE_BYTES).putInt(type.toInt()).array().forEach {
            container.add(it)
        }

        // body length

        ByteBuffer.allocate(ULong.SIZE_BYTES).putLong(bodyLength.toLong()).array().forEach {
            container.add(it)
        }

        // body

        encodeBody(container)
    }

    fun toByteArray(): ByteArray {
        val arr = ArrayList<Byte>()
        this.encode(arr)
        return arr.toByteArray()
    }

    fun toByteBuffer(): ByteBuffer {
        return ByteBuffer.wrap( toByteArray() )
    }

    open fun decodeBody(data: ByteBuffer): Int {
        return 0
    }

    protected open val bodyLength: ULong
        get() = 0uL

    protected open fun encodeBody(container: ArrayList<Byte>) {

    }

} // open class VesperProtocol

