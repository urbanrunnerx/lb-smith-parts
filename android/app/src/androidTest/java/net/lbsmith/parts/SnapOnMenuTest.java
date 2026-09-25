package net.lbsmith.parts;

import android.webkit.JavascriptInterface;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real app menu and catalog search; the test bridge captures requests without opening EPC. */
@RunWith(AndroidJUnit4.class)
public class SnapOnMenuTest {
 public static final class RecordingBridge {
  final AtomicReference<String> payload=new AtomicReference<>();
  final AtomicInteger calls=new AtomicInteger();
  @JavascriptInterface public boolean isFixture(){return true;}
  @JavascriptInterface public void openEpc(String request){payload.set(request);calls.incrementAndGet();}
 }

 private static String js(ActivityScenario<MainActivity> scenario,String code)throws Exception{
  CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
  scenario.onActivity(a->a.getWebView().evaluateJavascript(code,value->{result.set(value);done.countDown();}));
  assertTrue("JavaScript callback timed out",done.await(10,TimeUnit.SECONDS));return result.get();
 }

 private static void until(ActivityScenario<MainActivity> scenario,String code,String expected)throws Exception{
  long deadline=System.currentTimeMillis()+30000;String latest="";
  while(System.currentTimeMillis()<deadline){latest=js(scenario,code);if(expected.equals(latest))return;Thread.sleep(100);}
  assertEquals(expected,latest);
 }

 private static void reload(ActivityScenario<MainActivity> scenario)throws Exception{
  js(scenario,"window.__beforeReload=true;location.reload()");
  until(scenario,"window.__beforeReload===undefined && document.querySelector('#baseCount')?.textContent==='15,700'","true");
 }

 private static JSONObject stored(ActivityScenario<MainActivity> scenario,String key)throws Exception{
  Object value=new JSONTokener(js(scenario,"localStorage.getItem('lbsmith-parts-v1-"+key+"')")).nextValue();
  return new JSONObject(String.valueOf(value));
 }

 @Test public void snapOnMenuGroupsCommonNamesAndValidatesBeforeNativeLookup()throws Exception{
  RecordingBridge bridge=new RecordingBridge();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   until(scenario,"document.querySelector('#baseCount')?.textContent","\"15,700\"");
   js(scenario,"localStorage.clear();localStorage.setItem('lbsmith-parts-v1-worksheet',JSON.stringify({job:'Existing fixture job',vin:'"+EpcAdapterTest.OTHER_VIN+"',rows:[{id:'fixture-existing',name:'Existing fixture part',bases:['6731'],qty:2,note:'Keep this note'}]}))");
   // addJavascriptInterface replaces the production bridge on the next page load.
   scenario.onActivity(a->a.getWebView().addJavascriptInterface(bridge,"PartsNative"));reload(scenario);
   assertEquals("true",js(scenario,"PartsNative.isFixture()"));
   String before=js(scenario,"localStorage.getItem('lbsmith-parts-v1-jobs')");
   assertEquals("true",js(scenario,"!!document.querySelector('#snaponButton') && !!document.querySelector('#mobileSnapon')"));
   js(scenario,"document.querySelector('#snaponButton').click()");
   until(scenario,"document.querySelector('#info').open && !!document.querySelector('#snaponForm')","true");
   assertEquals("true",js(scenario,"document.querySelector('#snaponLaunch').disabled"));
   js(scenario,"document.querySelector('#snaponQuery').value='purge valve';document.querySelector('#snaponQuery').dispatchEvent(new Event('input',{bubbles:true}))");
   assertEquals("One common part name should produce one family","1",js(scenario,"document.querySelectorAll('[data-snap-match]').length"));
   assertEquals("true",js(scenario,"document.querySelector('#snaponMatches').textContent.includes('9C915') && document.querySelector('#snaponMatches').textContent.includes('9D289')"));
   js(scenario,"document.querySelector('[data-snap-match]').click()");
   assertEquals("true",js(scenario,"[...document.querySelector('#snaponBase').options].some(o=>o.value==='9C915') && [...document.querySelector('#snaponBase').options].some(o=>o.value==='9D289')"));
   js(scenario,"document.querySelector('#snaponVin').value='INVALID';document.querySelector('#snaponLaunch').click()");
   assertEquals("true",js(scenario,"document.querySelector('#snaponStatus').textContent.includes('17-character VIN')"));
   assertEquals("Invalid input must not launch EPC",0,bridge.calls.get());
   assertEquals("Invalid VIN must not change the saved jobs",before,js(scenario,"localStorage.getItem('lbsmith-parts-v1-jobs')"));
   assertEquals("null",js(scenario,"localStorage.getItem('lbsmith-parts-v1-epc-pending')"));

   js(scenario,"document.querySelector('#snaponVin').value='"+EpcAdapterTest.VIN+"';document.querySelector('#snaponBase').value='9D289';document.querySelector('#snaponLaunch').click()");
   long deadline=System.currentTimeMillis()+10000;while(bridge.payload.get()==null&&System.currentTimeMillis()<deadline)Thread.sleep(50);
   assertNotNull("The valid menu request should reach the native bridge",bridge.payload.get());assertEquals(1,bridge.calls.get());
   JSONObject payload=new JSONObject(bridge.payload.get());assertEquals(EpcAdapterTest.VIN,payload.getString("vin"));
   assertEquals("9D289",payload.getJSONArray("bases").getString(0));assertTrue(payload.getJSONArray("bases").toString().contains("9C915"));
   assertTrue(payload.getString("partName").toLowerCase().contains("purge"));
   JSONObject book=stored(scenario,"jobs");JSONArray jobs=book.getJSONArray("items");assertEquals(2,jobs.length());
   JSONObject active=null,original=null;
   for(int n=0;n<jobs.length();n++){JSONObject job=jobs.getJSONObject(n);if(job.getString("id").equals(book.getString("activeId")))active=job;if(job.getString("vin").equals(EpcAdapterTest.OTHER_VIN))original=job;}
   assertNotNull(active);assertNotNull(original);assertEquals(EpcAdapterTest.VIN,active.getString("vin"));assertEquals(1,active.getJSONArray("rows").length());
   assertEquals("Keep this note",original.getJSONArray("rows").getJSONObject(0).getString("note"));
   JSONObject pending=stored(scenario,"epc-pending");assertEquals(payload.getString("requestId"),pending.getString("id"));
   assertEquals(active.getString("id"),pending.getString("jobId"));assertEquals("9D289",pending.getString("base"));

   js(scenario,"document.querySelector('#info').close();document.querySelector('#mobileSnapon').click()");
   assertEquals(JSONObject.quote(EpcAdapterTest.VIN),js(scenario,"document.querySelector('#snaponVin').value"));
   assertEquals("true",js(scenario,"document.querySelector('#snaponLaunch').disabled"));assertEquals(1,bridge.calls.get());
   js(scenario,"localStorage.clear()");reload(scenario);
  }
 }
}
