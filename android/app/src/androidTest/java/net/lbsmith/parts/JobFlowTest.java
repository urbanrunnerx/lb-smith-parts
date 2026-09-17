package net.lbsmith.parts;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.app.Activity;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class JobFlowTest {
 private String js(ActivityScenario<MainActivity> s,String code)throws Exception{CountDownLatch done=new CountDownLatch(1);AtomicReference<String> out=new AtomicReference<>();s.onActivity(a->a.getWebView().evaluateJavascript(code,v->{out.set(v);done.countDown();}));assertTrue(done.await(10,TimeUnit.SECONDS));return out.get();}
 private void until(ActivityScenario<MainActivity> s,String code,String expected)throws Exception{long end=System.currentTimeMillis()+30000;String last="";while(System.currentTimeMillis()<end){last=js(s,code);if(expected.equals(last))return;Thread.sleep(200);}assertEquals(expected,last);}
 @Test public void upgradeNativeReturnMultipleJobsAndVinChangesPersist()throws Exception{
  try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
   until(s,"document.querySelector('#baseCount')?.textContent","\"15,700\"");
   js(s,"localStorage.clear();localStorage.setItem('lbsmith-parts-v1-worksheet',JSON.stringify({job:'Legacy RO',vin:'1M8GDM9AXKP042788',rows:[{id:'fixture-hub',name:'Fixture hub',bases:['1104'],qty:2,note:'Preserved counter note'}]}));location.reload()");
   until(s,"document.querySelector('#vehicleBar')?.innerText.includes('Legacy RO')","true");
   js(s,"document.querySelector('#activeJob').click()");
   assertEquals("true",js(s,"document.querySelector('#jobRows').innerText.includes('Fixture hub')"));
   js(s,"const book=JSON.parse(localStorage.getItem('lbsmith-parts-v1-jobs'));localStorage.setItem('lbsmith-parts-v1-epc-pending',JSON.stringify({id:'fixture-request',jobId:book.activeId,rowId:'fixture-hub',vin:'1M8GDM9AXKP042788',base:'1104'}))");
   s.onActivity(a->a.onActivityResult(43,Activity.RESULT_OK,new Intent().putExtra("selection","{\"requestId\":\"fixture-request\",\"vin\":\"1M8GDM9AXKP042788\",\"base\":\"1104\",\"serviceNumber\":\"TEST-1104-A\",\"source\":\"Synthetic EPC fixture\",\"context\":\"Front fixture\"}")));
   until(s,"document.querySelector('#jobRows')?.innerText.includes('TEST-1104-A')","true");
   assertEquals("\"\"",js(s,"PartsNative.getEpcResult()"));
   js(s,"document.querySelector('#allJobs').click();document.querySelector('#newJob').click();document.querySelector('#jobName').value='Second job';document.querySelector('#jobName').dispatchEvent(new Event('input',{bubbles:true}));document.querySelector('#allJobs').click()");
   assertEquals("2",js(s,"document.querySelectorAll('[data-switch-job]').length"));
   js(s,"Array.from(document.querySelectorAll('[data-switch-job]')).find(b=>b.innerText.includes('Legacy RO')).click();document.querySelector('#jobVin').value='1M8GDM9A0KP042788';document.querySelector('#jobVin').dispatchEvent(new Event('input',{bubbles:true}))");
   assertEquals("true",js(s,"document.querySelector('.selection').innerText.includes('RECHECK')"));
   js(s,"location.reload()");until(s,"document.querySelector('#vehicleBar')?.innerText.includes('1M8GDM9A0KP042788')","true");
   js(s,"document.querySelector('#activeJob').click()");
   assertEquals("true",js(s,"document.querySelector('.selection').innerText.includes('RECHECK')"));
   assertEquals("\"Preserved counter note\"",js(s,"document.querySelector('[data-job-note2]').value"));
   js(s,"localStorage.clear();location.reload()");
  }
 }
}
