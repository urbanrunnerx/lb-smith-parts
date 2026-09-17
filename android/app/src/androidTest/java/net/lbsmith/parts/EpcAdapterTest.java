package net.lbsmith.parts;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.content.Intent;
import android.webkit.WebView;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.io.*;

@RunWith(AndroidJUnit4.class)
public class EpcAdapterTest {
 private String js(ActivityScenario<EpcActivity> s,String code)throws Exception{CountDownLatch done=new CountDownLatch(1);AtomicReference<String> out=new AtomicReference<>();s.onActivity(a->a.getWebView().evaluateJavascript(code,v->{out.set(v);done.countDown();}));assertTrue(done.await(10,TimeUnit.SECONDS));return out.get();}
 private JSONObject read(ActivityScenario<EpcActivity> s,String adapter,String command,String vin)throws Exception{return new JSONObject(String.valueOf(new JSONTokener(js(s,"(()=>{"+adapter+";return JSON.stringify(lbEpc('"+command+"',{vin:'"+vin+"',base:'1104'}));})()")).nextValue()));}
 @Test public void adapterRejectsStaleVinAndReadsOnlyRequestedBase()throws Exception{
  String vin="1M8GDM9AXKP042788",adapter;
  try(InputStream in=ApplicationProvider.getApplicationContext().getAssets().open("epc-adapter.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);adapter=out.toString("UTF-8");}
  Intent i=new Intent(ApplicationProvider.getApplicationContext(),EpcActivity.class).putExtra("vin",vin).putExtra("requestId","fixture").putExtra("bases",new String[]{"1104"});
  try(ActivityScenario<EpcActivity> s=ActivityScenario.launch(i)){
   String fixture="<html><body><a id='toolbar-vin-url-anchor'>"+vin+"</a><div title='Toggle VIN filters on and off'><input type='checkbox' checked></div><span role='menuitem'>10 - Front Knuckle and Hub</span><div role='grid'><div role='row'><div role='columnheader'>Call/Base</div><div role='columnheader'>Part Description</div><div role='columnheader'>Part Number</div></div><div role='row'><div role='gridcell' col-id='calloutLabel'>1104</div><div role='gridcell' col-id='renderedDescription'>Fixture hub</div><div role='gridcell' col-id='formattedPartNumber'>TEST-1104-A</div></div><div role='row'><div role='gridcell' col-id='calloutLabel'></div><div role='gridcell' col-id='renderedDescription'></div><div role='gridcell' col-id='formattedPartNumber'>HUB-TEST</div></div><div role='row'><div role='gridcell' col-id='calloutLabel'>6731</div><div role='gridcell' col-id='renderedDescription'>Fixture filter</div><div role='gridcell' col-id='formattedPartNumber'>TEST-6731-A</div></div></div></body></html>";
   s.onActivity(a->{a.getWebView().stopLoading();a.getWebView().loadDataWithBaseURL("https://snaponepc.com/epc/",fixture,"text/html","UTF-8",null);});
   long end=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<end&&!"true".equals(js(s,"!!document.querySelector('#toolbar-vin-url-anchor')")))Thread.sleep(200);
   assertEquals("\"undefined\"",js(s,"typeof PartsNative"));
   JSONObject result=read(s,adapter,"capture",vin);assertEquals(2,result.getJSONArray("parts").length());assertEquals("TEST-1104-A",result.getJSONArray("parts").getJSONObject(0).getString("serviceNumber"));assertTrue(result.getString("context").contains("Front"));
   assertTrue(read(s,adapter,"capture","1M8GDM9A0KP042788").has("error"));
   js(s,"document.querySelector('input').checked=false");assertTrue(read(s,adapter,"capture",vin).has("error"));
  }
 }
}
