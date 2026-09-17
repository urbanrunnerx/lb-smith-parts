package net.lbsmith.parts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import android.view.View;
import org.json.*;
import java.nio.charset.StandardCharsets;

/** Separate remote catalog view; never exposes the local workspace JavaScript bridge. */
public class EpcActivity extends Activity {
 private WebView web; private TextView status; private Spinner bases; private String vin,requestId,adapter;
 public WebView getWebView(){return web;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);vin=getIntent().getStringExtra("vin");requestId=getIntent().getStringExtra("requestId");
  String[] values=getIntent().getStringArrayExtra("bases");
  if(vin==null||!vin.matches("[A-HJ-NPR-Z0-9]{17}")||values==null||values.length==0||values.length>100||requestId==null){finish();return;}
  for(String b:values)if(!b.matches("[0-9][A-Z0-9]{3,7}")){finish();return;}
  try(java.io.InputStream in=getAssets().open("epc-adapter.js");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);adapter=out.toString("UTF-8");}catch(Exception e){finish();return;}
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(16,29,50));
  root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
  TextView title=new TextView(this);title.setText("EPC LOOKUP  ·  "+vin);title.setTextColor(Color.WHITE);title.setTextSize(13);title.setPadding(16,10,16,4);root.addView(title);
  status=new TextView(this);status.setTextColor(Color.rgb(197,215,245));status.setTextSize(12);status.setPadding(16,4,16,8);status.setText("Sign in to Snap-on below. Load VIN → search a base → open a location → review parts.");root.addView(status);
  LinearLayout bar=new LinearLayout(this);bar.setPadding(8,0,8,4);bar.setBackgroundColor(Color.WHITE);
  bases=new Spinner(this);bases.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));bar.addView(bases,new LinearLayout.LayoutParams(0,48,1));
  addButton(bar,"Load VIN",()->action("vin"));addButton(bar,"Search",()->action("search"));addButton(bar,"Review parts",()->action("capture"));root.addView(bar);
  web=new WebView(this);web.setBackgroundColor(Color.WHITE);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
  // EPC is designed for a desktop-width catalog; allow pinch zoom on the phone.
  s.setUserAgentString(s.getUserAgentString().replace("; wv","").replace(" Mobile "," "));
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){
    Uri u=r.getUrl();if(allowed(u))return false;
    if(r.isForMainFrame()&&("https".equals(u.getScheme())||"http".equals(u.getScheme())))try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}
    return true;
   }
   @Override public void onReceivedError(WebView w,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())status.setText("EPC could not load. Check your connection, then reopen the lookup. Your job is saved.");}
  });
  root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout bottom=new LinearLayout(this);bottom.setBackgroundColor(Color.WHITE);addButton(bottom,"Back to job",this::finish);addButton(bottom,"Reload EPC",()->web.reload());root.addView(bottom);setContentView(root);
  if(state==null||web.restoreState(state)==null)web.loadUrl("https://snaponepc.com/epc/");
 }
 private boolean allowed(Uri u){String h=u.getHost();return "https".equals(u.getScheme())&&h!=null&&(h.equals("snaponepc.com")||h.endsWith(".snaponepc.com"));}
 private void addButton(LinearLayout bar,String label,Runnable run){Button b=new Button(this);b.setText(label);b.setTextSize(11);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(9,0,9,0);bar.addView(b,new LinearLayout.LayoutParams(-2,48));b.setOnClickListener(v->run.run());}
 private void action(String command){
  if(web.getUrl()==null||!allowed(Uri.parse(web.getUrl()))){status.setText("Open the signed-in Snap-on catalog first.");return;}
  try{JSONObject req=new JSONObject().put("vin",vin).put("base",bases.getSelectedItem().toString());
   String script="(()=>{"+adapter+";return JSON.stringify(lbEpc("+JSONObject.quote(command)+","+req+"));})()";
   web.evaluateJavascript(script,raw->{try{
    Object decoded=new JSONTokener(raw).nextValue();JSONObject result=new JSONObject(String.valueOf(decoded));
    if(result.has("error")){status.setText(result.getString("error"));return;}
    if(result.has("parts")){choose(result);return;}status.setText(result.optString("message","Ready."));
   }catch(Exception e){status.setText("EPC's page changed or is still loading. Use its controls, then try again.");}});
  }catch(Exception e){status.setText("Unable to start lookup.");}
 }
 private void choose(JSONObject result)throws JSONException{
  JSONArray parts=result.getJSONArray("parts");String[] choices=new String[parts.length()];
  for(int i=0;i<parts.length();i++){JSONObject p=parts.getJSONObject(i);choices[i]=p.getString("serviceNumber")+"\n"+p.optString("description");}
  new AlertDialog.Builder(this).setTitle("Select the required service part").setItems(choices,(d,n)->{try{
   JSONObject selected=parts.getJSONObject(n);String context=result.optString("context","");
   new AlertDialog.Builder(this).setTitle(selected.getString("serviceNumber")).setMessage("VIN: "+vin+"\nBase: "+selected.getString("base")+"\n\n"+context+"\n\n"+selected.optString("remarks")+"\n\nConfirm the position, restrictions and latest supersession in EPC. Save this selection to your job?")
    .setNegativeButton("Keep looking",null).setPositiveButton("Use this part",(dialog,which)->{
     // Recheck vehicle context after the review dialog; never return a stale vehicle's result.
     web.evaluateJavascript("(()=>{"+adapter+";return JSON.stringify(lbEpc('state',{vin:"+JSONObject.quote(vin)+"}));})()",raw->{try{
      JSONObject check=new JSONObject(String.valueOf(new JSONTokener(raw).nextValue()));if(!check.optBoolean("ready")){status.setText("The EPC vehicle or filters changed. Repeat the lookup.");return;}
      selected.put("context",context).put("source","Snap-on EPC — employee selected").put("requestId",requestId);
      setResult(RESULT_OK,new Intent().putExtra("selection",selected.toString()));finish();
     }catch(Exception e){status.setText("Could not recheck vehicle context. Repeat the lookup.");}});
    }).show();
  }catch(Exception e){status.setText("Unable to read the selected part.");}}).setNegativeButton("Cancel",null).show();
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);web.saveState(state);}
 @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
