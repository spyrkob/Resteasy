package org.jboss.resteasy.test.client.exception;

import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.exception.ResteasyWebApplicationException;
import org.jboss.resteasy.test.client.exception.resource.ClientWebApplicationExceptionExceptions;
import org.jboss.resteasy.test.client.exception.resource.ClientWebApplicationExceptionMicroProfileProxyApplication;
import org.jboss.resteasy.test.client.exception.resource.ClientWebApplicationExceptionMicroProfileProxyResource;
import org.jboss.resteasy.test.client.exception.resource.ClientWebApplicationExceptionProxyResourceInterface;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client throws ResteasyWebApplicationException on server side
 * @tpSince RESTEasy 3.11.3.Final
 * @tpTestCaseDetails Test ResteasyWebApplicationException and WebApplicationException in various circumstances,
 *                    calls made by MicroProfile REST Client proxies.
 *
 * NOTE. Unlike RESTEasy Clients and RESTEasy Client proxies, which throws the subtree of WebApplicationExceptions
 *       and ResteasyWebApplicationExceptions, MicroProfile REST Client proxies throw only WebApplicationExceptions
 *       and ResteasyWebApplicationExceptions.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientWebApplicationExceptionMicroProfileProxyNewBehaviorTest {

   private static ClientWebApplicationExceptionProxyResourceInterface proxy;

   static {
      try {
         proxy = RestClientBuilder.newBuilder()
               .baseUri(new URI(ClientWebApplicationExceptionExceptions.generateURL("/app/test/")))
               .build(ClientWebApplicationExceptionProxyResourceInterface.class);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }


   @Deployment
   public static Archive<?> deploy() {
      WebArchive war = TestUtil.prepareArchive("ClientWebApplicationExceptionTest");
      war.addClass(ClientWebApplicationExceptionMicroProfileProxyApplication.class);
      war.addClass(ClientWebApplicationExceptionMicroProfileProxyResource.class);
      war.addClass(ClientWebApplicationExceptionProxyResourceInterface.class);
      war.addClass(ClientWebApplicationExceptionExceptions.class);
      war.addClass(PortProviderUtil.class);
      war.addClass(TestUtil.class);
      war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      return TestUtil.finishContainerPrepare(war, null, ClientWebApplicationExceptionMicroProfileProxyResource.class);
   }

   ////////////////////////////////////////////////////////////////////////////////////////////
   /**
    * @tpTestDetails For each ResteasyWebApplicationException in newExceptions, calls the resource method newException() to throw
    *                that ResteasyWebApplicationException. Since it is running on the client side, the standard behavior of throwing a
    *                WebApplicationException will occur. That WebApplicationException should have a status of 500 and an entity representing a stacktrace.
    *
    * @tpSince RESTEasy 3.11.3.Final
    */
   @Test
   public void testNewExceptionsDirectly() throws Exception {
      for (int i = 0; i < ClientWebApplicationExceptionExceptions.newExceptions.length; i++) {
         try {
            proxy.newException(i);
            Assert.fail("expected exception");
         } catch (ResteasyWebApplicationException rwae) {
            Assert.fail("Didn't expect ResteasyWebApplicationException");
         } catch (WebApplicationException e) {
            final String msg = String.format("Failed on %d: %s", i, ClientWebApplicationExceptionExceptions.newExceptions[i].getResponse());
            Response response = e.getResponse();
            Assert.assertEquals(msg, ClientWebApplicationExceptionExceptions.newExceptions[i].getResponse().getStatus(), response.getStatus());
            Assert.assertNull(msg, response.getHeaderString("foo"));
            Assert.assertTrue(msg, response.readEntity(String.class).isEmpty());
            Assert.assertEquals(msg, WebApplicationException.class, e.getClass());
         }
      }
   }

   /**
    * @tpTestDetails 1. For each WebApplicationException in oldExceptions, the resource method noCatchOld() is called.
    *
    *                2. noCatchOld() calls oldException(), which throws the matching member of oldExceptions. The resulting
    *                   HTTP response contains the status, headers, and entity in that WebApplicationException.
    *
    *                3. In noCatchOld(), the new behavior causes the HTTP response to be turned into a ResteasyWebApplicationException,
    *                   which is thrown by the Client. ResteasyWebApplicationException.getResponse() returns null, so the resulting
    *                   HTTP response has status 500, no added headers, and an entity that represents a stacktrace.
    *
    *                4. The client side Client constructs and throws a WebApplicationException which is checked for status 500, no
    *                   added headers, and entity representing a stacktrace.
    *
    * @tpSince RESTEasy 3.11.3.Final
    */
   @Test
   public void testNoCatchNewBehaviorOldExceptions() throws Exception {
      for (int i = 0; i < ClientWebApplicationExceptionExceptions.oldExceptions.length; i++) {
         try {
            proxy.noCatchOld(i);
            Assert.fail("expected exception");
         } catch (ResteasyWebApplicationException e) {
            Assert.fail("didn't expect ResteasyWebApplicationException");
         } catch (WebApplicationException e) {
            Response response = e.getResponse();
            Assert.assertEquals(ClientWebApplicationExceptionExceptions.oldExceptions[i].getResponse().getStatus(), response.getStatus());
            Assert.assertNull(response.getHeaderString("foo"));
            Assert.assertEquals(WebApplicationException.class, e.getClass());
         } catch (Exception e) {
            Assert.fail("expected WebApplicationException");
         }
      }
   }

   /**
    * @tpTestDetails 1. For each ResteasyWebApplicationException in newExceptions, the resource method noCatchNew() is called.
    *
    *                2. noCatchNew() calls newException(), which throws the matching member of newExceptions. Since
    *                   ResteasyWebApplicationException.getResponse() returns null, the resulting
    *                   HTTP response has status 500, no added headers, and an entity that represents a stacktrace.
    *
    *                3. In noCatchNew(), the new behavior causes the HTTP response to be turned into a ResteasyWebApplicationException,
    *                   which is thrown by the Client. The resulting  HTTP response has status 500, no added headers, and an entity
    *                   that represents a stacktrace.
    *
    *                4. The client side Client constructs and throws a WebApplicationException which is checked for status 500, no
    *                   added headers, and entity representing a stacktrace.
    *
    * @tpSince RESTEasy 3.11.3.Final
    */
   @Test
   public void testNoCatchNewBehaviorNewExceptions() throws Exception {
      for (int i = 0; i < ClientWebApplicationExceptionExceptions.newExceptions.length; i++) {
         try {
            proxy.noCatchNew(i);
            Assert.fail("expected exception");
         } catch (ResteasyWebApplicationException e) {
            Assert.fail("didn't expect ResteasyWebApplicationException");
         } catch (WebApplicationException e) {
            Response response = e.getResponse();
            Assert.assertEquals(ClientWebApplicationExceptionExceptions.newExceptions[i].getResponse().getStatus(), response.getStatus());
            Assert.assertNull(response.getHeaderString("foo"));
            Assert.assertTrue(response.readEntity(String.class).isEmpty());
            Assert.assertEquals(WebApplicationException.class, e.getClass());
         } catch (Exception e) {
            Assert.fail("expected WebApplicationException");
         }
      }
   }

   /**
    * @tpTestDetails 1. For each WebApplicationException in oldExceptions, the resource method catchNewOld() is called.
    *
    *                2. catchNewOld() calls oldException(), which throws the matching member of oldExceptions. The resulting
    *                   HTTP response contains the status, headers, and entity in that WebApplicationException.
    *
    *                3. In catchNewOld(), the new behavior causes the HTTP response to be turned into a ResteasyWebApplicationException,
    *                   which is thrown by the Client, caught, tested, and rethrown. The ResteasyWebApplicationException.getOriginalResponse()
    *                   should match the Response in the matching WebApplicationException. The HTTP response constructed from the rethrown
    *                   ResteasyWebApplicationException will have status 500, no added headers, and an entity that represents a
    *                   stacktrace.
    *
    *                4. The client side Client constructs and throws a WebApplicationException which is checked for status 500, no
    *                   added headers, and entity representing a stacktrace.
    *
    * @tpSince RESTEasy 3.11.3.Final
    */
   @Test
   public void testCatchNewBehaviorOldExceptions() throws Exception {
      for (int i = 0; i < ClientWebApplicationExceptionExceptions.oldExceptions.length; i++) {
         try {
            proxy.catchNewOld(i);
            Assert.fail("expected exception");
         } catch (ResteasyWebApplicationException e) {
            Assert.fail("didn't expect ResteasyWebApplicationException");
         } catch (WebApplicationException e) {
            Response response = e.getResponse();
            Assert.assertNotNull(response);
            Assert.assertEquals(500, response.getStatus());
            Assert.assertNull(response.getHeaderString("foo"));
            Assert.assertTrue(response.readEntity(String.class).contains("Caused by"));
            Assert.assertEquals(WebApplicationException.class, e.getClass());
         } catch (Exception e) {
            Assert.fail("expected WebApplicationException");
         }
      }
   }

   /**
    * @tpTestDetails 1. For each ResteasyWebApplicationException in newExceptions, the resource method catchNewNew() is called.
    *
    *                2. catchNewNew() calls newException(), which throws the matching member of newExceptions. The resulting
    *                   HTTP response will have status 500, no added headers, and an entity that represents a
    *                   stacktrace.
    *
    *                3. In catchNewNew(), the new behavior causes the HTTP response to be turned into a ResteasyWebApplicationException,
    *                   which is thrown by the Client, caught, tested, and rethrown. The ResteasyWebApplicationException.getOriginalResponse()
    *                   should have status 500, no added headers, and an entity that represents a stacktrace. The HTTP response constructed
    *                   from the rethrown ResteasyWebApplicationException will have status 500, no added headers, and an entity
    *                   that represents a stacktrace.
    *
    *                4. The client side Client constructs and throws a WebApplicationException which is checked for status 500, no
    *                   added headers, and entity representing a stacktrace.
    *
    * @tpSince RESTEasy 3.11.3.Final
    */
   @Test
   public void testCatchNewBehaviorNewExceptions() throws Exception {
      for (int i = 0; i < ClientWebApplicationExceptionExceptions.newExceptions.length; i++) {
         try {
            proxy.catchNewNew(i);
            Assert.fail("expected exception");
         } catch (ResteasyWebApplicationException e) {
            Assert.fail("didn't expect ResteasyWebApplicationException");
         } catch (WebApplicationException e) {
            Response response = e.getResponse();
            Assert.assertNotNull(response);
            Assert.assertEquals(500, response.getStatus());
            Assert.assertNull(response.getHeaderString("foo"));
            Assert.assertTrue(response.readEntity(String.class).contains("Caused by"));
            Assert.assertEquals(WebApplicationException.class, e.getClass());
         } catch (Exception e) {
            Assert.fail("expected WebApplicationException");
         }
      }
   }
}