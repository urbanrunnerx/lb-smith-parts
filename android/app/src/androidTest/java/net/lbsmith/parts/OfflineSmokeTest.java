package net.lbsmith.parts;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class OfflineSmokeTest {
 private String js(ActivityScenario<MainActivity> scenario,String code)throws Exception{
  CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
  scenario.onActivity(a->a.getWebView().evaluateJavascript(code,v->{result.set(v);done.countDown();}));
  assertTrue("JavaScript callback timed out",done.await(10,TimeUnit.SECONDS));return result.get();
 }
 private void until(ActivityScenario<MainActivity> s,String code,String expected)throws Exception{
  long end=System.currentTimeMillis()+30000;String last="";
  while(System.currentTimeMillis()<end){last=js(s,code);if(expected.equals(last))return;Thread.sleep(250);}
  assertEquals(expected,last);
 }
 @Test public void offlineCatalogAndGroupedSearchLoadInAndroid()throws Exception{
  try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
   until(s,"document.querySelector('#baseCount')?.textContent","\"15,700\"");
   assertEquals("\"object\"",js(s,"typeof PartsNative"));
   js(s,"document.querySelector('[data-query=\"purge valve\"]').click()");
   until(s,"document.querySelectorAll('#results .part-card').length","1");
   assertEquals("true",js(s,"document.querySelector('#results').innerText.includes('9C915') && document.querySelector('#results').innerText.includes('9D289')"));
   js(s,"document.querySelector('#results [data-detail]').click()");
   until(s,"document.querySelector('#detail').open","true");
   js(s,"document.querySelector('[data-add-job]').click()");
   assertEquals("1",js(s,"JSON.parse(localStorage.getItem('lbsmith-parts-v1-worksheet')).rows.length"));
  }
 }
}
