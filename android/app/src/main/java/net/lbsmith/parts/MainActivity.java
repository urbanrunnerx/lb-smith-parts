package net.lbsmith.parts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
 public static final String ORIGIN="https://appassets.androidplatform.net";
 private WebView web;
 private WebView printWeb;
 private ValueCallback<Uri[]> fileCallback;
 private String exportText;
 private final ExecutorService decoderExecutor=Executors.newSingleThreadExecutor();
 private static final int PICK_FILE=41, SAVE_FILE=42, EPC_LOOKUP=43;
 public WebView getWebView(){return web;}

 @SuppressLint({"SetJavaScriptEnabled"})
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  web=new WebView(this);
  web.setBackgroundColor(0xfff6f8fb);
  web.setOnApplyWindowInsetsListener((v,insets)->{
   v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   return insets;
  });
  setContentView(web);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
  s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  final WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.addJavascriptInterface(new LocalTools(),"PartsNative");
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
    WebResourceResponse response=loader.shouldInterceptRequest(request.getUrl());
    return response!=null?response:new WebResourceResponse("text/plain","UTF-8",403,"Forbidden",null,new ByteArrayInputStream(new byte[0]));
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    Uri u=request.getUrl();
    if("https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/assets/"))return false;
    if("https".equals(u.getScheme())||"http".equals(u.getScheme())){
     try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){toast("No browser is available to open this link.");}
    }
    return true;
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){
    if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/comma-separated-values","application/json","text/plain"});
    try{startActivityForResult(i,PICK_FILE);}catch(Exception e){fileCallback.onReceiveValue(null);fileCallback=null;toast("No file picker is available.");}
    return true;
   }
  });
  if(state==null||web.restoreState(state)==null)web.loadUrl(ORIGIN+"/assets/index.html");
 }
 private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
 public final class LocalTools {
  @JavascriptInterface public void printJob(String text){if(text==null||text.length()>200000)return;runOnUiThread(()->{if(printWeb!=null)printWeb.destroy();printWeb=new WebView(MainActivity.this);printWeb.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String u){android.print.PrintManager pm=(android.print.PrintManager)getSystemService(PRINT_SERVICE);pm.print("L.B. Smith Parts job",v.createPrintDocumentAdapter("L.B. Smith Parts job"),new android.print.PrintAttributes.Builder().build());}});printWeb.loadDataWithBaseURL(null,"<html><head><meta charset='utf-8'><style>body{font:12px sans-serif;padding:24px}pre{white-space:pre-wrap;line-height:1.6}</style></head><body><h2>L.B. Smith Parts</h2><pre>"+android.text.TextUtils.htmlEncode(text)+"</pre></body></html>","text/html","UTF-8",null);});}
  @JavascriptInterface public void openEpc(String payload){
   try{JSONObject p=new JSONObject(payload);String vin=p.getString("vin"),id=p.getString("requestId");org.json.JSONArray a=p.getJSONArray("bases");
    if(!vin.matches("[A-HJ-NPR-Z0-9]{17}")||!id.matches("[A-Za-z0-9|_-]{1,180}")||a.length()<1||a.length()>100)return;
    String[] bases=new String[a.length()];for(int n=0;n<a.length();n++){bases[n]=a.getString(n);if(!bases[n].matches("[0-9][A-Z0-9]{3,7}"))return;}
    String partName=p.optString("partName","Service part lookup");if(partName.length()>160)partName=partName.substring(0,160);final String title=partName;
    runOnUiThread(()->startActivityForResult(new Intent(MainActivity.this,EpcActivity.class).putExtra("vin",vin).putExtra("requestId",id).putExtra("bases",bases).putExtra("partName",title),EPC_LOOKUP));
   }catch(Exception e){runOnUiThread(()->toast("Unable to open the EPC lookup."));}
  }
  @JavascriptInterface public String getEpcResult(){return getSharedPreferences("epc-results",MODE_PRIVATE).getString("pending","");}
  @JavascriptInterface public void acknowledgeEpcResult(){getSharedPreferences("epc-results",MODE_PRIVATE).edit().remove("pending").apply();}
  @JavascriptInterface public void decodeVin(String vin,String year,String id){
   if(vin==null||!vin.matches("[A-HJ-NPR-Z0-9]{17}")||year==null||!year.matches("(?:[0-9]{4})?")||id==null||!id.matches("[0-9-]{1,40}"))return;
   decoderExecutor.execute(()->{
    JSONObject result=new JSONObject();HttpsURLConnection connection=null;
    try{
     result.put("id",id);
     String address="https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValuesExtended/"+vin+"?format=json"+(year.isEmpty()?"":"&modelyear="+year);
     connection=(HttpsURLConnection)new URL(address).openConnection();connection.setConnectTimeout(7000);connection.setReadTimeout(10000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("Accept","application/json");
     if(connection.getResponseCode()!=200)throw new java.io.IOException("Service temporarily unavailable");
     ByteArrayOutputStream output=new ByteArrayOutputStream();
     try(InputStream input=connection.getInputStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){output.write(buffer,0,count);if(output.size()>1000000)throw new java.io.IOException("Response too large");}}
     byte[] bytes=output.toByteArray();
     result.put("data",new JSONObject(new String(bytes,StandardCharsets.UTF_8)));
    }catch(Exception e){try{result.put("error","Vehicle lookup unavailable. Check your internet connection and try again.");}catch(Exception ignored){}}
    finally{if(connection!=null)connection.disconnect();}
    runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())web.evaluateJavascript("window.dispatchEvent(new CustomEvent('parts-vin-result',{detail:"+result.toString()+"}));",null);});
   });
  }
  @JavascriptInterface public void copyText(String text){if(text==null||text.length()>1000000)return;runOnUiThread(()->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("L.B. Smith Parts",text));});}
  @JavascriptInterface public void saveText(String name,String text,String mime){
   if(text==null||text.length()>20000000||name==null||!name.matches("[A-Za-z0-9._ -]{1,100}"))return;
   if(!"text/csv".equals(mime)&&!"application/json".equals(mime)&&!"text/plain".equals(mime))return;
   runOnUiThread(()->{if(exportText!=null){toast("Finish the current file save first.");return;}exportText=text;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(mime);intent.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(intent,SAVE_FILE);}catch(Exception e){exportText=null;toast("Unable to open the file save picker.");}});
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==EPC_LOOKUP&&result==RESULT_OK&&data!=null){String selected=data.getStringExtra("selection");if(selected!=null&&selected.length()<12000){getSharedPreferences("epc-results",MODE_PRIVATE).edit().putString("pending",selected).apply();web.evaluateJavascript("window.dispatchEvent(new Event('parts-epc-result'));",null);}}
  if(request==PICK_FILE&&fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);fileCallback=null;}
  if(request==SAVE_FILE){String text=exportText;exportText=null;if(result==RESULT_OK&&text!=null&&data!=null&&data.getData()!=null){try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){if(stream==null)throw new java.io.IOException();stream.write(text.getBytes(StandardCharsets.UTF_8));toast("File saved.");}catch(Exception e){toast("The file could not be saved.");}}}
 }
 @Override public void onBackPressed(){web.evaluateJavascript("(()=>{const d=document.querySelector('dialog[open]');if(d){d.close();return true;}return false;})()",value->{if(!"true".equals(value)){if(web.canGoBack())web.goBack();else super.onBackPressed();}});}
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);web.saveState(state);}
 @Override protected void onDestroy(){decoderExecutor.shutdownNow();if(fileCallback!=null)fileCallback.onReceiveValue(null);if(printWeb!=null)printWeb.destroy();web.removeJavascriptInterface("PartsNative");web.destroy();super.onDestroy();}
}
