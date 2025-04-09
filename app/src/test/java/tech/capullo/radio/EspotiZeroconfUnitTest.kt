package tech.capullo.radio

class EspotiZeroconfUnitTest {
    /*
    private suspend fun testClientSocket(hostname: String, port: Int) =
        aSocket(SelectorManager(Dispatchers.IO))
            .tcp()
            .connect(hostname, port)
            .also { println("client connected as ${it.localAddress}") }

    @Test
    fun clientConnectsToLocalhost_ConnectionIsSuccessful() = runTest {
        val espotiZeroconfServer = EspotiZeroconfServer()
        val port = espotiZeroconfServer.initAndGetPort()
        backgroundScope.launch { espotiZeroconfServer.listen() }

        val localhostSocket = testClientSocket("localhost", port)
        assertEquals(localhostSocket.remoteAddress.toJavaAddress().port, port)
    }

    @Test
    fun `connect to loopback interface and verify connection`() = runTest {
        val espotiZeroconfServer = EspotiZeroconfServer()
        val port = espotiZeroconfServer.initAndGetPort()
        backgroundScope.launch { espotiZeroconfServer.listen() }

        val loopbackSocket = testClientSocket("127.0.0.1", port)
        assertEquals(loopbackSocket.remoteAddress.toJavaAddress().port, port)
    }

    @Test
    fun clientOnConnectWithGetRequest_verifyGetInfoIsCalled() = runTest {
        val clientSocket = mockk<Socket>()
        val inputStream = mockk<DataInputStream>()
        val outputStream = mockk<OutputStream>()
        val handler = spyk(EspotiConnectHandler())

        every { clientSocket.getInputStream() } returns inputStream
        every { clientSocket.getOutputStream() } returns outputStream
        every { inputStream.read() } returnsMany (
            "GET /?action=getInfo HTTP/1.1\r\n".toByteArray().map { it.toInt() } +
                "Accept-Encoding: gzip\r\n".toByteArray().map { it.toInt() } +
                "Connection: keep-alive\r\n".toByteArray().map { it.toInt() } +
                "Content-Type: application/x-www-form-urlencoded\r\n".toByteArray().map {
                    it.toInt()
                } +
                "Keep-Alive: 0\r\n".toByteArray().map { it.toInt() } +
                "Host: 0.0.0.0:1234\r\n".toByteArray().map { it.toInt() } +
                "User-Agent: Spotify/0.0.0.1234 Android/35 (Pixel 3a)\r\n\r\n".toByteArray().map {
                    it.toInt()
                }
            )
        every { outputStream.write(any<ByteArray>()) } just Runs
        every { outputStream.flush() } just Runs

        handler.onConnect(clientSocket)

        verify { handler.handleGetInfo(outputStream, "HTTP/1.1") }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun clientOnConnect_throwsException_isHandled() = runTest {
        val handler = mockk<EspotiConnectHandler>()

        every { runBlocking { handler.onConnect(any()) } } throws Exception("Test exception")

        val espotiZeroconfServer = EspotiZeroconfServer(espotiConnectHandler = handler)
        val port = espotiZeroconfServer.initAndGetPort()
        backgroundScope.launch { espotiZeroconfServer.listen() }

        // launch 10 different test client connections, ensure the exception is handled
        repeat(10) {
            testClientSocket("localhost", port)
        }
    }
*/
}
