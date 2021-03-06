/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.test.core;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Headers;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.Registration;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.RequestOptionsBase;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.impl.HeadersAdaptor;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.WorkerContext;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.CaOptions;
import io.vertx.core.net.ClientOptions;
import io.vertx.core.net.JKSOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetServerOptionsBase;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PKCS12Options;
import io.vertx.core.net.TCPOptions;
import io.vertx.core.net.TrustStoreOptions;
import io.vertx.core.net.impl.SocketDefaults;
import io.vertx.core.streams.Pump;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 */
public class HttpTest extends HttpTestBase {

  private File testDir;

  public void setUp() throws Exception {
    super.setUp();
    testDir = Files.createTempDirectory("vertx-test").toFile();
    testDir.deleteOnExit();
    server = vertx.createHttpServer(HttpServerOptions.options().setPort(DEFAULT_HTTP_PORT).setHost(DEFAULT_HTTP_HOST));
    client = vertx.createHttpClient(HttpClientOptions.options());
  }

  @Test
  public void testClientOptions() {
    HttpClientOptions options = HttpClientOptions.options();

    assertEquals(-1, options.getSendBufferSize());
    int rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setSendBufferSize(rand));
    assertEquals(rand, options.getSendBufferSize());
    try {
      options.setSendBufferSize(0);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setSendBufferSize(-123);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertEquals(-1, options.getReceiveBufferSize());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setReceiveBufferSize(rand));
    assertEquals(rand, options.getReceiveBufferSize());
    try {
      options.setReceiveBufferSize(0);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setReceiveBufferSize(-123);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertTrue(options.isReuseAddress());
    assertEquals(options, options.setReuseAddress(false));
    assertFalse(options.isReuseAddress());

    assertEquals(-1, options.getTrafficClass());
    rand = 23;
    assertEquals(options, options.setTrafficClass(rand));
    assertEquals(rand, options.getTrafficClass());
    try {
      options.setTrafficClass(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setTrafficClass(256);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertTrue(options.isTcpNoDelay());
    assertEquals(options, options.setTcpNoDelay(false));
    assertFalse(options.isTcpNoDelay());

    boolean tcpKeepAlive = SocketDefaults.instance.isTcpKeepAlive();
    assertEquals(tcpKeepAlive, options.isTcpKeepAlive());
    assertEquals(options, options.setTcpKeepAlive(!tcpKeepAlive));
    assertEquals(!tcpKeepAlive, options.isTcpKeepAlive());

    int soLinger = SocketDefaults.instance.getSoLinger();
    assertEquals(soLinger, options.getSoLinger());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setSoLinger(rand));
    assertEquals(rand, options.getSoLinger());
    try {
      options.setSoLinger(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertFalse(options.isUsePooledBuffers());
    assertEquals(options, options.setUsePooledBuffers(true));
    assertTrue(options.isUsePooledBuffers());

    assertEquals(0, options.getIdleTimeout());
    assertEquals(options, options.setIdleTimeout(10));
    assertEquals(10, options.getIdleTimeout());
    try {
      options.setIdleTimeout(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertFalse(options.isSsl());
    assertEquals(options, options.setSsl(true));
    assertTrue(options.isSsl());

    assertNull(options.getKeyStoreOptions());
    JKSOptions keyStoreOptions = JKSOptions.options().setPath(TestUtils.randomAlphaString(100)).setPassword(TestUtils.randomAlphaString(100));
    assertEquals(options, options.setKeyStoreOptions(keyStoreOptions));
    assertEquals(keyStoreOptions, options.getKeyStoreOptions());

    assertNull(options.getTrustStoreOptions());
    JKSOptions trustStoreOptions = JKSOptions.options().setPath(TestUtils.randomAlphaString(100)).setPassword(TestUtils.randomAlphaString(100));
    assertEquals(options, options.setTrustStoreOptions(trustStoreOptions));
    assertEquals(trustStoreOptions, options.getTrustStoreOptions());

    assertFalse(options.isTrustAll());
    assertEquals(options, options.setTrustAll(true));
    assertTrue(options.isTrustAll());

    assertTrue(options.isVerifyHost());
    assertEquals(options, options.setVerifyHost(false));
    assertFalse(options.isVerifyHost());

    assertEquals(5, options.getMaxPoolSize());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setMaxPoolSize(rand));
    assertEquals(rand, options.getMaxPoolSize());
    try {
      options.setMaxPoolSize(0);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setMaxPoolSize(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertTrue(options.isKeepAlive());
    assertEquals(options, options.setKeepAlive(false));
    assertFalse(options.isKeepAlive());

    assertFalse(options.isPipelining());
    assertEquals(options, options.setPipelining(true));
    assertTrue(options.isPipelining());

    assertEquals(60000, options.getConnectTimeout());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setConnectTimeout(rand));
    assertEquals(rand, options.getConnectTimeout());
    try {
      options.setConnectTimeout(-2);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertFalse(options.isTryUseCompression());
    assertEquals(options, options.setTryUseCompression(true));
    assertEquals(true, options.isTryUseCompression());

    assertTrue(options.getEnabledCipherSuites().isEmpty());
    assertEquals(options, options.addEnabledCipherSuite("foo"));
    assertEquals(options, options.addEnabledCipherSuite("bar"));
    assertNotNull(options.getEnabledCipherSuites());
    assertTrue(options.getEnabledCipherSuites().contains("foo"));
    assertTrue(options.getEnabledCipherSuites().contains("bar"));

    testComplete();
  }


  @Test
  public void testServerOptions() {
    HttpServerOptions options = HttpServerOptions.options();

    assertEquals(-1, options.getSendBufferSize());
    int rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setSendBufferSize(rand));
    assertEquals(rand, options.getSendBufferSize());
    try {
      options.setSendBufferSize(0);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setSendBufferSize(-123);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertEquals(-1, options.getReceiveBufferSize());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setReceiveBufferSize(rand));
    assertEquals(rand, options.getReceiveBufferSize());
    try {
      options.setReceiveBufferSize(0);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setReceiveBufferSize(-123);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertTrue(options.isReuseAddress());
    assertEquals(options, options.setReuseAddress(false));
    assertFalse(options.isReuseAddress());

    assertEquals(-1, options.getTrafficClass());
    rand = 23;
    assertEquals(options, options.setTrafficClass(rand));
    assertEquals(rand, options.getTrafficClass());
    try {
      options.setTrafficClass(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setTrafficClass(256);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertTrue(options.isTcpNoDelay());
    assertEquals(options, options.setTcpNoDelay(false));
    assertFalse(options.isTcpNoDelay());

    boolean tcpKeepAlive = SocketDefaults.instance.isTcpKeepAlive();
    assertEquals(tcpKeepAlive, options.isTcpKeepAlive());
    assertEquals(options, options.setTcpKeepAlive(!tcpKeepAlive));
    assertEquals(!tcpKeepAlive, options.isTcpKeepAlive());

    int soLinger = SocketDefaults.instance.getSoLinger();
    assertEquals(soLinger, options.getSoLinger());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setSoLinger(rand));
    assertEquals(rand, options.getSoLinger());
    try {
      options.setSoLinger(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertFalse(options.isUsePooledBuffers());
    assertEquals(options, options.setUsePooledBuffers(true));
    assertTrue(options.isUsePooledBuffers());

    assertEquals(0, options.getIdleTimeout());
    assertEquals(options, options.setIdleTimeout(10));
    assertEquals(10, options.getIdleTimeout());
    try {
      options.setIdleTimeout(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertFalse(options.isSsl());
    assertEquals(options, options.setSsl(true));
    assertTrue(options.isSsl());

    assertNull(options.getKeyStoreOptions());
    JKSOptions keyStoreOptions = JKSOptions.options().setPath(TestUtils.randomAlphaString(100)).setPassword(TestUtils.randomAlphaString(100));
    assertEquals(options, options.setKeyStoreOptions(keyStoreOptions));
    assertEquals(keyStoreOptions, options.getKeyStoreOptions());

    assertNull(options.getTrustStoreOptions());
    JKSOptions trustStoreOptions = JKSOptions.options().setPath(TestUtils.randomAlphaString(100)).setPassword(TestUtils.randomAlphaString(100));
    assertEquals(options, options.setTrustStoreOptions(trustStoreOptions));
    assertEquals(trustStoreOptions, options.getTrustStoreOptions());

    assertEquals(1024, options.getAcceptBacklog());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setAcceptBacklog(rand));
    assertEquals(rand, options.getAcceptBacklog());

    assertFalse(options.isCompressionSupported());
    assertEquals(options, options.setCompressionSupported(true));
    assertTrue(options.isCompressionSupported());

    assertEquals(65536, options.getMaxWebsocketFrameSize());
    rand = TestUtils.randomPositiveInt();
    assertEquals(options, options.setMaxWebsocketFrameSize(rand));
    assertEquals(rand, options.getMaxWebsocketFrameSize());

    assertEquals(80, options.getPort());
    assertEquals(options, options.setPort(1234));
    assertEquals(1234, options.getPort());
    try {
      options.setPort(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setPort(65536);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertEquals("0.0.0.0", options.getHost());
    String randString = TestUtils.randomUnicodeString(100);
    assertEquals(options, options.setHost(randString));
    assertEquals(randString, options.getHost());

    assertTrue(options.getWebsocketSubProtocols().isEmpty());
    assertEquals(options, options.addWebsocketSubProtocol("foo"));
    assertEquals(options, options.addWebsocketSubProtocol("bar"));
    assertNotNull(options.getWebsocketSubProtocols());
    assertTrue(options.getWebsocketSubProtocols().contains("foo"));
    assertTrue(options.getWebsocketSubProtocols().contains("bar"));

    assertTrue(options.getEnabledCipherSuites().isEmpty());
    assertEquals(options, options.addEnabledCipherSuite("foo"));
    assertEquals(options, options.addEnabledCipherSuite("bar"));
    assertNotNull(options.getEnabledCipherSuites());
    assertTrue(options.getEnabledCipherSuites().contains("foo"));
    assertTrue(options.getEnabledCipherSuites().contains("bar"));


    testComplete();
  }

  @Test
  public void testCopyClientOptions() {
    HttpClientOptions options = HttpClientOptions.options();
    int sendBufferSize = TestUtils.randomPositiveInt();
    int receiverBufferSize = TestUtils.randomPortInt();
    Random rand = new Random();
    boolean reuseAddress = rand.nextBoolean();
    int trafficClass = TestUtils.randomByte() + 127;
    boolean tcpNoDelay = rand.nextBoolean();
    boolean tcpKeepAlive = rand.nextBoolean();
    int soLinger = TestUtils.randomPositiveInt();
    boolean usePooledBuffers = rand.nextBoolean();
    int idleTimeout = TestUtils.randomPositiveInt();
    boolean ssl = rand.nextBoolean();
    JKSOptions keyStoreOptions = JKSOptions.options();
    String ksPassword = TestUtils.randomAlphaString(100);
    keyStoreOptions.setPassword(ksPassword);
    JKSOptions trustStoreOptions = JKSOptions.options();
    String tsPassword = TestUtils.randomAlphaString(100);
    trustStoreOptions.setPassword(tsPassword);
    String enabledCipher = TestUtils.randomAlphaString(100);
    int connectTimeout = TestUtils.randomPositiveInt();
    boolean trustAll = rand.nextBoolean();
    String crlPath = TestUtils.randomUnicodeString(100);
    Buffer crlValue = TestUtils.randomBuffer(100);

    boolean verifyHost = rand.nextBoolean();
    int maxPoolSize = TestUtils.randomPositiveInt();
    boolean keepAlive = rand.nextBoolean();
    boolean pipelining = rand.nextBoolean();
    boolean tryUseCompression = rand.nextBoolean();

    options.setSendBufferSize(sendBufferSize);
    options.setReceiveBufferSize(receiverBufferSize);
    options.setReuseAddress(reuseAddress);
    options.setTrafficClass(trafficClass);
    options.setSsl(ssl);
    options.setTcpNoDelay(tcpNoDelay);
    options.setTcpKeepAlive(tcpKeepAlive);
    options.setSoLinger(soLinger);
    options.setUsePooledBuffers(usePooledBuffers);
    options.setIdleTimeout(idleTimeout);
    options.setKeyStoreOptions(keyStoreOptions);
    options.setTrustStoreOptions(trustStoreOptions);
    options.addEnabledCipherSuite(enabledCipher);
    options.setConnectTimeout(connectTimeout);
    options.setTrustAll(trustAll);
    options.addCrlPath(crlPath);
    options.addCrlValue(crlValue);
    options.setVerifyHost(verifyHost);
    options.setMaxPoolSize(maxPoolSize);
    options.setKeepAlive(keepAlive);
    options.setPipelining(pipelining);
    options.setTryUseCompression(tryUseCompression);
    HttpClientOptions copy = HttpClientOptions.copiedOptions(options);
    assertEquals(sendBufferSize, copy.getSendBufferSize());
    assertEquals(receiverBufferSize, copy.getReceiveBufferSize());
    assertEquals(reuseAddress, copy.isReuseAddress());
    assertEquals(trafficClass, copy.getTrafficClass());
    assertEquals(tcpNoDelay, copy.isTcpNoDelay());
    assertEquals(tcpKeepAlive, copy.isTcpKeepAlive());
    assertEquals(soLinger, copy.getSoLinger());
    assertEquals(usePooledBuffers, copy.isUsePooledBuffers());
    assertEquals(idleTimeout, copy.getIdleTimeout());
    assertEquals(ssl, copy.isSsl());
    assertNotSame(keyStoreOptions, copy.getKeyStoreOptions());
    assertEquals(ksPassword, ((JKSOptions) copy.getKeyStoreOptions()).getPassword());
    assertNotSame(trustStoreOptions, copy.getTrustStoreOptions());
    assertEquals(tsPassword, ((JKSOptions)copy.getTrustStoreOptions()).getPassword());
    assertEquals(1, copy.getEnabledCipherSuites().size());
    assertTrue(copy.getEnabledCipherSuites().contains(enabledCipher));
    assertEquals(connectTimeout, copy.getConnectTimeout());
    assertEquals(trustAll, copy.isTrustAll());
    assertEquals(1, copy.getCrlPaths().size());
    assertEquals(crlPath, copy.getCrlPaths().get(0));
    assertEquals(1, copy.getCrlValues().size());
    assertEquals(crlValue, copy.getCrlValues().get(0));
    assertEquals(verifyHost, copy.isVerifyHost());
    assertEquals(maxPoolSize, copy.getMaxPoolSize());
    assertEquals(keepAlive, copy.isKeepAlive());
    assertEquals(pipelining, copy.isPipelining());
    assertEquals(tryUseCompression, copy.isTryUseCompression());
  }

  @Test
  public void testDefaultClientOptionsJson() {
    HttpClientOptions def = HttpClientOptions.options();
    HttpClientOptions json = HttpClientOptions.optionsFromJson(new JsonObject());
    assertEquals(def.getMaxPoolSize(), json.getMaxPoolSize());
    assertEquals(def.isKeepAlive(), json.isKeepAlive());
    assertEquals(def.isPipelining(), json.isPipelining());
    assertEquals(def.isVerifyHost(), json.isVerifyHost());
    assertEquals(def.isTryUseCompression(), json.isTryUseCompression());
    testDefaultClientOptions(def, json);
  }

  @Test
  public void testClientOptionsJson() {
    int sendBufferSize = TestUtils.randomPositiveInt();
    int receiverBufferSize = TestUtils.randomPortInt();
    Random rand = new Random();
    boolean reuseAddress = rand.nextBoolean();
    int trafficClass = TestUtils.randomByte() + 127;
    boolean tcpNoDelay = rand.nextBoolean();
    boolean tcpKeepAlive = rand.nextBoolean();
    int soLinger = TestUtils.randomPositiveInt();
    boolean usePooledBuffers = rand.nextBoolean();
    int idleTimeout = TestUtils.randomPositiveInt();
    boolean ssl = rand.nextBoolean();
    JKSOptions keyStoreOptions = JKSOptions.options();
    String ksPassword = TestUtils.randomAlphaString(100);
    keyStoreOptions.setPassword(ksPassword);
    String ksPath = TestUtils.randomAlphaString(100);
    keyStoreOptions.setPath(ksPath);
    JKSOptions trustStoreOptions = JKSOptions.options();
    String tsPassword = TestUtils.randomAlphaString(100);
    trustStoreOptions.setPassword(tsPassword);
    String tsPath = TestUtils.randomAlphaString(100);
    trustStoreOptions.setPath(tsPath);
    String enabledCipher = TestUtils.randomAlphaString(100);
    int connectTimeout = TestUtils.randomPositiveInt();
    boolean trustAll = rand.nextBoolean();
    String crlPath = TestUtils.randomUnicodeString(100);
    boolean verifyHost = rand.nextBoolean();
    int maxPoolSize = TestUtils.randomPositiveInt();
    boolean keepAlive = rand.nextBoolean();
    boolean pipelining = rand.nextBoolean();
    boolean tryUseCompression = rand.nextBoolean();

    JsonObject json = new JsonObject();
    json.putNumber("sendBufferSize", sendBufferSize)
      .putNumber("receiveBufferSize", receiverBufferSize)
      .putBoolean("reuseAddress", reuseAddress)
      .putNumber("trafficClass", trafficClass)
      .putBoolean("tcpNoDelay", tcpNoDelay)
      .putBoolean("tcpKeepAlive", tcpKeepAlive)
      .putNumber("soLinger", soLinger)
      .putBoolean("usePooledBuffers", usePooledBuffers)
      .putNumber("idleTimeout", idleTimeout)
      .putBoolean("ssl", ssl)
      .putArray("enabledCipherSuites", new JsonArray().addString(enabledCipher))
      .putNumber("connectTimeout", connectTimeout)
      .putBoolean("trustAll", trustAll)
      .putArray("crlPaths", new JsonArray().addString(crlPath))
      .putObject("keyStoreOptions", new JsonObject().putString("type", "jks").putString("password", ksPassword).putString("path", ksPath))
      .putObject("trustStoreOptions", new JsonObject().putString("type", "jks").putString("password", tsPassword).putString("path", tsPath))
      .putBoolean("verifyHost", verifyHost)
      .putNumber("maxPoolSize", maxPoolSize)
      .putBoolean("keepAlive", keepAlive)
      .putBoolean("pipelining", pipelining)
      .putBoolean("tryUseCompression", tryUseCompression);

    HttpClientOptions options = HttpClientOptions.optionsFromJson(json);
    assertEquals(sendBufferSize, options.getSendBufferSize());
    assertEquals(receiverBufferSize, options.getReceiveBufferSize());
    assertEquals(reuseAddress, options.isReuseAddress());
    assertEquals(trafficClass, options.getTrafficClass());
    assertEquals(tcpKeepAlive, options.isTcpKeepAlive());
    assertEquals(tcpNoDelay, options.isTcpNoDelay());
    assertEquals(soLinger, options.getSoLinger());
    assertEquals(usePooledBuffers, options.isUsePooledBuffers());
    assertEquals(idleTimeout, options.getIdleTimeout());
    assertEquals(ssl, options.isSsl());
    assertNotSame(keyStoreOptions, options.getKeyStoreOptions());
    assertEquals(ksPassword, ((JKSOptions) options.getKeyStoreOptions()).getPassword());
    assertEquals(ksPath, ((JKSOptions) options.getKeyStoreOptions()).getPath());
    assertNotSame(trustStoreOptions, options.getTrustStoreOptions());
    assertEquals(tsPassword, ((JKSOptions) options.getTrustStoreOptions()).getPassword());
    assertEquals(tsPath, ((JKSOptions) options.getTrustStoreOptions()).getPath());
    assertEquals(1, options.getEnabledCipherSuites().size());
    assertTrue(options.getEnabledCipherSuites().contains(enabledCipher));
    assertEquals(connectTimeout, options.getConnectTimeout());
    assertEquals(trustAll, options.isTrustAll());
    assertEquals(1, options.getCrlPaths().size());
    assertEquals(crlPath, options.getCrlPaths().get(0));
    assertEquals(verifyHost, options.isVerifyHost());
    assertEquals(maxPoolSize, options.getMaxPoolSize());
    assertEquals(keepAlive, options.isKeepAlive());
    assertEquals(pipelining, options.isPipelining());
    assertEquals(tryUseCompression, options.isTryUseCompression());

    // Test other keystore/truststore types
    json.putObject("keyStoreOptions", new JsonObject().putString("type", "pkcs12").putString("password", ksPassword))
      .putObject("trustStoreOptions", new JsonObject().putString("type", "pkcs12").putString("password", tsPassword));
    options = HttpClientOptions.optionsFromJson(json);
    assertTrue(options.getTrustStoreOptions() instanceof PKCS12Options);
    assertTrue(options.getKeyStoreOptions() instanceof PKCS12Options);

    json.putObject("keyStoreOptions", new JsonObject().putString("type", "keyCert"))
      .putObject("trustStoreOptions", new JsonObject().putString("type", "ca"));
    options = HttpClientOptions.optionsFromJson(json);
    assertTrue(options.getTrustStoreOptions() instanceof CaOptions);
    assertTrue(options.getKeyStoreOptions() instanceof KeyCertOptions);


    // Invalid types
    json.putObject("keyStoreOptions", new JsonObject().putString("type", "foo"));
    try {
      HttpClientOptions.optionsFromJson(json);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    json.putObject("trustStoreOptions", new JsonObject().putString("type", "foo"));
    try {
      HttpClientOptions.optionsFromJson(json);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
  }

  @Test
  public void testCopyServerOptions() {
    HttpServerOptions options = HttpServerOptions.options();
    int sendBufferSize = TestUtils.randomPositiveInt();
    int receiverBufferSize = TestUtils.randomPortInt();
    Random rand = new Random();
    boolean reuseAddress = rand.nextBoolean();
    int trafficClass = TestUtils.randomByte() + 127;boolean tcpNoDelay = rand.nextBoolean();
    boolean tcpKeepAlive = rand.nextBoolean();
    int soLinger = TestUtils.randomPositiveInt();
    boolean usePooledBuffers = rand.nextBoolean();
    int idleTimeout = TestUtils.randomPositiveInt();
    boolean ssl = rand.nextBoolean();
    JKSOptions keyStoreOptions = JKSOptions.options();
    String ksPassword = TestUtils.randomAlphaString(100);
    keyStoreOptions.setPassword(ksPassword);
    JKSOptions trustStoreOptions = JKSOptions.options();
    String tsPassword = TestUtils.randomAlphaString(100);
    trustStoreOptions.setPassword(tsPassword);
    String enabledCipher = TestUtils.randomAlphaString(100);
    String crlPath = TestUtils.randomUnicodeString(100);
    Buffer crlValue = TestUtils.randomBuffer(100);
    int port = 1234;
    String host = TestUtils.randomAlphaString(100);
    int acceptBacklog = TestUtils.randomPortInt();
    boolean compressionSupported = rand.nextBoolean();
    int maxWebsocketFrameSize = TestUtils.randomPositiveInt();
    String wsSubProtocol = TestUtils.randomAlphaString(10);
    options.setSendBufferSize(sendBufferSize);
    options.setReceiveBufferSize(receiverBufferSize);
    options.setReuseAddress(reuseAddress);
    options.setTrafficClass(trafficClass);
    options.setTcpNoDelay(tcpNoDelay);
    options.setTcpKeepAlive(tcpKeepAlive);
    options.setSoLinger(soLinger);
    options.setUsePooledBuffers(usePooledBuffers);
    options.setIdleTimeout(idleTimeout);
    options.setSsl(ssl);
    options.setKeyStoreOptions(keyStoreOptions);
    options.setTrustStoreOptions(trustStoreOptions);
    options.addEnabledCipherSuite(enabledCipher);
    options.addCrlPath(crlPath);
    options.addCrlValue(crlValue);
    options.setPort(port);
    options.setHost(host);
    options.setAcceptBacklog(acceptBacklog);
    options.setCompressionSupported(compressionSupported);
    options.setMaxWebsocketFrameSize(maxWebsocketFrameSize);
    options.addWebsocketSubProtocol(wsSubProtocol);
    HttpServerOptions copy = HttpServerOptions.copiedOptions(options);
    assertEquals(sendBufferSize, copy.getSendBufferSize());
    assertEquals(receiverBufferSize, copy.getReceiveBufferSize());
    assertEquals(reuseAddress, copy.isReuseAddress());
    assertEquals(trafficClass, copy.getTrafficClass());
    assertEquals(tcpNoDelay, copy.isTcpNoDelay());
    assertEquals(tcpKeepAlive, copy.isTcpKeepAlive());
    assertEquals(soLinger, copy.getSoLinger());
    assertEquals(usePooledBuffers, copy.isUsePooledBuffers());
    assertEquals(idleTimeout, copy.getIdleTimeout());
    assertEquals(ssl, copy.isSsl());
    assertNotSame(keyStoreOptions, copy.getKeyStoreOptions());
    assertEquals(ksPassword, ((JKSOptions) copy.getKeyStoreOptions()).getPassword());
    assertNotSame(trustStoreOptions, copy.getTrustStoreOptions());
    assertEquals(tsPassword, ((JKSOptions)copy.getTrustStoreOptions()).getPassword());
    assertEquals(1, copy.getEnabledCipherSuites().size());
    assertTrue(copy.getEnabledCipherSuites().contains(enabledCipher));
    assertEquals(1, copy.getCrlPaths().size());
    assertEquals(crlPath, copy.getCrlPaths().get(0));
    assertEquals(1, copy.getCrlValues().size());
    assertEquals(crlValue, copy.getCrlValues().get(0));
    assertEquals(port, copy.getPort());
    assertEquals(host, copy.getHost());
    assertEquals(acceptBacklog, copy.getAcceptBacklog());
    assertEquals(compressionSupported, copy.isCompressionSupported());
    assertEquals(maxWebsocketFrameSize, options.getMaxWebsocketFrameSize());
    assertTrue(options.getWebsocketSubProtocols().contains(wsSubProtocol));
  }

  @Test
  public void testDefaultServerOptionsJson() {
    HttpServerOptions def = HttpServerOptions.options();
    HttpServerOptions json = HttpServerOptions.optionsFromJson(new JsonObject());
    assertEquals(def.getMaxWebsocketFrameSize(), json.getMaxWebsocketFrameSize());
    assertEquals(def.getWebsocketSubProtocols(), json.getWebsocketSubProtocols());
    assertEquals(def.isCompressionSupported(), json.isCompressionSupported());
    testDefaultNetServerOptionsBase(def, json);
  }

  @Test
  public void testServerOptionsJson() {
    int sendBufferSize = TestUtils.randomPositiveInt();
    int receiverBufferSize = TestUtils.randomPortInt();
    Random rand = new Random();
    boolean reuseAddress = rand.nextBoolean();
    int trafficClass = TestUtils.randomByte() + 127;
    boolean tcpNoDelay = rand.nextBoolean();
    boolean tcpKeepAlive = rand.nextBoolean();
    int soLinger = TestUtils.randomPositiveInt();
    boolean usePooledBuffers = rand.nextBoolean();
    int idleTimeout = TestUtils.randomPositiveInt();
    boolean ssl = rand.nextBoolean();
    JKSOptions keyStoreOptions = JKSOptions.options();
    String ksPassword = TestUtils.randomAlphaString(100);
    keyStoreOptions.setPassword(ksPassword);
    String ksPath = TestUtils.randomAlphaString(100);
    keyStoreOptions.setPath(ksPath);
    JKSOptions trustStoreOptions = JKSOptions.options();
    String tsPassword = TestUtils.randomAlphaString(100);
    trustStoreOptions.setPassword(tsPassword);
    String tsPath = TestUtils.randomAlphaString(100);
    trustStoreOptions.setPath(tsPath);
    String enabledCipher = TestUtils.randomAlphaString(100);
    String crlPath = TestUtils.randomUnicodeString(100);
    int port = 1234;
    String host = TestUtils.randomAlphaString(100);
    int acceptBacklog = TestUtils.randomPortInt();
    boolean compressionSupported = rand.nextBoolean();
    int maxWebsocketFrameSize = TestUtils.randomPositiveInt();
    String wsSubProtocol = TestUtils.randomAlphaString(10);

    JsonObject json = new JsonObject();
    json.putNumber("sendBufferSize", sendBufferSize)
      .putNumber("receiveBufferSize", receiverBufferSize)
      .putBoolean("reuseAddress", reuseAddress)
      .putNumber("trafficClass", trafficClass)
      .putBoolean("tcpNoDelay", tcpNoDelay)
      .putBoolean("tcpKeepAlive", tcpKeepAlive)
      .putNumber("soLinger", soLinger)
      .putBoolean("usePooledBuffers", usePooledBuffers)
      .putNumber("idleTimeout", idleTimeout)
      .putBoolean("ssl", ssl)
      .putArray("enabledCipherSuites", new JsonArray().addString(enabledCipher))
      .putArray("crlPaths", new JsonArray().addString(crlPath))
      .putObject("keyStoreOptions", new JsonObject().putString("type", "jks").putString("password", ksPassword).putString("path", ksPath))
      .putObject("trustStoreOptions", new JsonObject().putString("type", "jks").putString("password", tsPassword).putString("path", tsPath))
      .putNumber("port", port)
      .putString("host", host)
      .putNumber("acceptBacklog", acceptBacklog)
      .putBoolean("compressionSupported", compressionSupported)
      .putNumber("maxWebsocketFrameSize", maxWebsocketFrameSize)
      .putArray("websocketSubProtocols", new JsonArray().addString(wsSubProtocol));

    HttpServerOptions options = HttpServerOptions.optionsFromJson(json);
    assertEquals(sendBufferSize, options.getSendBufferSize());
    assertEquals(receiverBufferSize, options.getReceiveBufferSize());
    assertEquals(reuseAddress, options.isReuseAddress());
    assertEquals(trafficClass, options.getTrafficClass());
    assertEquals(tcpKeepAlive, options.isTcpKeepAlive());
    assertEquals(tcpNoDelay, options.isTcpNoDelay());
    assertEquals(soLinger, options.getSoLinger());
    assertEquals(usePooledBuffers, options.isUsePooledBuffers());
    assertEquals(idleTimeout, options.getIdleTimeout());
    assertEquals(ssl, options.isSsl());
    assertNotSame(keyStoreOptions, options.getKeyStoreOptions());
    assertEquals(ksPassword, ((JKSOptions) options.getKeyStoreOptions()).getPassword());
    assertEquals(ksPath, ((JKSOptions) options.getKeyStoreOptions()).getPath());
    assertNotSame(trustStoreOptions, options.getTrustStoreOptions());
    assertEquals(tsPassword, ((JKSOptions) options.getTrustStoreOptions()).getPassword());
    assertEquals(tsPath, ((JKSOptions) options.getTrustStoreOptions()).getPath());
    assertEquals(1, options.getEnabledCipherSuites().size());
    assertTrue(options.getEnabledCipherSuites().contains(enabledCipher));
    assertEquals(1, options.getCrlPaths().size());
    assertEquals(crlPath, options.getCrlPaths().get(0));
    assertEquals(port, options.getPort());
    assertEquals(host, options.getHost());
    assertEquals(acceptBacklog, options.getAcceptBacklog());
    assertEquals(compressionSupported, options.isCompressionSupported());
    assertEquals(maxWebsocketFrameSize, options.getMaxWebsocketFrameSize());
    assertTrue(options.getWebsocketSubProtocols().contains(wsSubProtocol));

    // Test other keystore/truststore types
    json.putObject("keyStoreOptions", new JsonObject().putString("type", "pkcs12").putString("password", ksPassword))
      .putObject("trustStoreOptions", new JsonObject().putString("type", "pkcs12").putString("password", tsPassword));
    options = HttpServerOptions.optionsFromJson(json);
    assertTrue(options.getTrustStoreOptions() instanceof PKCS12Options);
    assertTrue(options.getKeyStoreOptions() instanceof PKCS12Options);

    json.putObject("keyStoreOptions", new JsonObject().putString("type", "keyCert"))
      .putObject("trustStoreOptions", new JsonObject().putString("type", "ca"));
    options = HttpServerOptions.optionsFromJson(json);
    assertTrue(options.getTrustStoreOptions() instanceof CaOptions);
    assertTrue(options.getKeyStoreOptions() instanceof KeyCertOptions);


    // Invalid types
    json.putObject("keyStoreOptions", new JsonObject().putString("type", "foo"));
    try {
      HttpServerOptions.optionsFromJson(json);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    json.putObject("trustStoreOptions", new JsonObject().putString("type", "foo"));
    try {
      HttpServerOptions.optionsFromJson(json);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
  }

  @Test
  public void testServerChaining() {
    server.requestHandler(req -> {
      assertTrue(req.response().setChunked(true) == req.response());
      assertTrue(req.response().writeString("foo", "UTF-8") == req.response());
      assertTrue(req.response().writeString("foo") == req.response());
      testComplete();
    });

    server.listen(onSuccess(server -> {
      client.put(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler()).end();
    }));

    await();
  }

  @Test
  public void testServerChainingSendFile() throws Exception {
    File file = setupFile("test-server-chaining.dat", "blah");
    server.requestHandler(req -> {
      assertTrue(req.response().sendFile(file.getAbsolutePath()) == req.response());
      file.delete();
      testComplete();
    });

    server.listen(onSuccess(server -> {
      client.put(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler()).end();
    }));

    await();
  }

  @Test
  public void testClientChaining() {
    server.requestHandler(noOpHandler());

    server.listen(onSuccess(server -> {
      HttpClientRequest req = client.put(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler());
      assertTrue(req.setChunked(true) == req);
      assertTrue(req.sendHead() == req);
      assertTrue(req.writeString("foo", "UTF-8") == req);
      assertTrue(req.writeString("foo") == req);
      assertTrue(req.writeBuffer(Buffer.buffer("foo")) == req);
      testComplete();
    }));

    await();
  }

  @Test
  public void testLowerCaseHeaders() {
    server.requestHandler(req -> {
      assertEquals("foo", req.headers().get("Foo"));
      assertEquals("foo", req.headers().get("foo"));
      assertEquals("foo", req.headers().get("fOO"));
      assertTrue(req.headers().contains("Foo"));
      assertTrue(req.headers().contains("foo"));
      assertTrue(req.headers().contains("fOO"));

      req.response().putHeader("Quux", "quux");

      assertEquals("quux", req.response().headers().get("Quux"));
      assertEquals("quux", req.response().headers().get("quux"));
      assertEquals("quux", req.response().headers().get("qUUX"));
      assertTrue(req.response().headers().contains("Quux"));
      assertTrue(req.response().headers().contains("quux"));
      assertTrue(req.response().headers().contains("qUUX"));

      req.response().end();
    });

    server.listen(onSuccess(server -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        assertEquals("quux", resp.headers().get("Quux"));
        assertEquals("quux", resp.headers().get("quux"));
        assertEquals("quux", resp.headers().get("qUUX"));
        assertTrue(resp.headers().contains("Quux"));
        assertTrue(resp.headers().contains("quux"));
        assertTrue(resp.headers().contains("qUUX"));
        testComplete();
      });

      req.putHeader("Foo", "foo");
      assertEquals("foo", req.headers().get("Foo"));
      assertEquals("foo", req.headers().get("foo"));
      assertEquals("foo", req.headers().get("fOO"));
      assertTrue(req.headers().contains("Foo"));
      assertTrue(req.headers().contains("foo"));
      assertTrue(req.headers().contains("fOO"));

      req.end();
    }));

    await();
  }

  @Test
  public void testHeadersOnRequestOptions() {
    server.requestHandler(req -> {
      assertEquals("bar", req.headers().get("foo"));
      req.response().end();
    });
    server.listen(onSuccess(server -> {
      Headers headers = new CaseInsensitiveHeaders();
      headers.add("foo", "bar");
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI).setHeaders(headers), resp -> {
        assertEquals(200, resp.statusCode());
        testComplete();
      });
    }));
    await();
  }

  @Test
  public void testPutHeadersOnRequestOptions() {
    server.requestHandler(req -> {
      assertEquals("bar", req.headers().get("foo"));
      req.response().end();
    });
    server.listen(onSuccess(server -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI).addHeader("foo", "bar"), resp -> {
        assertEquals(200, resp.statusCode());
        testComplete();
      });
    }));
    await();
  }

  @Test
  public void testSimpleGET() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "GET", client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimplePUT() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "PUT", client.put(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimplePOST() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "POST", client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimpleDELETE() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "DELETE", client.delete(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimpleHEAD() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "HEAD", client.head(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimpleTRACE() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "TRACE", client.trace(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimpleCONNECT() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "CONNECT", client.connect(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimpleOPTIONS() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "OPTIONS", client.options(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimplePATCH() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "PATCH", client.patch(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete()));
  }

  @Test
  public void testSimpleGETNonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "GET", resp -> testComplete());
  }

  @Test
  public void testSimplePUTNonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "PUT", resp -> testComplete());
  }

  @Test
  public void testSimplePOSTNonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "POST", resp -> testComplete());
  }

  @Test
  public void testSimpleDELETENonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "DELETE", resp -> testComplete());
  }

  @Test
  public void testSimpleHEADNonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "HEAD", resp -> testComplete());
  }

  @Test
  public void testSimpleTRACENonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "TRACE", resp -> testComplete());
  }

