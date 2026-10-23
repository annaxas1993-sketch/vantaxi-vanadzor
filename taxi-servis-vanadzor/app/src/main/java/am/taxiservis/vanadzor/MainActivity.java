package am.taxiservis.vanadzor;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    LinearLayout root;
    TextView status;
    EditText name, phone, pickup, destination, email, password;
    LocationManager lm;
    Location latest;
    String token = "";
    String userId = "";
    static final String SB_URL = "https://itcismqoozcevvrthgve.supabase.co";
    static final String SB_KEY = "sb_publishable_ZuV-MRbChdSIybvepuOz5g_jYEgW52n";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        android.os.StrictMode.setThreadPolicy(new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build());
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        home();
        askLocation();
    }
    void home() {
        root = new LinearLayout(this); root.setOrientation(1); root.setPadding(22,24,22,24); root.setBackgroundColor(Color.rgb(246,249,250));
        ScrollView s = new ScrollView(this); s.addView(root); setContentView(s);
        TextView brand = new TextView(this); brand.setText("🚕 TAXI SERVIS VANADZOR"); brand.setTextSize(23); brand.setTextColor(Color.rgb(0,119,128)); brand.setGravity(Gravity.CENTER); brand.setPadding(0,12,0,24); brand.setTypeface(null,1); root.addView(brand);
        TextView title = new TextView(this); title.setText(BuildConfig.APP_ROLE.equals("passenger") ? "Տաքսի պատվիրել" : "Վարորդի մուտք"); title.setTextSize(21); title.setTextColor(Color.DKGRAY); root.addView(title);
        status = new TextView(this); status.setTextColor(Color.rgb(160,55,40)); status.setPadding(0,16,0,8); root.addView(status);
        if(BuildConfig.APP_ROLE.equals("passenger")) {
            name=field("Ուղևորի անուն"); phone=field("Հեռախոսահամար"); pickup=field("Վերցնելու հասցե"); destination=field("Նպատակակետի հասցե");
            btn("Օգտագործել իմ GPS տեղադրությունը", () -> { askLocation(); if(latest!=null){pickup.setText(String.format(java.util.Locale.US,"%.6f, %.6f",latest.getLatitude(),latest.getLongitude())); msg("GPS կոորդինատները լրացվեցին։ Գրեք նաև հասկանալի հասցեն։");}else msg("Միացրեք հեռախոսի տեղադրությունը և թույլատրեք GPS-ը։"); });
            btn("Պատվիրել տաքսի", () -> order());
            msg("Վանաձոր · Առցանց պատվեր");
        } else {
            email=field("Վարորդի էլ․ փոստ"); password=field("Գաղտնաբառ");
            btn("Մուտք գործել", () -> login());
            btn("Թարմացնել պատվերները", () -> rides());
            btn("Ուղարկել իմ GPS տեղադրությունը", () -> sendLocation());
            msg("Մուտքը հասանելի է միայն դիսպետչերի կողմից ստեղծված վարորդի հաշվով։");
        }
    }
    EditText field(String h){ EditText e=new EditText(this); e.setSingleLine(true); e.setHint(h); root.addView(e,new LinearLayout.LayoutParams(-1,-2)); return e; }
    void btn(String t,Runnable r){ Button b=new Button(this); b.setText(t); root.addView(b,new LinearLayout.LayoutParams(-1,-2)); b.setOnClickListener(v->r.run()); }
    void msg(String m){status.setText(m);}
    void askLocation(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},44);return;}
        try { if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,10,new LocationListener(){public void onLocationChanged(Location l){latest=l;} public void onProviderEnabled(String p){} public void onProviderDisabled(String p){} public void onStatusChanged(String p,int s,android.os.Bundle b){} }); latest=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); } } catch(Exception e){msg("GPS-ը հասանելի չէ։");}
    }
    JSONObject call(String path,String method,JSONObject body,boolean auth)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(SB_URL+path).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(10000); c.setReadTimeout(15000);
        c.setRequestProperty("apikey",SB_KEY); c.setRequestProperty("Authorization","Bearer "+(auth&&!token.isEmpty()?token:SB_KEY)); c.setRequestProperty("Content-Type","application/json"); c.setRequestProperty("Accept","application/json"); c.setRequestProperty("Prefer","return=representation");
        if(body!=null){c.setDoOutput(true);try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode(); InputStream in=code<400?c.getInputStream():c.getErrorStream(); ByteArrayOutputStream out=new ByteArrayOutputStream(); if(in!=null){byte[] buf=new byte[2048];int n;while((n=in.read(buf))>0)out.write(buf,0,n);} String text=out.toString("UTF-8"); c.disconnect();
        if(code<200||code>=300)throw new Exception("Սերվեր "+code+": "+text);
        if(text.trim().startsWith("[")){JSONObject x=new JSONObject();x.put("data",new JSONArray(text));return x;} return text.trim().isEmpty()?new JSONObject():new JSONObject(text);
    }
    void order(){
        try {
            if(name.getText().toString().trim().isEmpty()||phone.getText().toString().trim().isEmpty()||pickup.getText().toString().trim().isEmpty()||destination.getText().toString().trim().isEmpty()){msg("Լրացրեք անունը, հեռախոսը և երկու հասցեները։");return;}
            JSONObject b=new JSONObject();b.put("p_customer_name",name.getText().toString().trim());b.put("p_customer_phone",phone.getText().toString().trim());b.put("p_pickup_address",pickup.getText().toString().trim());b.put("p_destination_address",destination.getText().toString().trim());b.put("p_customer_note","");
            call("/rest/v1/rpc/submit_ride_order","POST",b,false);msg("Պատվերն ուղարկվեց։ Դիսպետչերը կկապվի ձեզ հետ։");
        }catch(Exception e){msg("Պատվերը չուղարկվեց։ Ստուգեք կապը կամ դիմեք դիսպետչերին։ "+e.getMessage());}
    }
    void login(){
        try{
            JSONObject b=new JSONObject();b.put("email",email.getText().toString().trim());b.put("password",password.getText().toString());
            JSONObject r=call("/auth/v1/token?grant_type=password","POST",b,false);token=r.optString("access_token");userId=r.optJSONObject("user")==null?"":r.optJSONObject("user").optString("id");
            if(token.isEmpty())throw new Exception("Մուտքը չհաջողվեց");
            JSONObject p=call("/rest/v1/profiles?id=eq."+URLEncoder.encode(userId,"UTF-8")+"&select=role,active","GET",null,true);JSONArray a=p.optJSONArray("data");
            if(a==null||a.length()==0||!a.getJSONObject(0).optString("role").equals("driver")||!a.getJSONObject(0).optBoolean("active"))throw new Exception("Այս հաշիվը հաստատված վարորդ չէ։");
            msg("Մուտքը հաջողվեց։");rides();
        }catch(Exception e){token="";msg("Մուտքը չհաջողվեց. "+e.getMessage());}
    }
    void rides(){
        if(token.isEmpty()){msg("Նախ մուտք գործեք։");return;}
        try{
            JSONObject r=call("/rest/v1/rides?select=id,pickup_address,destination_address,status,created_at&status=in.(requested,assigned,accepted)&order=created_at.desc","GET",null,true);
            JSONArray a=r.optJSONArray("data"); StringBuilder text=new StringBuilder("Պատվերներ՝\n");
            if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);text.append("\n• ").append(x.optString("pickup_address")).append(" → ").append(x.optString("destination_address")).append(" [").append(x.optString("status")).append("]\n");}
            msg(text.toString());
        }catch(Exception e){msg("Պատվերները բեռնել չհաջողվեց։ "+e.getMessage());}
    }
}
