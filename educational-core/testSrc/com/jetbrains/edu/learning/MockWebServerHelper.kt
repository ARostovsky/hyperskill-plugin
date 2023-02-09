package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals

typealias ResponseHandler = (RecordedRequest, String) -> MockResponse?

class MockWebServerHelper(parentDisposable: Disposable) {

  private val handlers = mutableSetOf<ResponseHandler>()
  val webSocketMockSever = MockWebServer()

  private val mockWebServer = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if (expectEduToolsUserAgent(request)) {
          assertEquals(eduToolsUserAgent, request.getHeader(USER_AGENT))
        }
        val path = request.path ?: error("Request path should not be null. Probably, `requestLine` is empty")
        for (handler in handlers) {
          val response = handler(request, path)
          if (response != null) return response
        }
        return MockResponseFactory.notFound()
      }
    }
  }

  init {
    Disposer.register(parentDisposable, Disposable { mockWebServer.shutdown() })
    Disposer.register(parentDisposable, Disposable { webSocketMockSever.shutdown() })
    longRunningThreadCreated(parentDisposable, "MockWebServer", "OkHttp TaskRunner", "Okio Watchdog")
  }

  val baseUrl: String get() = mockWebServer.url("/").toString()

  fun addResponseHandler(disposable: Disposable, handler: ResponseHandler) {
    handlers += handler
    Disposer.register(disposable, Disposable { handlers -= handler })
  }

  // DownloadUtil.downloadAtomically(), used in com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector.loadCourseStructure(),
  // sets product name as user agent, so such requests are not expected to contain eduToolsUserAgent
  private fun expectEduToolsUserAgent(request: RecordedRequest): Boolean = !request.pathWithoutPrams.contains("plugin")
}

fun RecordedRequest.hasParams(vararg params: Pair<String, String>): Boolean {
  val url = requestUrl ?: return false
  return params.all { param -> url.queryParameter(param.first) == param.second }
}

val RecordedRequest.pathWithoutPrams: String
  get() {
    val requestUrl = requestUrl ?: error("Request url should not be null. Probably, `requestLine` is empty")
    return requestUrl.toUrl().path
  }
