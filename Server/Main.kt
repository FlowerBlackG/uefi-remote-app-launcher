/*
 * gongty [at] tongji [dot] edu [dot] cn
 * Created on 2024.12.7 at Jiangchuan, Minhang, Shanghai
 */

package cn.edu.sjtu.ddst.uefiserver

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.system.exitProcess

typealias FileMap = java.util.HashMap<ULong, String>


fun buildFileMap(rootDir: String, stdOutputPrefix: String = ""): FileMap {

    var id = 0uL
    val map = FileMap()

    File(rootDir).listFiles()?.forEach { file ->
        if (file.isFile) {
            println("$stdOutputPrefix$id -> ${file.absolutePath}")
            map[id] = file.absolutePath
            id ++
        }
        else
            println("$stdOutputPrefix${file.absolutePath} [Ignored]")
    }

    return map
}


fun InputStream.readVesperHeader(): Triple<String, UInt, ULong>? {
    val byteArr = try {
        this.readNBytes(VesperProtocol.HEADER_LEN)
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }


    if (byteArr.size != VesperProtocol.HEADER_LEN)
        return null

    val buf = ByteBuffer.wrap(byteArr)

    val magicByteArr = ByteArray(4)  // magic 4字节
    buf.get(magicByteArr)
    val magic = String(magicByteArr)

    val type = buf.getInt().toUInt()
    val length = buf.getLong().toULong()

    return Triple(magic, type, length)
}


@OptIn(ExperimentalStdlibApi::class)
fun Socket.readVesperProtocolMessage(): VesperProtocol? {
    val client = this
    val reader = this.inputStream

    val requestHeader = reader.readVesperHeader()

    if (requestHeader == null) {
        println("[main.kt serveClient] (${client.inetAddress.hostAddress}) Failed to read header. Bad connection!")
        return null
    }

    val magic = requestHeader.first

    if (magic != AppServerProtocols.Base.MAGIC_STR) {
        println("[main.kt serveClient] (${client.inetAddress.hostAddress}) Bad magic!")
        return null
    }

    val type = requestHeader.second
    val length = requestHeader.third
    if (length > 12uL * 1024uL) {
        println("[main.kt serveClient] (${client.inetAddress.hostAddress}) $length is too large!")
        return null
    }

    println("[Read Vesper Protocol Msg] Type: 0x${type.toHexString()}, Len: $length")

    val dataArr = reader.readNBytes(length.toInt())
    if (dataArr.size != length.toInt()) {
        println("[main.kt serveClient] (${client.inetAddress.hostAddress}) Bad message. Length ${dataArr.size} != $length .")
        return null
    }

    println("[Read Vesper Protocol Msg] Received data, length is ${dataArr.size}")

    return VesperProtocol.decode(ByteBuffer.wrap(dataArr), magic, type)
}


fun Socket.sendError(code: Int, msg: String) {
    val response = AppServerProtocols.Response()
    response.code = code
    response.msg = msg.toByteArray()

    this.outputStream.write(response.toByteArray())
}


fun AppServerProtocols.ListFiles.process(client: Socket, fileMap: FileMap) {
    println("[Server] Message: List Files")

    val response = AppServerProtocols.Response()

    val msg = ArrayList<Byte>()

    fileMap.forEach { fileId, filePath ->
        msg.addAll(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(fileId.toLong()).array().toList())
        msg.addAll(File(filePath).name.toByteArray().toList())
        msg.add(0)
    }

    response.code = 0
    response.msg = msg.toByteArray()
    client.outputStream.write(response.toByteArray())
}


fun AppServerProtocols.FetchFile.process(client: Socket, fileMap: FileMap) {
    println("[Server] Message: Fetch Files")

    val fileId = this.fileId
    if (!fileMap.contains(fileId)) {
        client.sendError(1, "No such file!")
        return
    }

    val response = AppServerProtocols.Response()
    val file = fileMap[fileId]!!

    response.code = 0
    response.msg = File(file).readBytes()
    client.outputStream.write(response.toByteArray())
}


@OptIn(ExperimentalStdlibApi::class)
fun serveClient(client: Socket, fileMap: FileMap) {
    while (true) {
        val message = client.readVesperProtocolMessage()
        if (message == null) {
            println("[Serve Client] Message is null!")
            break
        }

        when (message.type) {
            AppServerProtocols.FetchFile.typeCode -> (message as AppServerProtocols.FetchFile).process(client, fileMap)
            AppServerProtocols.ListFiles.typeCode -> (message as AppServerProtocols.ListFiles).process(client, fileMap)
            else -> {
                println("[Serve Client] Type ${message.type} unknown!")
            }
        }

        println("Received: ${message.magicStr} 0x${message.type.toHexString()}")
    }

    println("Connection with ${client.inetAddress.hostAddress} closed.")
    client.close()
}


fun runServer(port: Int, dir: String): Int {

    println("Files in $dir:")
    val fileMap = buildFileMap(dir, "> ")

    val server = ServerSocket(port)
    println("Server started on port $port.")

    while (true) {
        val client = server.accept()
        println("Client connected: ${client.inetAddress.hostAddress}")

        thread {
            serveClient(client, fileMap)
        }
    }

    return 0
}


fun main(args: Array<String>) {

    if (args.size < 2) {
        println("Usage: server [port] [EFI binary dir]")
        exitProcess(1)
    }

    val port = args[0].toInt()
    val dir = args[1]

    exitProcess(runServer(port, dir))
}