  @Test
  public void testSimpleCONNECTNonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "CONNECT", resp -> testComplete());
  }

  @Test
  public void testSimpleOPTIONSNonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "OPTIONS", resp -> testComplete());
  }

  @Test
  public void testSimplePATCHNonSpecific() {
    String uri = "/some-uri?foo=bar";
    testSimpleRequest(uri, "PATCH", resp -> testComplete());
  }

  private void testSimpleRequest(String uri, String method, Handler<HttpClientResponse> handler) {
    testSimpleRequest(uri, method, client.request(method, RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), handler));
  }

  private void testSimpleRequest(String uri, String method, HttpClientRequest request) {
    String path = uri.indexOf('?') == -1 ? uri : uri.substring(0, uri.indexOf('?'));
    server.requestHandler(req -> {
      assertEquals(path, req.path());
      assertEquals(method, req.method());
      req.response().end();
    });

    server.listen(onSuccess(server -> request.end()));

    await();
  }

  @Test
  public void testAbsoluteURI() {
    testURIAndPath("http://localhost:" + DEFAULT_HTTP_PORT + "/this/is/a/path/foo.html", "/this/is/a/path/foo.html");
  }

  @Test
  public void testRelativeURI() {
    testURIAndPath("/this/is/a/path/foo.html", "/this/is/a/path/foo.html");
  }

  @Test
  public void testAbsoluteURIWithHttpSchemaInQuery() {
    testURIAndPath("http://localhost:" + DEFAULT_HTTP_PORT + "/correct/path?url=http://localhost:8008/wrong/path", "/correct/path");
  }

  @Test
  public void testRelativeURIWithHttpSchemaInQuery() {
    testURIAndPath("/correct/path?url=http://localhost:8008/wrong/path", "/correct/path");
  }

  @Test
  public void testAbsoluteURIEmptyPath() {
    testURIAndPath("http://localhost:" + DEFAULT_HTTP_PORT + "/", "/");
  }

  private void testURIAndPath(String uri, String path) {
    server.requestHandler(req -> {
      assertEquals(uri, req.uri());
      assertEquals(path, req.path());
      req.response().end();
    });

    server.listen(onSuccess(server -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(uri), resp -> testComplete());
    }));

    await();
  }

  @Test
  public void testParamsAmpersand() {
    testParams('&');
  }

  @Test
  public void testParamsSemiColon() {
    testParams(';');
  }

  private void testParams(char delim) {
    Map<String, String> params = genMap(10);
    String query = generateQueryString(params, delim);

    server.requestHandler(req -> {
      assertEquals(query, req.query());
      assertEquals(params.size(), req.params().size());
      for (Map.Entry<String, String> entry : req.params()) {
        assertEquals(entry.getValue(), params.get(entry.getKey()));
      }
      req.response().end();
    });

    server.listen(onSuccess(server -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("some-uri/?" + query), resp -> testComplete());
    }));

    await();
  }

  @Test
  public void testNoParams() {
    server.requestHandler(req -> {
      assertNull(req.query());
      assertTrue(req.params().isEmpty());
      req.response().end();
    });

    server.listen(onSuccess(server -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> testComplete());
    }));

    await();
  }

  @Test
  public void testDefaultRequestHeaders() {
    server.requestHandler(req -> {
      assertEquals(1, req.headers().size());
      assertEquals("localhost:" + DEFAULT_HTTP_PORT, req.headers().get("host"));
      req.response().end();
    });

    server.listen(onSuccess(server -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> testComplete());
    }));

    await();
  }

  @Test
  public void testRequestHeadersPutAll() {
    testRequestHeaders(false);
  }

  @Test
  public void testRequestHeadersIndividually() {
    testRequestHeaders(true);
  }

  private void testRequestHeaders(boolean individually) {
    Headers headers = getHeaders(10);

    server.requestHandler(req -> {
      assertEquals(headers.size() + 1, req.headers().size());
      for (Map.Entry<String, String> entry : headers) {
        assertEquals(entry.getValue(), req.headers().get(entry.getKey()));
      }
      req.response().end();
    });

    server.listen(onSuccess(server -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> testComplete());
      if (individually) {
        for (Map.Entry<String, String> header : headers) {
          req.headers().add(header.getKey(), header.getValue());
        }
      } else {
        req.headers().setAll(headers);
      }
      req.end();
    }));

    await();
  }

  @Test
  public void testResponseHeadersPutAll() {
    testResponseHeaders(false);
  }

  @Test
  public void testResponseHeadersIndividually() {
    testResponseHeaders(true);
  }

  private void testResponseHeaders(boolean individually) {
    Headers headers = getHeaders(10);

    server.requestHandler(req -> {
      if (individually) {
        for (Map.Entry<String, String> header : headers) {
          req.response().headers().add(header.getKey(), header.getValue());
        }
      } else {
        req.response().headers().setAll(headers);
      }
      req.response().end();
    });

    server.listen(onSuccess(server -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        assertEquals(headers.size() + 1, resp.headers().size());
        for (Map.Entry<String, String> entry : headers) {
          assertEquals(entry.getValue(), resp.headers().get(entry.getKey()));
        }
        testComplete();
      });
    }));

    await();
  }

  @Test
  public void testResponseMultipleSetCookieInHeader() {
    testResponseMultipleSetCookie(true, false);
  }

  @Test
  public void testResponseMultipleSetCookieInTrailer() {
    testResponseMultipleSetCookie(false, true);
  }

  @Test
  public void testResponseMultipleSetCookieInHeaderAndTrailer() {
    testResponseMultipleSetCookie(true, true);
  }

  private void testResponseMultipleSetCookie(boolean inHeader, boolean inTrailer) {
    List<String> cookies = new ArrayList<>();

    server.requestHandler(req -> {
      if (inHeader) {
        List<String> headers = new ArrayList<>();
        headers.add("h1=h1v1");
        headers.add("h2=h2v2; Expires=Wed, 09-Jun-2021 10:18:14 GMT");
        cookies.addAll(headers);
        req.response().headers().set("Set-Cookie", headers);
      }
      if (inTrailer) {
        req.response().setChunked(true);
        List<String> trailers = new ArrayList<>();
        trailers.add("t1=t1v1");
        trailers.add("t2=t2v2; Expires=Wed, 09-Jun-2021 10:18:14 GMT");
        cookies.addAll(trailers);
        req.response().trailers().set("Set-Cookie", trailers);
      }
      req.response().end();
    });

    server.listen(onSuccess(server -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.endHandler(v -> {
          assertEquals(cookies.size(), resp.cookies().size());
          for (int i = 0; i < cookies.size(); ++i) {
            assertEquals(cookies.get(i), resp.cookies().get(i));
          }
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void testUseRequestAfterComplete() {
    server.requestHandler(noOpHandler());

    server.listen(onSuccess(server -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler());
      req.end();

      Buffer buff = Buffer.buffer();
      try {
        req.end();
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.continueHandler(noOpHandler());
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.drainHandler(noOpHandler());
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeStringAndEnd("foo");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeBufferAndEnd(buff);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeStringAndEnd("foo", "UTF-8");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.exceptionHandler(noOpHandler());
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.sendHead();
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.setChunked(false);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.setWriteQueueMaxSize(123);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeBuffer(buff);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeString("foo");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeString("foo", "UTF-8");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeBuffer(buff);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        req.writeQueueFull();
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }

      testComplete();
    }));

    await();
  }

  @Test
  public void testRequestBodyBufferAtEnd() {
    Buffer body = TestUtils.randomBuffer(1000);
    server.requestHandler(req -> req.bodyHandler(buffer -> {
      assertEquals(body, buffer);
      req.response().end();
    }));

    server.listen(onSuccess(server -> {
      client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> testComplete()).writeBufferAndEnd(body);
    }));

    await();
  }

  @Test
  public void testRequestBodyStringDefaultEncodingAtEnd() {
    testRequestBodyStringAtEnd(null);
  }

  @Test
  public void testRequestBodyStringUTF8AtEnd() {
    testRequestBodyStringAtEnd("UTF-8");
  }

  @Test
  public void testRequestBodyStringUTF16AtEnd() {
    testRequestBodyStringAtEnd("UTF-16");
  }

  private void testRequestBodyStringAtEnd(String encoding) {
    String body = TestUtils.randomUnicodeString(1000);
    Buffer bodyBuff;

    if (encoding == null) {
      bodyBuff = Buffer.buffer(body);
    } else {
      bodyBuff = Buffer.buffer(body, encoding);
    }

    server.requestHandler(req -> {
      req.bodyHandler(buffer -> {
        assertEquals(bodyBuff, buffer);
        testComplete();
      });
    });

    server.listen(onSuccess(server -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler());
      if (encoding == null) {
        req.writeStringAndEnd(body);
      } else {
        req.writeStringAndEnd(body, encoding);
      }
    }));

    await();
  }

  @Test
  public void testRequestBodyWriteChunked() {
    testRequestBodyWrite(true);
  }

  @Test
  public void testRequestBodyWriteNonChunked() {
    testRequestBodyWrite(false);
  }

  private void testRequestBodyWrite(boolean chunked) {
    Buffer body = Buffer.buffer();

    server.requestHandler(req -> {
      req.bodyHandler(buffer -> {
        assertEquals(body, buffer);
        req.response().end();
      });
    });

    server.listen(onSuccess(server -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> testComplete());
      int numWrites = 10;
      int chunkSize = 100;

      if (chunked) {
        req.setChunked(true);
      } else {
        req.headers().set("Content-Length", String.valueOf(numWrites * chunkSize));
      }
      for (int i = 0; i < numWrites; i++) {
        Buffer b = TestUtils.randomBuffer(chunkSize);
        body.appendBuffer(b);
        req.writeBuffer(b);
      }
      req.end();
    }));

    await();
  }

  @Test
  public void testRequestBodyWriteStringChunkedDefaultEncoding() {
    testRequestBodyWriteString(true, null);
  }

  @Test
  public void testRequestBodyWriteStringChunkedUTF8() {
    testRequestBodyWriteString(true, "UTF-8");
  }

  @Test
  public void testRequestBodyWriteStringChunkedUTF16() {
    testRequestBodyWriteString(true, "UTF-16");
  }

  @Test
  public void testRequestBodyWriteStringNonChunkedDefaultEncoding() {
    testRequestBodyWriteString(false, null);
  }

  @Test
  public void testRequestBodyWriteStringNonChunkedUTF8() {
    testRequestBodyWriteString(false, "UTF-8");
  }

  @Test
  public void testRequestBodyWriteStringNonChunkedUTF16() {
    testRequestBodyWriteString(false, "UTF-16");
  }

  private void testRequestBodyWriteString(boolean chunked, String encoding) {
    String body = TestUtils.randomUnicodeString(1000);
    Buffer bodyBuff;

    if (encoding == null) {
      bodyBuff = Buffer.buffer(body);
    } else {
      bodyBuff = Buffer.buffer(body, encoding);
    }

    server.requestHandler(req -> {
      req.bodyHandler(buff -> {
        assertEquals(bodyBuff, buff);
        testComplete();
      });
    });

    server.listen(onSuccess(server -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler());

      if (chunked) {
        req.setChunked(true);
      } else {
        req.headers().set("Content-Length", String.valueOf(bodyBuff.length()));
      }

      if (encoding == null) {
        req.writeString(body);
      } else {
        req.writeString(body, encoding);
      }
      req.end();
    }));

    await();
  }

  @Test
  public void testRequestWrite() {
    Buffer body = TestUtils.randomBuffer(1000);

    server.requestHandler(req -> {
      req.bodyHandler(buff -> {
        assertEquals(body, buff);
        testComplete();
      });
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler());
      req.setChunked(true);
      req.writeBuffer(body);
      req.end();
    }));

    await();
  }

  @Test
  public void testDefaultStatus() {
    testStatusCode(-1, null);
  }

  @Test
  public void testDefaultOther() {
    // Doesn't really matter which one we choose
    testStatusCode(405, null);
  }

  @Test
  public void testOverrideStatusMessage() {
    testStatusCode(404, "some message");
  }

  @Test
  public void testOverrideDefaultStatusMessage() {
    testStatusCode(-1, "some other message");
  }

  private void testStatusCode(int code, String statusMessage) {
    server.requestHandler(req -> {
      if (code != -1) {
        req.response().setStatusCode(code);
      }
      if (statusMessage != null) {
        req.response().setStatusMessage(statusMessage);
      }
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        int theCode;
        if (code == -1) {
          // Default code - 200
          assertEquals(200, resp.statusCode());
          theCode = 200;
        } else {
          theCode = code;
        }
        if (statusMessage != null) {
          assertEquals(statusMessage, resp.statusMessage());
        } else {
          assertEquals(HttpResponseStatus.valueOf(theCode).reasonPhrase(), resp.statusMessage());
        }
        testComplete();
      });
    }));

    await();
  }

  @Test
  public void testResponseTrailersPutAll() {
    testResponseTrailers(false);
  }

  @Test
  public void testResponseTrailersPutIndividually() {
    testResponseTrailers(true);
  }

  private void testResponseTrailers(boolean individually) {
    Headers trailers = getHeaders(10);

    server.requestHandler(req -> {
      req.response().setChunked(true);
      if (individually) {
        for (Map.Entry<String, String> header : trailers) {
          req.response().trailers().add(header.getKey(), header.getValue());
        }
      } else {
        req.response().trailers().setAll(trailers);
      }
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.endHandler(v -> {
          assertEquals(trailers.size(), resp.trailers().size());
          for (Map.Entry<String, String> entry : trailers) {
            assertEquals(entry.getValue(), resp.trailers().get(entry.getKey()));
          }
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void testResponseNoTrailers() {
    server.requestHandler(req -> {
      req.response().setChunked(true);
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.endHandler(v -> {
          assertTrue(resp.trailers().isEmpty());
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void testUseResponseAfterComplete() {
    server.requestHandler(req -> {
      Buffer buff = Buffer.buffer();
      HttpServerResponse resp = req.response();
      resp.end();

      try {
        resp.drainHandler(noOpHandler());
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }

      try {
        resp.end();
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.writeStringAndEnd("foo");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.writeBufferAndEnd(buff);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.writeStringAndEnd("foo", "UTF-8");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.exceptionHandler(noOpHandler());
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.setChunked(false);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.setWriteQueueMaxSize(123);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.writeBuffer(buff);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.writeString("foo");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }
      try {
        resp.writeString("foo", "UTF-8");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }

      try {
        resp.writeBuffer(buff);
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }

      try {
        resp.writeQueueFull();
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }

      try {
        resp.sendFile("asokdasokd");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
      }

      testComplete();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler());
    }));

    await();
  }

  @Test
  public void testResponseBodyBufferAtEnd() {
    Buffer body = TestUtils.randomBuffer(1000);

    server.requestHandler(req -> {
      req.response().writeBufferAndEnd(body);
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.bodyHandler(buff -> {
          assertEquals(body, buff);
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void testResponseBodyStringDefaultEncodingAtEnd() {
    testResponseBodyStringAtEnd(null);
  }

  @Test
  public void testResponseBodyStringUTF8AtEnd() {
    testResponseBodyStringAtEnd("UTF-8");
  }

  @Test
  public void testResponseBodyStringUTF16AtEnd() {
    testResponseBodyStringAtEnd("UTF-16");
  }

  private void testResponseBodyStringAtEnd(String encoding) {
    String body = TestUtils.randomUnicodeString(1000);
    Buffer bodyBuff;

    if (encoding == null) {
      bodyBuff = Buffer.buffer(body);
    } else {
      bodyBuff = Buffer.buffer(body, encoding);
    }

    server.requestHandler(req -> {
      if (encoding == null) {
        req.response().writeStringAndEnd(body);
      } else {
        req.response().writeStringAndEnd(body, encoding);
      }
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.bodyHandler(buff -> {
          assertEquals(bodyBuff, buff);
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void testResponseBodyWriteStringNonChunked() {
    server.requestHandler(req -> {
      try {
        req.response().writeString("foo");
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //OK
        testComplete();
      }
    });

    server.listen(onSuccess(s -> {
      client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler()).end();
    }));

    await();
  }

  @Test
  public void testResponseBodyWriteChunked() {
    testResponseBodyWrite(true);
  }

  @Test
  public void testResponseBodyWriteNonChunked() {
    testResponseBodyWrite(false);
  }

  private void testResponseBodyWrite(boolean chunked) {
    Buffer body = Buffer.buffer();

    int numWrites = 10;
    int chunkSize = 100;

    server.requestHandler(req -> {
      if (chunked) {
        req.response().setChunked(true);
      } else {
        req.response().headers().set("Content-Length", String.valueOf(numWrites * chunkSize));
      }

      for (int i = 0; i < numWrites; i++) {
        Buffer b = TestUtils.randomBuffer(chunkSize);
        body.appendBuffer(b);
        req.response().writeBuffer(b);
      }
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.bodyHandler(buff -> {
          assertEquals(body, buff);
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void testResponseBodyWriteStringChunkedDefaultEncoding() {
    testResponseBodyWriteString(true, null);
  }

  @Test
  public void testResponseBodyWriteStringChunkedUTF8() {
    testResponseBodyWriteString(true, "UTF-8");
  }

  @Test
  public void testResponseBodyWriteStringChunkedUTF16() {
    testResponseBodyWriteString(true, "UTF-16");
  }

  @Test
  public void testResponseBodyWriteStringNonChunkedDefaultEncoding() {
    testResponseBodyWriteString(false, null);
  }

  @Test
  public void testResponseBodyWriteStringNonChunkedUTF8() {
    testResponseBodyWriteString(false, "UTF-8");
  }

  @Test
  public void testResponseBodyWriteStringNonChunkedUTF16() {
    testResponseBodyWriteString(false, "UTF-16");
  }

  private void testResponseBodyWriteString(boolean chunked, String encoding) {
    String body = TestUtils.randomUnicodeString(1000);
    Buffer bodyBuff;

    if (encoding == null) {
      bodyBuff = Buffer.buffer(body);
    } else {
      bodyBuff = Buffer.buffer(body, encoding);
    }

    server.requestHandler(req -> {
      if (chunked) {
        req.response().setChunked(true);
      } else {
        req.response().headers().set("Content-Length", String.valueOf(bodyBuff.length()));
      }
      if (encoding == null) {
        req.response().writeString(body);
      } else {
        req.response().writeString(body, encoding);
      }
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.bodyHandler(buff -> {
          assertEquals(bodyBuff, buff);
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void testResponseWrite() {
    Buffer body = TestUtils.randomBuffer(1000);

    server.requestHandler(req -> {
      req.response().setChunked(true);
      req.response().writeBuffer(body);
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.bodyHandler(buff -> {
          assertEquals(body, buff);
          testComplete();
        });
      }).end();
    }));

    await();
  }

  @Test
  public void testPipeliningOrder() throws Exception {
    client.close();
    client = vertx.createHttpClient(HttpClientOptions.options().setKeepAlive(true).setPipelining(true).setMaxPoolSize(1));
    int requests = 100;

    AtomicInteger reqCount = new AtomicInteger(0);
    server.requestHandler(req -> {
      int theCount = reqCount.get();
      assertEquals(theCount, Integer.parseInt(req.headers().get("count")));
      reqCount.incrementAndGet();
      req.response().setChunked(true);
      req.bodyHandler(buff -> {
        assertEquals("This is content " + theCount, buff.toString());
        // We write the response back after a random time to increase the chances of responses written in the
        // wrong order if we didn't implement pipelining correctly
        vertx.setTimer(1 + (long) (10 * Math.random()), id -> {
          req.response().headers().set("count", String.valueOf(theCount));
          req.response().writeBuffer(buff);
          req.response().end();
        });
      });
    });


    CountDownLatch latch = new CountDownLatch(requests);
    AtomicInteger cnt = new AtomicInteger(0);

    server.listen(onSuccess(s -> {
      vertx.setTimer(500, id -> {
        for (int count = 0; count < requests; count++) {
          int theCount = count;
          HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
            assertEquals(theCount, Integer.parseInt(resp.headers().get("count")));
            resp.bodyHandler(buff -> {
              assertEquals("This is content " + theCount, buff.toString());
              latch.countDown();
            });
          });
          req.setChunked(true);
          req.headers().set("count", String.valueOf(count));
          req.writeString("This is content " + count);
          req.end();
        }
      });

    }));

    awaitLatch(latch);

  }

  @Test
  public void testKeepAlive() throws Exception {
    testKeepAlive(true, 5, 10, 5);
  }

  @Test
  public void testNoKeepAlive() throws Exception {
    testKeepAlive(false, 5, 10, 10);
  }

  private void testKeepAlive(boolean keepAlive, int poolSize, int numServers, int expectedConnectedServers) throws Exception {
    client.close();
    server.close();
    client = vertx.createHttpClient(HttpClientOptions.options().setKeepAlive(keepAlive).setPipelining(false).setMaxPoolSize(poolSize));
    int requests = 100;

    // Start the servers
    HttpServer[] servers = new HttpServer[numServers];
    CountDownLatch startServerLatch = new CountDownLatch(numServers);
    Set<HttpServer> connectedServers = new ConcurrentHashSet<>();
    for (int i = 0; i < numServers; i++) {
      HttpServer server = vertx.createHttpServer(HttpServerOptions.options().setHost(DEFAULT_HTTP_HOST).setPort(DEFAULT_HTTP_PORT));
      server.requestHandler(req -> {
        connectedServers.add(server);
        req.response().end();
      });
      server.listen(ar -> {
        assertTrue(ar.succeeded());
        startServerLatch.countDown();
      });
      servers[i] = server;
    }

    awaitLatch(startServerLatch);

    CountDownLatch reqLatch = new CountDownLatch(requests);
    for (int count = 0; count < requests; count++) {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        assertEquals(200, resp.statusCode());
        reqLatch.countDown();
      });
    }

    awaitLatch(reqLatch);

    assertEquals(expectedConnectedServers, connectedServers.size());

    CountDownLatch serverCloseLatch = new CountDownLatch(numServers);
    for (HttpServer server: servers) {
      server.close(ar -> {
        assertTrue(ar.succeeded());
        serverCloseLatch.countDown();
      });
    }

    awaitLatch(serverCloseLatch);
  }

  @Test
  public void testSendFile() throws Exception {
    String content = TestUtils.randomUnicodeString(10000);
    sendFile("test-send-file.html", content, null, false);
  }

  @Test
  public void testSendFileWithHandler() throws Exception {
    String content = TestUtils.randomUnicodeString(10000);
    sendFile("test-send-file.html", content, null, true);
  }

  @Test
  public void testFileNotFound() throws Exception {
    sendFile(null, "<html><body>Resource not found</body><html>", null, false);
  }

  @Test
  public void testSendFileNotFoundWith404Page() throws Exception {
    String content = "<html><body>This is my 404 page</body></html>";
    sendFile(null, content, "my-404-page.html", false);
  }

  @Test
  public void testSendFileNotFoundWith404PageAndHandler() throws Exception {
    String content = "<html><body>This is my 404 page</body></html>";
    sendFile(null, content, "my-404-page.html", true);
  }

  private void sendFile(String sendFile, String contentExpected, String notFoundFile, boolean handler) throws Exception {
    File fileToDelete;
    if (sendFile != null) {
      fileToDelete = setupFile(sendFile, contentExpected);
    } else if (notFoundFile != null) {
      fileToDelete = setupFile(notFoundFile, contentExpected);
    } else {
      fileToDelete = null;
    }

    CountDownLatch latch;
    if (handler) {
      latch = new CountDownLatch(2);
    } else {
      latch = new CountDownLatch(1);
    }

    server.requestHandler(req -> {
      if (handler) {
        Handler<AsyncResult<Void>> completionHandler = onSuccess(v -> latch.countDown());
        if (sendFile != null) { // Send file with handler
          req.response().sendFile(fileToDelete.getAbsolutePath(), null, completionHandler);
        } else if (notFoundFile != null) { // File doesn't exist, send not found resource with handler
          req.response().sendFile("doesnotexist.html", fileToDelete.getAbsolutePath(), completionHandler);
        } else { // File doesn't exist, send default not found resource with handler
          req.response().sendFile("doesnotexist.html", null, completionHandler);
        }
      } else {
        if (sendFile != null) { // Send file
          req.response().sendFile(fileToDelete.getAbsolutePath());
        } else if (notFoundFile != null) { // File doesn't exist, send not found resource
          req.response().sendFile("doesnotexist.html", fileToDelete.getAbsolutePath());
        } else { // File doesn't exist, send default not found resource
          req.response().sendFile("doesnotexist.html");
        }
      }
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        if (sendFile != null) {
          assertEquals(200, resp.statusCode());
        } else {
          assertEquals(404, resp.statusCode());
        }
        assertEquals("text/html", resp.headers().get("Content-Type"));
        resp.bodyHandler(buff -> {
          assertEquals(contentExpected, buff.toString());
          if (fileToDelete != null) {
            assertEquals(fileToDelete.length(), Long.parseLong(resp.headers().get("content-length")));
            fileToDelete.delete();
          }
          latch.countDown();
        });
      });
    }));

    assertTrue("Timed out waiting for test to complete.", latch.await(10, TimeUnit.SECONDS));

    testComplete();
  }

  @Test
  public void testSendFileOverrideHeaders() throws Exception {
    String content = TestUtils.randomUnicodeString(10000);
    File file = setupFile("test-send-file.html", content);

    server.requestHandler(req -> {
      req.response().putHeader("Content-Type", "wibble");
      req.response().sendFile(file.getAbsolutePath());
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        assertEquals(file.length(), Long.parseLong(resp.headers().get("content-length")));
        assertEquals("wibble", resp.headers().get("content-type"));
        resp.bodyHandler(buff -> {
          assertEquals(content, buff.toString());
          file.delete();
          testComplete();
        });
      });
    }));

    await();
  }

  @Test
  public void test100ContinueDefault() throws Exception {
    Buffer toSend = TestUtils.randomBuffer(1000);

    server.requestHandler(req -> {
      req.bodyHandler(data -> {
        assertEquals(toSend, data);
        req.response().end();
      });
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.put(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.endHandler(v -> testComplete());
      });
      req.headers().set("Expect", "100-continue");
      req.setChunked(true);
      req.continueHandler(v -> {
        req.writeBuffer(toSend);
        req.end();
      });
      req.sendHead();
    }));

    await();
  }

  @Test
  public void test100ContinueHandled() throws Exception {
    Buffer toSend = TestUtils.randomBuffer(1000);
    server.requestHandler(req -> {
      req.response().headers().set("HTTP/1.1", "100 Continue");
      req.bodyHandler(data -> {
        assertEquals(toSend, data);
        req.response().end();
      });
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.put(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.endHandler(v -> testComplete());
      });
      req.headers().set("Expect", "100-continue");
      req.setChunked(true);
      req.continueHandler(v -> {
        req.writeBuffer(toSend);
        req.end();
      });
      req.sendHead();
    }));

    await();
  }

  @Test
  public void testClientDrainHandler() {
    pausingServer(s -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), noOpHandler());
      req.setChunked(true);
      assertFalse(req.writeQueueFull());
      req.setWriteQueueMaxSize(1000);
      Buffer buff = TestUtils.randomBuffer(10000);
      vertx.setPeriodic(1, id -> {
        req.writeBuffer(buff);
        if (req.writeQueueFull()) {
          vertx.cancelTimer(id);
          req.drainHandler(v -> {
            assertFalse(req.writeQueueFull());
            testComplete();
          });

          // Tell the server to resume
          vertx.eventBus().send("server_resume", "");
        }
      });
    });

    await();
  }

  @Test
  public void testServerDrainHandler() {
    drainingServer(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.pause();
        Handler<Message<Buffer>> resumeHandler = msg -> resp.resume();
        Registration reg = vertx.eventBus().registerHandler("client_resume", resumeHandler);
        resp.endHandler(v -> reg.unregister());
      });
    });

    await();
  }

  @Test
  public void testPoolingKeepAliveAndPipelining() {
    testPooling(true, true);
  }

  @Test
  public void testPoolingKeepAliveNoPipelining() {
    testPooling(true, false);
  }

  @Test
  public void testPoolingNoKeepAliveNoPipelining() {
    testPooling(false, false);
  }

  @Test
  public void testPoolingNoKeepAliveAndPipelining() {
    testPooling(false, true);
  }

  private void testPooling(boolean keepAlive, boolean pipelining) {
    String path = "foo.txt";
    int numGets = 100;
    int maxPoolSize = 10;
    client.close();
    client = vertx.createHttpClient(HttpClientOptions.options().setKeepAlive(keepAlive).setPipelining(pipelining).setMaxPoolSize(maxPoolSize));

    server.requestHandler(req -> {
      String cnt = req.headers().get("count");
      req.response().headers().set("count", cnt);
      req.response().end();
    });

    AtomicBoolean completeAlready = new AtomicBoolean();

    server.listen(onSuccess(s -> {

      AtomicInteger cnt = new AtomicInteger(0);
      for (int i = 0; i < numGets; i++) {
        int theCount = i;
        HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(path), resp -> {
          assertEquals(200, resp.statusCode());
          assertEquals(theCount, Integer.parseInt(resp.headers().get("count")));
          if (cnt.incrementAndGet() == numGets) {
            testComplete();
          }
        });
        req.exceptionHandler(t -> {
          if (pipelining && !keepAlive) {
            // Illegal combination - should get exception
            assertTrue(t instanceof IllegalStateException);
            if (completeAlready.compareAndSet(false, true)) {
              testComplete();
            }
          } else {
            fail("Should not throw exception: " + t.getMessage());
          }
        });
        req.headers().set("count", String.valueOf(i));
        req.end();
      }
    }));

    await();
  }

  @Test
  public void testConnectionErrorsGetReportedToRequest() throws InterruptedException {
    AtomicInteger clientExceptions = new AtomicInteger();
    AtomicInteger req2Exceptions = new AtomicInteger();
    AtomicInteger req3Exceptions = new AtomicInteger();

    CountDownLatch latch = new CountDownLatch(3);

    client.exceptionHandler(t -> {
      assertEquals("More than one call to client exception handler was not expected", 1, clientExceptions.incrementAndGet());
      latch.countDown();
    });

    // This one should cause an error in the Client Exception handler, because it has no exception handler set specifically.
    HttpClientRequest req1 = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setPort(9998).setRequestURI("someurl1"), resp -> {
      fail("Should never get a response on a bad port, if you see this message than you are running an http server on port 9998");
    });
    // No exception handler set on request!

    HttpClientRequest req2 = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setPort(9998).setRequestURI("someurl2"), resp -> {
      fail("Should never get a response on a bad port, if you see this message than you are running an http server on port 9998");
    });

    req2.exceptionHandler(t -> {
      assertEquals("More than one call to req2 exception handler was not expected", 1, req2Exceptions.incrementAndGet());
      latch.countDown();
    });

    HttpClientRequest req3 = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setPort(9998).setRequestURI("someurl2"), resp -> {
      fail("Should never get a response on a bad port, if you see this message than you are running an http server on port 9998");
    });

    req3.exceptionHandler(t -> {
      assertEquals("More than one call to req2 exception handler was not expected", 1, req3Exceptions.incrementAndGet());
      latch.countDown();
    });

    req1.end();
    req2.end();
    req3.end();

    awaitLatch(latch);
    testComplete();
  }

  @Test
  public void testRequestTimesoutWhenIndicatedPeriodExpiresWithoutAResponseFromRemoteServer() {
    server.requestHandler(noOpHandler()); // No response handler so timeout triggers

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("timeoutTest"), resp -> {
        fail("End should not be called because the request should timeout");
      });
      req.exceptionHandler(t -> {
        assertTrue("Expected to end with timeout exception but ended with other exception: " + t, t instanceof TimeoutException);
        testComplete();
      });
      req.setTimeout(1000);
      req.end();
    }));

    await();
  }

  @Test
  public void testRequestTimeoutExtendedWhenResponseChunksReceived() {
    long timeout = 2000;
    int numChunks = 100;
    AtomicInteger count = new AtomicInteger(0);
    long interval = timeout * 2 / numChunks;

    server.requestHandler(req -> {
      req.response().setChunked(true);
      vertx.setPeriodic(interval, timerID -> {
        req.response().writeString("foo");
        if (count.incrementAndGet() == numChunks) {
          req.response().end();
          vertx.cancelTimer(timerID);
        }
      });
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("timeoutTest"), resp -> {
        assertEquals(200, resp.statusCode());
        resp.endHandler(v -> testComplete());
      });
      req.exceptionHandler(t -> fail("Should not be called"));
      req.setTimeout(timeout);
      req.end();
    }));

    await();
  }

  @Test
  public void testRequestTimeoutCanceledWhenRequestHasAnOtherError() {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    // There is no server running, should fail to connect
    HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("timeoutTest"), resp -> {
      fail("End should not be called because the request should fail to connect");
    });
    req.exceptionHandler(exception::set);
    req.setTimeout(800);
    req.end();

    vertx.setTimer(1500, id -> {
      assertNotNull("Expected an exception to be set", exception.get());
      assertFalse("Expected to not end with timeout exception, but did: " + exception.get(), exception.get() instanceof TimeoutException);
      testComplete();
    });

    await();
  }

  @Test
  public void testRequestTimeoutCanceledWhenRequestEndsNormally() {
    server.requestHandler(req -> req.response().end());

    server.listen(onSuccess(s -> {
      AtomicReference<Throwable> exception = new AtomicReference<>();

      // There is no server running, should fail to connect
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("timeoutTest"), noOpHandler());
      req.exceptionHandler(exception::set);
      req.setTimeout(500);
      req.end();

      vertx.setTimer(1000, id -> {
        assertNull("Did not expect any exception", exception.get());
        testComplete();
      });
    }));

    await();
  }

  @Test
  public void testRequestNotReceivedIfTimedout() {
    server.requestHandler(req -> {
      vertx.setTimer(500, id -> {
        req.response().setStatusCode(200);
        req.response().writeStringAndEnd("OK");
      });
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("timeoutTest"), resp -> fail("Response should not be handled"));
      req.exceptionHandler(t -> {
        assertTrue("Expected to end with timeout exception but ended with other exception: " + t, t instanceof TimeoutException);
        //Delay a bit to let any response come back
        vertx.setTimer(500, id -> testComplete());
      });
      req.setTimeout(100);
      req.end();
    }));

    await();
  }

  @Test
  public void testServerWebsocketIdleTimeout() {
    server.close();
    server = vertx.createHttpServer(HttpServerOptions.options().setIdleTimeout(1).setPort(DEFAULT_HTTP_PORT).setHost(DEFAULT_HTTP_HOST));
    server.websocketHandler(ws -> {}).listen(ar -> {
      assertTrue(ar.succeeded());
      client.connectWebsocket(WebSocketConnectOptions.options().setPort(DEFAULT_HTTP_PORT), ws -> {
        ws.closeHandler(v -> testComplete());
      });
    });

    await();
  }


  @Test
  public void testClientWebsocketIdleTimeout() {
    client.close();
    client = vertx.createHttpClient(HttpClientOptions.options().setIdleTimeout(1));
    server.websocketHandler(ws -> {}).listen(ar -> {
      client.connectWebsocket(WebSocketConnectOptions.options().setPort(DEFAULT_HTTP_PORT), ws -> {
        ws.closeHandler(v -> testComplete());
      });

    });

    await();
  }

  @Test
  // Client trusts all server certs
  public void testTLSClientTrustAll() throws Exception {
    testTLS(KS.NONE, TS.NONE, KS.JKS, TS.NONE, false, false, true, false, true);
  }

  @Test
  // Server specifies cert that the client trusts (not trust all)
  public void testTLSClientTrustServerCert() throws Exception {
    testTLS(KS.NONE, TS.JKS, KS.JKS, TS.NONE, false, false, false, false, true);
  }

  @Test
  // Server specifies cert that the client trusts (not trust all)
  public void testTLSClientTrustServerCertPKCS12() throws Exception {
    testTLS(KS.NONE, TS.JKS, KS.PKCS12, TS.NONE, false, false, false, false, true);
  }

  @Test
  // Server specifies cert that the client trusts (not trust all)
  public void testTLSClientTrustServerCertPEM() throws Exception {
    testTLS(KS.NONE, TS.JKS, KS.PEM, TS.NONE, false, false, false, false, true);
  }

  @Test
  // Server specifies cert that the client trusts via a CA (not trust all)
  public void testTLSClientTrustServerCertPEM_CA() throws Exception {
    testTLS(KS.NONE, TS.PEM_CA, KS.PEM_CA, TS.NONE, false, false, false, false, true);
  }

  @Test
  // Server specifies cert that the client trusts (not trust all)
  public void testTLSClientTrustPKCS12ServerCert() throws Exception {
    testTLS(KS.NONE, TS.PKCS12, KS.JKS, TS.NONE, false, false, false, false, true);
  }

  @Test
  // Server specifies cert that the client trusts (not trust all)
  public void testTLSClientTrustPEMServerCert() throws Exception {
    testTLS(KS.NONE, TS.PEM, KS.JKS, TS.NONE, false, false, false, false, true);
  }

  @Test
  // Server specifies cert that the client doesn't trust
  public void testTLSClientUntrustedServer() throws Exception {
    testTLS(KS.NONE, TS.NONE, KS.JKS, TS.NONE, false, false, false, false, false);
  }

  @Test
  // Server specifies cert that the client doesn't trust
  public void testTLSClientUntrustedServerPEM() throws Exception {
    testTLS(KS.NONE, TS.NONE, KS.PEM, TS.NONE, false, false, false, false, false);
  }

  @Test
  //Client specifies cert even though it's not required
  public void testTLSClientCertNotRequired() throws Exception {
    testTLS(KS.JKS, TS.JKS, KS.JKS, TS.JKS, false, false, false, false, true);
  }

  @Test
  //Client specifies cert even though it's not required
  public void testTLSClientCertNotRequiredPEM() throws Exception {
    testTLS(KS.JKS, TS.JKS, KS.PEM, TS.JKS, false, false, false, false, true);
  }

  @Test
  //Client specifies cert and it is required
  public void testTLSClientCertRequired() throws Exception {
    testTLS(KS.JKS, TS.JKS, KS.JKS, TS.JKS, true, false, false, false, true);
  }

  @Test
  //Client specifies cert and it is required
  public void testTLSClientCertRequiredPKCS12() throws Exception {
    testTLS(KS.JKS, TS.JKS, KS.JKS, TS.PKCS12, true, false, false, false, true);
  }

  @Test
  //Client specifies cert and it is required
  public void testTLSClientCertRequiredPEM() throws Exception {
    testTLS(KS.JKS, TS.JKS, KS.JKS, TS.PEM, true, false, false, false, true);
  }

  @Test
  //Client specifies cert and it is required
  public void testTLSClientCertPKCS12Required() throws Exception {
    testTLS(KS.PKCS12, TS.JKS, KS.JKS, TS.JKS, true, false, false, false, true);
  }

  @Test
  //Client specifies cert and it is required
  public void testTLSClientCertPEMRequired() throws Exception {
    testTLS(KS.PEM, TS.JKS, KS.JKS, TS.JKS, true, false, false, false, true);
  }

  @Test
  //Client specifies cert by CA and it is required
  public void testTLSClientCertPEM_CARequired() throws Exception {
    testTLS(KS.PEM_CA, TS.JKS, KS.JKS, TS.PEM_CA, true, false, false, false, true);
  }

  @Test
  //Client doesn't specify cert but it's required
  public void testTLSClientCertRequiredNoClientCert() throws Exception {
    testTLS(KS.NONE, TS.JKS, KS.JKS, TS.JKS, true, false, false, false, false);
  }

  @Test
  //Client specifies cert but it's not trusted
  public void testTLSClientCertClientNotTrusted() throws Exception {
    testTLS(KS.JKS, TS.JKS, KS.JKS, TS.NONE, true, false, false, false, false);
  }

  @Test
  // Server specifies cert that the client does not trust via a revoked certificate of the CA
  public void testTLSClientRevokedServerCert() throws Exception {
    testTLS(KS.NONE, TS.PEM_CA, KS.PEM_CA, TS.NONE, false, false, false, true, false);
  }

  @Test
  //Client specifies cert that the server does not trust via a revoked certificate of the CA
  public void testTLSRevokedClientCertServer() throws Exception {
    testTLS(KS.PEM_CA, TS.JKS, KS.JKS, TS.PEM_CA, true, true, false, false, false);
  }

  @Test
  // Specify some cipher suites
  public void testTLSCipherSuites() throws Exception {
    testTLS(KS.NONE, TS.NONE, KS.JKS, TS.NONE, false, false, true, false, true, ENABLED_CIPHER_SUITES);
  }

  private void testTLS(KS clientCert, TS clientTrust,
                       KS serverCert, TS serverTrust,
                       boolean requireClientAuth, boolean serverUsesCrl, boolean clientTrustAll,
                       boolean clientUsesCrl, boolean shouldPass,
                       String... enabledCipherSuites) throws Exception {
    client.close();
    server.close();
    HttpClientOptions options = HttpClientOptions.options();
    options.setSsl(true);
    if (clientTrustAll) {
      options.setTrustAll(true);
    }
    if (clientUsesCrl) {
      options.addCrlPath(findFileOnClasspath("tls/ca/crl.pem"));
    }
    options.setTrustStoreOptions(getClientTrustOptions(clientTrust));
    options.setKeyStoreOptions(getClientCertOptions(clientCert));
    for (String suite: enabledCipherSuites) {
      options.addEnabledCipherSuite(suite);
    }
    client = vertx.createHttpClient(options);
    HttpServerOptions serverOptions = HttpServerOptions.options();
    serverOptions.setSsl(true);
    serverOptions.setTrustStoreOptions(getServerTrustOptions(serverTrust));
    serverOptions.setKeyStoreOptions(getServerCertOptions(serverCert));
    if (requireClientAuth) {
      serverOptions.setClientAuthRequired(true);
    }
    if (serverUsesCrl) {
      serverOptions.addCrlPath(findFileOnClasspath("tls/ca/crl.pem"));
    }
    for (String suite: enabledCipherSuites) {
      serverOptions.addEnabledCipherSuite(suite);
    }
    server = vertx.createHttpServer(serverOptions.setPort(4043));
    server.requestHandler(req -> {
      req.bodyHandler(buffer -> {
        assertEquals("foo", buffer.toString());
        req.response().writeStringAndEnd("bar");
      });
    });
    server.listen(ar -> {
      assertTrue(ar.succeeded());

      client.exceptionHandler(t -> {
        if (shouldPass) {
          fail("Should not throw exception");
        } else {
          testComplete();
        }
      });
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setPort(4043).setRequestURI(DEFAULT_TEST_URI), response -> {
        response.bodyHandler(data -> assertEquals("bar", data.toString()));
        testComplete();
      });
      req.writeStringAndEnd("foo");
    });
    await();
  }

  @Test
  public void testJKSInvalidPath() {
    testInvalidKeyStore(((JKSOptions) getServerCertOptions(KS.JKS)).setPath("/invalid.jks"), "java.nio.file.NoSuchFileException: /invalid.jks");
  }

  @Test
  public void testJKSMissingPassword() {
    testInvalidKeyStore(((JKSOptions) getServerCertOptions(KS.JKS)).setPassword(null), "Password must not be null");
  }

  @Test
  public void testJKSInvalidPassword() {
    testInvalidKeyStore(((JKSOptions) getServerCertOptions(KS.JKS)).setPassword("wrongpassword"), "Keystore was tampered with, or password was incorrect");
  }

  @Test
  public void testPKCS12InvalidPath() {
    testInvalidKeyStore(((PKCS12Options) getServerCertOptions(KS.PKCS12)).setPath("/invalid.p12"), "java.nio.file.NoSuchFileException: /invalid.p12");
  }

  @Test
  public void testPKCS12MissingPassword() {
    testInvalidKeyStore(((PKCS12Options) getServerCertOptions(KS.PKCS12)).setPassword(null), "Get Key failed: null");
  }

  @Test
  public void testPKCS12InvalidPassword() {
    testInvalidKeyStore(((PKCS12Options) getServerCertOptions(KS.PKCS12)).setPassword("wrongpassword"), "failed to decrypt safe contents entry: javax.crypto.BadPaddingException: Given final block not properly padded");
  }

  @Test
  public void testKeyCertMissingKeyPath() {
    testInvalidKeyStore(((KeyCertOptions) getServerCertOptions(KS.PEM)).setKeyPath(null), "Missing private key");
  }

  @Test
  public void testKeyCertInvalidKeyPath() {
    testInvalidKeyStore(((KeyCertOptions) getServerCertOptions(KS.PEM)).setKeyPath("/invalid.pem"), "java.nio.file.NoSuchFileException: /invalid.pem");
  }

  @Test
  public void testKeyCertMissingCertPath() {
    testInvalidKeyStore(((KeyCertOptions) getServerCertOptions(KS.PEM)).setCertPath(null), "Missing X.509 certificate");
  }

  @Test
  public void testKeyCertInvalidCertPath() {
    testInvalidKeyStore(((KeyCertOptions) getServerCertOptions(KS.PEM)).setCertPath("/invalid.pem"), "java.nio.file.NoSuchFileException: /invalid.pem");
  }

  @Test
  public void testKeyCertInvalidPem() throws IOException {
    String[] contents = {
        "",
        "-----BEGIN PRIVATE KEY-----",
        "-----BEGIN PRIVATE KEY-----\n-----END PRIVATE KEY-----",
        "-----BEGIN PRIVATE KEY-----\n*\n-----END PRIVATE KEY-----"
    };
    String[] messages = {
        "Missing -----BEGIN PRIVATE KEY----- delimiter",
        "Missing -----END PRIVATE KEY----- delimiter",
        "Empty pem file",
        "Input byte[] should at least have 2 bytes for base64 bytes"
    };
    for (int i = 0;i < contents.length;i++) {
      Path file = Files.createTempFile("vertx", ".pem");
      file.toFile().deleteOnExit();
      Files.write(file, Collections.singleton(contents[i]));
      String expectedMessage = messages[i];
      testInvalidKeyStore(((KeyCertOptions) getServerCertOptions(KS.PEM)).setKeyPath(file.toString()), expectedMessage);
    }
  }

  @Test
  public void testCaInvalidPath() {
    testInvalidTrustStore(CaOptions.options().addCertPath("/invalid.pem"), "java.nio.file.NoSuchFileException: /invalid.pem");
  }

  @Test
  public void testCaInvalidPem() throws IOException {
    String[] contents = {
        "",
        "-----BEGIN CERTIFICATE-----",
        "-----BEGIN CERTIFICATE-----\n-----END CERTIFICATE-----",
        "-----BEGIN CERTIFICATE-----\n*\n-----END CERTIFICATE-----"
    };
    String[] messages = {
        "Missing -----BEGIN CERTIFICATE----- delimiter",
        "Missing -----END CERTIFICATE----- delimiter",
        "Empty pem file",
        "Input byte[] should at least have 2 bytes for base64 bytes"
    };
    for (int i = 0;i < contents.length;i++) {
      Path file = Files.createTempFile("vertx", ".pem");
      file.toFile().deleteOnExit();
      Files.write(file, Collections.singleton(contents[i]));
      String expectedMessage = messages[i];
      testInvalidTrustStore(CaOptions.options().addCertPath(file.toString()), expectedMessage);
    }
  }

  private void testInvalidKeyStore(KeyStoreOptions ksOptions, String expectedMessage) {
    HttpServerOptions serverOptions = HttpServerOptions.options();
    serverOptions.setKeyStoreOptions(ksOptions);
    serverOptions.setSsl(true);
    serverOptions.setPort(4043);
    testStore(serverOptions, expectedMessage);
  }

  private void testInvalidTrustStore(TrustStoreOptions tsOptions, String expectedMessage) {
    HttpServerOptions serverOptions = HttpServerOptions.options();
    serverOptions.setTrustStoreOptions(tsOptions);
    serverOptions.setSsl(true);
    serverOptions.setPort(4043);
    testStore(serverOptions, expectedMessage);
  }

  private void testStore(HttpServerOptions serverOptions, String expectedMessage) {
    HttpServer server = vertx.createHttpServer(serverOptions);
    server.requestHandler(req -> {
    });
    try {
      server.listen();
      fail("Was expecting a failure");
    } catch (VertxException e) {
      assertNotNull(e.getCause());
      assertEquals(expectedMessage, e.getCause().getMessage());
    }
  }

  @Test
  public void testCrlInvalidPath() throws Exception {
    HttpClientOptions clientOptions = HttpClientOptions.options();
    clientOptions.setTrustStoreOptions(getClientTrustOptions(TS.PEM_CA));
    clientOptions.setSsl(true);
    clientOptions.addCrlPath("/invalid.pem");
    HttpClient client = vertx.createHttpClient(clientOptions);
    HttpClientRequest req = client.connect(RequestOptions.options(), (handler) -> {});
    try {
      req.end();
      fail("Was expecting a failure");
    } catch (VertxException e) {
      assertNotNull(e.getCause());
      assertEquals("java.nio.file.NoSuchFileException: /invalid.pem", e.getCause().getMessage());
    }
  }

  @Test
  public void testConnectInvalidPort() {
    client.exceptionHandler(t -> testComplete());
    client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setPort(9998).setRequestURI(DEFAULT_TEST_URI), resp -> fail("Connect should not be called"));

    await();
  }

  @Test
  public void testConnectInvalidHost() {
    client.exceptionHandler(t -> testComplete());
    client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setHost("255.255.255.255").setRequestURI(DEFAULT_TEST_URI), resp -> fail("Connect should not be called"));

    await();
  }

  @Test
  public void testSetHandlersAfterListening() throws Exception {
    server.requestHandler(noOpHandler());

    server.listen(onSuccess(s -> {
      try {
        server.requestHandler(noOpHandler());
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //Ok
      }
      try {
        server.websocketHandler(noOpHandler());
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //Ok
      }
      testComplete();
    }));

    await();
  }

  @Test
  public void testSetHandlersAfterListening2() throws Exception {
    server.requestHandler(noOpHandler());

    server.listen();
    try {
      server.requestHandler(noOpHandler());
      fail("Should throw exception");
    } catch (IllegalStateException e) {
      //Ok
    }
    try {
      server.websocketHandler(noOpHandler());
      fail("Should throw exception");
    } catch (IllegalStateException e) {
      //Ok
    }
  }

  @Test
  public void testListenNoHandlers() throws Exception {
    try {
      server.listen(ar -> {
      });
      fail("Should throw exception");
    } catch (IllegalStateException e) {
      //Ok
    }
  }

  @Test
  public void testListenNoHandlers2() throws Exception {
    try {
      server.listen();
      fail("Should throw exception");
    } catch (IllegalStateException e) {
      //Ok
    }
  }

  @Test
  public void testListenTwice() throws Exception {
    server.requestHandler(noOpHandler());
    server.listen();
    try {
      server.listen();
      fail("Should throw exception");
    } catch (IllegalStateException e) {
      //Ok
    }
  }

  @Test
  public void testListenTwice2() throws Exception {
    server.requestHandler(noOpHandler());
    server.listen(ar -> {
      assertTrue(ar.succeeded());
      try {
        server.listen();
        fail("Should throw exception");
      } catch (IllegalStateException e) {
        //Ok
      }
      testComplete();
    });
    await();
  }

  @Test
  public void testSharedServersRoundRobin() throws Exception {
    client.close();
    server.close();
    client = vertx.createHttpClient(HttpClientOptions.options().setKeepAlive(false));
    int numServers = 5;
    int numRequests = numServers * 100;

    List<HttpServer> servers = new ArrayList<>();
    Set<HttpServer> connectedServers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    Map<HttpServer, Integer> requestCount = new ConcurrentHashMap<>();

    CountDownLatch latchListen = new CountDownLatch(numServers);
    CountDownLatch latchConns = new CountDownLatch(numRequests);
    Set<Context> contexts = new ConcurrentHashSet<>();
    for (int i = 0; i < numServers; i++) {
      HttpServer theServer = vertx.createHttpServer(HttpServerOptions.options().setPort(DEFAULT_HTTP_PORT));
      servers.add(theServer);
      final AtomicReference<Context> context = new AtomicReference<>();
      theServer.requestHandler(req -> {
        Context ctx = vertx.currentContext();
        if (context.get() != null) {
          assertSame(ctx, context.get());
        } else {
          context.set(ctx);
          contexts.add(ctx);
        }
        connectedServers.add(theServer);
        Integer cnt = requestCount.get(theServer);
        int icnt = cnt == null ? 0 : cnt;
        icnt++;
        requestCount.put(theServer, icnt);
        latchConns.countDown();
        req.response().end();
      }).listen(onSuccess(s -> latchListen.countDown()));
    }
    assertTrue(latchListen.await(10, TimeUnit.SECONDS));


    // Create a bunch of connections
    CountDownLatch latchClient = new CountDownLatch(numRequests);
    for (int i = 0; i < numRequests; i++) {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), res -> latchClient.countDown());
    }

    assertTrue(latchClient.await(10, TimeUnit.SECONDS));
    assertTrue(latchConns.await(10, TimeUnit.SECONDS));

    assertEquals(numServers, connectedServers.size());
    for (HttpServer server : servers) {
      assertTrue(connectedServers.contains(server));
    }
    assertEquals(numServers, requestCount.size());
    for (int cnt : requestCount.values()) {
      assertEquals(numRequests / numServers, cnt);
    }
    assertEquals(numServers, contexts.size());

    CountDownLatch closeLatch = new CountDownLatch(numServers);

    for (HttpServer server : servers) {
      server.close(ar -> {
        assertTrue(ar.succeeded());
        closeLatch.countDown();
      });
    }

    assertTrue(closeLatch.await(10, TimeUnit.SECONDS));

    testComplete();
  }

  @Test
  public void testSharedServersRoundRobinWithOtherServerRunningOnDifferentPort() throws Exception {
    // Have a server running on a different port to make sure it doesn't interact
    CountDownLatch latch = new CountDownLatch(1);
    HttpServer theServer = vertx.createHttpServer(HttpServerOptions.options().setPort(8081));
    theServer.requestHandler(req -> {
      fail("Should not process request");
    }).listen(onSuccess(s -> latch.countDown()));
    awaitLatch(latch);

    testSharedServersRoundRobin();
  }

  @Test
  public void testSharedServersRoundRobinButFirstStartAndStopServer() throws Exception {
    // Start and stop a server on the same port/host before hand to make sure it doesn't interact
    CountDownLatch latch = new CountDownLatch(1);
    HttpServer theServer = vertx.createHttpServer(HttpServerOptions.options().setPort(DEFAULT_HTTP_PORT));
    theServer.requestHandler(req -> {
      fail("Should not process request");
    }).listen(onSuccess(s -> latch.countDown()));
    awaitLatch(latch);

    CountDownLatch closeLatch = new CountDownLatch(1);
    theServer.close(ar -> {
      assertTrue(ar.succeeded());
      closeLatch.countDown();
    });
    assertTrue(closeLatch.await(10, TimeUnit.SECONDS));

    testSharedServersRoundRobin();
  }

  @Test
  public void testHeadNoBody() {
    server.requestHandler(req -> {
      assertEquals("HEAD", req.method());
      // Head never contains a body but it can contain a Content-Length header
      // Since headers from HEAD must correspond EXACTLY with corresponding headers for GET
      req.response().headers().set("Content-Length", String.valueOf(41));
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.head(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        assertEquals(41, Integer.parseInt(resp.headers().get("Content-Length")));
        resp.endHandler(v -> testComplete());
      }).end();
    }));

    await();
  }

  @Test
  public void testRemoteAddress() {
    server.requestHandler(req -> {
      assertEquals("127.0.0.1", req.remoteAddress().hostAddress());
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> resp.endHandler(v -> testComplete()));
    }));

    await();
  }

  @Test
  public void testGetAbsoluteURI() {
    server.requestHandler(req -> {
      assertEquals("http://localhost:" + DEFAULT_HTTP_PORT + "/foo/bar", req.absoluteURI().toString());
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("/foo/bar"), resp -> resp.endHandler(v -> testComplete()));
    }));

    await();
  }

  @Test
  public void testListenInvalidPort() {
    server.close();
    server = vertx.createHttpServer(HttpServerOptions.options().setPort(7));
    server.requestHandler(noOpHandler()).listen(onFailure(server -> {
      testComplete();
    }));
    await();
  }

  @Test
  public void testListenInvalidHost() {
    server.close();
    server = vertx.createHttpServer(HttpServerOptions.options().setPort(DEFAULT_HTTP_PORT).setHost("iqwjdoqiwjdoiqwdiojwd"));
    server.requestHandler(noOpHandler());
    server.listen(onFailure(s -> testComplete()));
  }

  @Test
  public void testPauseClientResponse() {
    int numWrites = 10;
    int numBytes = 100;
    server.requestHandler(req -> {
      req.response().setChunked(true);
      // Send back a big response in several chunks
      for (int i = 0; i < numWrites; i++) {
        req.response().writeBuffer(TestUtils.randomBuffer(numBytes));
      }
      req.response().end();
    });

    AtomicBoolean paused = new AtomicBoolean();
    Buffer totBuff = Buffer.buffer();
    HttpClientRequest clientRequest = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
      resp.pause();
      paused.set(true);
      resp.dataHandler(chunk -> {
        if (paused.get()) {
          fail("Shouldn't receive chunks when paused");
        } else {
          totBuff.appendBuffer(chunk);
        }
      });
      resp.endHandler(v -> {
        if (paused.get()) {
          fail("Shouldn't receive chunks when paused");
        } else {
          assertEquals(numWrites * numBytes, totBuff.length());
          testComplete();
        }
      });
      vertx.setTimer(500, id -> {
        paused.set(false);
        resp.resume();
      });
    });

    server.listen(onSuccess(s -> clientRequest.end()));

    await();
  }

  @Test
  public void testHttpVersion() {
    server.requestHandler(req -> {
      assertEquals("HTTP/1.1", req.version());
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> resp.endHandler(v -> testComplete()));
    }));

    await();
  }

  @Test
  public void testFormUploadFile() throws Exception {
    AtomicInteger attributeCount = new AtomicInteger();
    String content = "Vert.x rocks!";

    server.requestHandler(req -> {
      if (req.method().equals("POST")) {
        assertEquals(req.path(), "/form");
        req.response().setChunked(true);
        req.setExpectMultipart(true);
        req.uploadHandler(upload -> {
          upload.dataHandler(buffer -> {
            assertEquals(content, buffer.toString("UTF-8"));
          });
          assertEquals("file", upload.name());
          assertEquals("tmp-0.txt", upload.filename());
          assertEquals("image/gif", upload.contentType());
          upload.endHandler(v -> {
            assertTrue(upload.isSizeAvailable());
            assertEquals(content.length(), upload.size());
          });
        });
        req.endHandler(v -> {
          Headers attrs = req.formAttributes();
          attributeCount.set(attrs.size());
          req.response().end();
        });
      }
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("/form"), resp -> {
        // assert the response
        assertEquals(200, resp.statusCode());
        resp.bodyHandler(body -> {
          assertEquals(0, body.length());
        });
        assertEquals(0, attributeCount.get());
        testComplete();
      });

      String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
      Buffer buffer = Buffer.buffer();
      String body =
        "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"file\"; filename=\"tmp-0.txt\"\r\n" +
          "Content-Type: image/gif\r\n" +
          "\r\n" +
          content + "\r\n" +
          "--" + boundary + "--\r\n";

      buffer.appendString(body);
      req.headers().set("content-length", String.valueOf(buffer.length()));
      req.headers().set("content-type", "multipart/form-data; boundary=" + boundary);
      req.writeBuffer(buffer).end();
    }));

    await();
  }

  @Test
  public void testFormUploadAttributes() throws Exception {
    AtomicInteger attributeCount = new AtomicInteger();
    server.requestHandler(req -> {
      if (req.method().equals("POST")) {
        assertEquals(req.path(), "/form");
        req.response().setChunked(true);
        req.setExpectMultipart(true);
        req.uploadHandler(upload -> upload.dataHandler(buffer -> {
          fail("Should get here");
        }));
        req.endHandler(v -> {
          Headers attrs = req.formAttributes();
          attributeCount.set(attrs.size());
          assertEquals("vert x", attrs.get("framework"));
          assertEquals("jvm", attrs.get("runson"));
          req.response().end();
        });
      }
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("/form"), resp -> {
        // assert the response
        assertEquals(200, resp.statusCode());
        resp.bodyHandler(body -> {
          assertEquals(0, body.length());
        });
        assertEquals(2, attributeCount.get());
        testComplete();
      });
      try {
        Buffer buffer = Buffer.buffer();
        // Make sure we have one param that needs url encoding
        buffer.appendString("framework=" + URLEncoder.encode("vert x", "UTF-8") + "&runson=jvm", "UTF-8");
        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.writeBuffer(buffer).end();
      } catch (UnsupportedEncodingException e) {
        fail(e.getMessage());
      }
    }));

    await();
  }

  @Test
  public void testFormUploadAttributes2() throws Exception {
    AtomicInteger attributeCount = new AtomicInteger();
    server.requestHandler(req -> {
      if (req.method().equals("POST")) {
        assertEquals(req.path(), "/form");
        req.setExpectMultipart(true);
        req.uploadHandler(event -> event.dataHandler(buffer -> {
          fail("Should not get here");
        }));
        req.endHandler(v -> {
          Headers attrs = req.formAttributes();
          attributeCount.set(attrs.size());
          assertEquals("junit-testUserAlias", attrs.get("origin"));
          assertEquals("admin@foo.bar", attrs.get("login"));
          assertEquals("admin", attrs.get("pass word"));
          req.response().end();
        });
      }
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.post(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI("/form"), resp -> {
        // assert the response
        assertEquals(200, resp.statusCode());
        resp.bodyHandler(body -> {
          assertEquals(0, body.length());
        });
        assertEquals(3, attributeCount.get());
        testComplete();
      });
      Buffer buffer = Buffer.buffer();
      buffer.appendString("origin=junit-testUserAlias&login=admin%40foo.bar&pass+word=admin");
      req.headers().set("content-length", String.valueOf(buffer.length()));
      req.headers().set("content-type", "application/x-www-form-urlencoded");
      req.writeBuffer(buffer).end();
    }));

    await();
  }

  @Test
  public void testAccessNetSocket() throws Exception {
    Buffer toSend = TestUtils.randomBuffer(1000);

    server.requestHandler(req -> {
      req.response().headers().set("HTTP/1.1", "101 Upgrade");
      req.bodyHandler(data -> {
        assertEquals(toSend, data);
        req.response().end();
      });
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.endHandler(v -> {
          assertNotNull(resp.netSocket());
          testComplete();
        });
      });
      req.headers().set("content-length", String.valueOf(toSend.length()));
      req.writeBuffer(toSend);
    }));

    await();
  }

  @Test
  public void testHostHeaderOverridePossible() {
    server.requestHandler(req -> {
      assertEquals("localhost:4444", req.headers().get("Host"));
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> testComplete());
      req.putHeader("Host", "localhost:4444");
      req.end();
    }));

    await();
  }

  @Test
  public void testResponseBodyWriteFixedString() {
    String body = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    Buffer bodyBuff = Buffer.buffer(body);

    server.requestHandler(req -> {
      req.response().setChunked(true);
      req.response().writeString(body);
      req.response().end();
    });

    server.listen(onSuccess(s -> {
      client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        resp.bodyHandler(buff -> {
          assertEquals(bodyBuff, buff);
          testComplete();
        });
      }).end();
    }));

    await();
  }

  @Test
  public void testHttpConnect() {
    Buffer buffer = TestUtils.randomBuffer(128);
    Buffer received = Buffer.buffer();
    vertx.createNetServer(NetServerOptions.options().setPort(1235)).connectHandler(socket -> {
      socket.dataHandler(socket::writeBuffer);
    }).listen(onSuccess(netServer -> {
      server.requestHandler(req -> {
        vertx.createNetClient(NetClientOptions.options()).connect(netServer.actualPort(), "localhost", onSuccess(socket -> {
          req.response().setStatusCode(200);
          req.response().setStatusMessage("Connection established");
          req.response().end();

          // Create pumps which echo stuff
          Pump.pump(req.netSocket(), socket).start();
          Pump.pump(socket, req.netSocket()).start();
          req.netSocket().closeHandler(v -> socket.close());
        }));
      });
      server.listen(onSuccess(s -> {
        client.connect(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
          assertEquals(200, resp.statusCode());
          NetSocket socket = resp.netSocket();
          socket.dataHandler(buff -> {
            received.appendBuffer(buff);
            if (received.length() == buffer.length()) {
              netServer.close();
              assertEquals(buffer, received);
              testComplete();
            }
          });
          socket.writeBuffer(buffer);
        }).end();
      }));
    }));

    await();
  }

  @Test
  public void testRequestsTimeoutInQueue() {

    server.requestHandler(req -> {
      vertx.setTimer(1000, id -> {
        req.response().end();
      });
    });

    client.close();
    client = vertx.createHttpClient(HttpClientOptions.options().setKeepAlive(false).setMaxPoolSize(1));

    server.listen(onSuccess(s -> {
      // Add a few requests that should all timeout
      for (int i = 0; i < 5; i++) {
        HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
          fail("Should not be called");
        });
        req.exceptionHandler(t -> assertTrue(t instanceof TimeoutException));
        req.setTimeout(500);
        req.end();
      }
      // Now another request that should not timeout
      HttpClientRequest req = client.get(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        assertEquals(200, resp.statusCode());
        testComplete();
      });
      req.exceptionHandler(t -> fail("Should not throw exception"));
      req.setTimeout(3000);
      req.end();
    }));

    await();
  }

  @Test
  public void testSendFileDirectory() {
    File file = new File(testDir, "testdirectory");
    server.requestHandler(req -> {
      vertx.fileSystem().mkdir(file.getAbsolutePath(), onSuccess(v -> {
        req.response().sendFile(file.getAbsolutePath());
      }));
    });

    server.listen(onSuccess(s -> {
      client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).setRequestURI(DEFAULT_TEST_URI), resp -> {
        assertEquals(403, resp.statusCode());
        vertx.fileSystem().delete(file.getAbsolutePath(), v -> testComplete());
      });
    }));

    await();
  }

  @Test
  public void testServerOptionsCopiedBeforeUse() {
    server.close();
    HttpServerOptions options = HttpServerOptions.options().setHost(DEFAULT_HTTP_HOST).setPort(DEFAULT_HTTP_PORT);
    HttpServer server = vertx.createHttpServer(options);
    // Now change something - but server should still listen at previous port
    options.setPort(DEFAULT_HTTP_PORT + 1);
    server.requestHandler(req -> {
      req.response().end();
    });
    server.listen(ar -> {
      assertTrue(ar.succeeded());
      client.getNow(RequestOptions.options().setHost(DEFAULT_HTTP_HOST).setPort(DEFAULT_HTTP_PORT).setRequestURI("/uri"), res -> {
        assertEquals(200, res.statusCode());
        testComplete();
      });
    });
    await();
  }

  @Test
  public void testClientOptionsCopiedBeforeUse() {
    client.close();
    server.requestHandler(req -> {
      req.response().end();
    });
    server.listen(ar -> {
      assertTrue(ar.succeeded());
      HttpClientOptions options = HttpClientOptions.options();
      client = vertx.createHttpClient(options);
      // Now change something - but server should ignore this
      options.setSsl(true);
      client.getNow(RequestOptions.options().setHost(DEFAULT_HTTP_HOST).setPort(DEFAULT_HTTP_PORT).setRequestURI("/uri"), res -> {
        assertEquals(200, res.statusCode());
        testComplete();
      });
    });
    await();
  }

  @Test
  public void testRequestOptions() {
    RequestOptions options = RequestOptions.options();
    assertEquals(80, options.getPort());
    assertEquals(options, options.setPort(1234));
    assertEquals(1234, options.getPort());
    try {
      options.setPort(0);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setPort(-1);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    try {
      options.setPort(65536);
      fail("Should throw exception");
    } catch (IllegalArgumentException e) {
      // OK
    }
    assertEquals("localhost", options.getHost());
    String randString = TestUtils.randomUnicodeString(100);
    assertEquals(options, options.setHost(randString));
    assertEquals(randString, options.getHost());
    Headers headers = new CaseInsensitiveHeaders();
    assertNull(options.getHeaders());
    assertEquals(options, options.setHeaders(headers));
    assertSame(headers, options.getHeaders());
    randString = TestUtils.randomUnicodeString(100);
    assertEquals("/", options.getRequestURI());
    assertEquals(options, options.setRequestURI(randString));
    assertEquals(randString, options.getRequestURI());
    options.addHeader("foo", "bar");
    assertNotNull(options.getHeaders());
    assertEquals("bar", options.getHeaders().get("foo"));
    testComplete();
  }

  @Test
  public void testCopyRequestOptions() {
    int port = 4523;
    String host = TestUtils.randomAlphaString(100);
    Headers headers = new CaseInsensitiveHeaders();
    headers.add("foo", "bar");
    String uri = TestUtils.randomAlphaString(100);
    RequestOptions options = RequestOptions.options().setPort(port).setHost(host).setHeaders(headers).setRequestURI(uri);
    RequestOptions copy = RequestOptions.copiedOptions(options);
    assertEquals(port, copy.getPort());
    assertEquals(host, copy.getHost());
    assertEquals(uri, copy.getRequestURI());
    assertSame(headers, copy.getHeaders());
    assertEquals("bar", copy.getHeaders().get("foo"));
    testComplete();
  }

  @Test
  public void testDefaultRequestOptionsJson() {
    RequestOptions def = RequestOptions.options();
    RequestOptions json = RequestOptions.optionsFromJson(new JsonObject());
    testDefaultRequestOptionsBaseJson(def, json);
  }

  @Test
  public void testCopyRequestOptionsJson() {
    int port = 4523;
    String host = TestUtils.randomAlphaString(100);
    Headers headers = new CaseInsensitiveHeaders();
    headers.add("foo", "bar");
    String uri = TestUtils.randomAlphaString(100);
    JsonObject json = new JsonObject();
    json.putNumber("port", port);
    json.putString("host", host);
    json.putString("requestURI", uri);
    JsonObject jheaders = new JsonObject();
    jheaders.putString("foo", "bar");
    json.putObject("headers", jheaders);
    RequestOptions copy = RequestOptions.optionsFromJson(json);
    assertEquals(port, copy.getPort());
    assertEquals(host, copy.getHost());
    assertEquals(uri, copy.getRequestURI());
    assertEquals("bar", copy.getHeaders().get("foo"));
    testComplete();
  }

  @Test
  public void testClientMultiThreaded() throws Exception {
    int numThreads = 10;
    Thread[] threads = new Thread[numThreads];
    CountDownLatch latch = new CountDownLatch(numThreads);
    server.requestHandler(req -> {
      req.response().putHeader("count", req.headers().get("count"));
      req.response().end();
    }).listen(ar -> {
      assertTrue(ar.succeeded());
      for (int i = 0; i < numThreads; i++) {
        int index = i;
        threads[i] = new Thread() {
          public void run() {
            client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT).addHeader("count", String.valueOf(index)), res -> {
              assertEquals(200, res.statusCode());
              assertEquals(String.valueOf(index), res.headers().get("count"));
              latch.countDown();
            });
          }
        };
        threads[i].start();
      }
    });
    awaitLatch(latch);
    for (int i = 0; i < numThreads; i++) {
      threads[i].join();
    }
  }

  @Test
  public void testInVerticle() throws Exception {
    testInVerticle(false);
  }

  @Test
  public void testInWorkerVerticle() throws Exception {
    testInVerticle(true);
  }

  private void testInVerticle(boolean worker) throws Exception {
    client.close();
    server.close();
    class MyVerticle extends AbstractVerticle {
      Context ctx;
      @Override
      public void start() {
        ctx = vertx.currentContext();
        if (worker) {
          assertTrue(ctx instanceof WorkerContext);
        } else {
          assertTrue(ctx instanceof EventLoopContext);
        }
        Thread thr = Thread.currentThread();
        server = vertx.createHttpServer(HttpServerOptions.options().setPort(DEFAULT_HTTP_PORT));
        server.requestHandler(req -> {
          req.response().end();
          assertSame(ctx, vertx.currentContext());
          if (!worker) {
            assertSame(thr, Thread.currentThread());
          }
        });
        server.listen(ar -> {
          assertTrue(ar.succeeded());
          assertSame(ctx, vertx.currentContext());
          if (!worker) {
            assertSame(thr, Thread.currentThread());
          }
          client = vertx.createHttpClient(HttpClientOptions.options());
          client.getNow(RequestOptions.options().setPort(DEFAULT_HTTP_PORT), res -> {
            assertSame(ctx, vertx.currentContext());
            if (!worker) {
              assertSame(thr, Thread.currentThread());
            }
            assertEquals(200, res.statusCode());
            testComplete();
          });
        });
      }
    }
    MyVerticle verticle = new MyVerticle();
    vertx.deployVerticleWithOptions(verticle, DeploymentOptions.options().setWorker(worker));
    await();
  }

  @Test
  public void testUseInMultithreadedWorker() throws Exception {
    class MyVerticle extends AbstractVerticle {
      @Override
      public void start() {
        try {
          server = vertx.createHttpServer(HttpServerOptions.options());
          fail("Should throw exception");
        } catch (IllegalStateException e) {
          // OK
        }
        try {
          client = vertx.createHttpClient(HttpClientOptions.options());
          fail("Should throw exception");
        } catch (IllegalStateException e) {
          // OK
        }
        testComplete();
      }
    }
    MyVerticle verticle = new MyVerticle();
    vertx.deployVerticleWithOptions(verticle, DeploymentOptions.options().setWorker(true).setMultiThreaded(true));
    await();
  }

  @Test
  public void testContexts() throws Exception {
    Set<ContextImpl> contexts = new ConcurrentHashSet<>();
    AtomicInteger cnt = new AtomicInteger();
    AtomicReference<ContextImpl> serverRequestContext = new AtomicReference<>();
    // Server connect handler should always be called with same context
    server.requestHandler(req -> {
      ContextImpl serverContext = ((VertxInternal) vertx).getContext();
      if (serverRequestContext.get() != null) {
        assertSame(serverRequestContext.get(), serverContext);
      } else {
        serverRequestContext.set(serverContext);
      }
      req.response().end();
    });
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<ContextImpl> listenContext = new AtomicReference<>();
    server.listen(ar -> {
      assertTrue(ar.succeeded());
      listenContext.set(((VertxInternal) vertx).getContext());
      latch.countDown();
    });
    awaitLatch(latch);
    CountDownLatch latch2 = new CountDownLatch(1);
    int numReqs = 16;
    int numConns = 8;
    // There should be a context per *connection*
    client.close();
    client = vertx.createHttpClient(HttpClientOptions.options().setMaxPoolSize(numConns));
    for (int i = 0; i < numReqs; i++) {
      client.getNow(RequestOptions.options().setHost(DEFAULT_HTTP_HOST).setPort(DEFAULT_HTTP_PORT), resp -> {
        assertEquals(200, resp.statusCode());
        contexts.add(((VertxInternal) vertx).getContext());
        if (cnt.incrementAndGet() == numReqs) {
          // Some connections might get closed if response comes back quick enough hence the >=
          assertTrue(contexts.size() >= numConns);
          latch2.countDown();
        }
      });
    }
    awaitLatch(latch2);
    // Close should be in own context
    server.close(ar -> {
      assertTrue(ar.succeeded());
      ContextImpl closeContext = ((VertxInternal) vertx).getContext();
      assertFalse(contexts.contains(closeContext));
      assertNotSame(serverRequestContext.get(), closeContext);
      assertFalse(contexts.contains(listenContext.get()));
      assertSame(serverRequestContext.get(), listenContext.get());
      testComplete();
    });

    server = null;
    await();
  }

  @Test
  public void testRequestHandlerNotCalledInvalidRequest() {
    server.requestHandler(req -> {
      fail();
    });
    server.listen(onSuccess(s -> {
      vertx.createNetClient(NetClientOptions.options()).connect(8080, "127.0.0.1", result -> {
        NetSocket socket = result.result();
        socket.closeHandler(r -> {
          testComplete();
        });
        socket.writeString("GET HTTP1/1\r\n");

        // trigger another write to be sure we detect that the other peer has closed the connection.
        socket.writeString("X-Header: test\r\n");
      });
    }));
    await();
  }

  private void pausingServer(Consumer<HttpServer> consumer) {
    server.requestHandler(req -> {
      req.response().setChunked(true);
      req.pause();
      Handler<Message<Buffer>> resumeHandler = msg -> req.resume();
      Registration reg = vertx.eventBus().registerHandler("server_resume", resumeHandler);
      req.endHandler(v -> reg.unregister());

      req.dataHandler(buff -> {
        req.response().writeBuffer(buff);
      });
    });

    server.listen(onSuccess(consumer));
  }

  private void drainingServer(Consumer<HttpServer> consumer) {
    server.requestHandler(req -> {
      req.response().setChunked(true);
      assertFalse(req.response().writeQueueFull());
      req.response().setWriteQueueMaxSize(1000);

      Buffer buff = TestUtils.randomBuffer(10000);
      //Send data until the buffer is full
      vertx.setPeriodic(1, id -> {
        req.response().writeBuffer(buff);
        if (req.response().writeQueueFull()) {
          vertx.cancelTimer(id);
          req.response().drainHandler(v -> {
            assertFalse(req.response().writeQueueFull());
            testComplete();
          });

          // Tell the client to resume
          vertx.eventBus().send("client_resume", "");
        }
      });
    });

    server.listen(onSuccess(consumer));
  }

  private static Headers getHeaders(int num) {
    Map<String, String> map = genMap(num);
    Headers headers = new HeadersAdaptor(new DefaultHttpHeaders());
    for (Map.Entry<String, String> entry : map.entrySet()) {
      headers.add(entry.getKey(), entry.getValue());
    }
    return headers;
  }

  private static Map<String, String> genMap(int num) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < num; i++) {
      String key;
      do {
        key = TestUtils.randomAlphaString(1 + (int) ((19) * Math.random())).toLowerCase();
      } while (map.containsKey(key));
      map.put(key, TestUtils.randomAlphaString(1 + (int) ((19) * Math.random())));
    }
    return map;
  }

  private static String generateQueryString(Map<String, String> params, char delim) {
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (Map.Entry<String, String> param : params.entrySet()) {
      sb.append(param.getKey()).append("=").append(param.getValue());
      if (++count != params.size()) {
        sb.append(delim);
      }
    }
    return sb.toString();
  }

  private File setupFile(String fileName, String content) throws Exception {
    File file = new File(testDir, fileName);
    if (file.exists()) {
      file.delete();
    }
    file.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
    out.write(content);
    out.close();
    return file;
  }
}
