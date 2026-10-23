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
    LinearLayout rideList;
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
        TextView title = new TextView(this); title.setText(BuildConfig.APP_ROLE.equals("passenger") ? "Տաքսի պատվիրել" : (BuildConfig.APP_ROLE.equals("dispatcher") ? "Դիսպետչերական վահանակ" : "Վարորդի մուտք")); title.setTextSize(21); title.setTextColor(Color.DKGRAY); root.addView(title);
        status = new TextView(this); status.setTextColor(Color.rgb(160,55,40)); status.setPadding(0,16,0,8); root.addView(status);
        if(BuildConfig.APP_ROLE.equals("passenger")) {
            name=field("Ուղևորի անուն"); phone=field("Հեռախոսահամար"); pickup=field("Վերցնելու հասցե"); destination=field("Նպատակակետի հասցե");
            btn("Օգտագործել իմ GPS տեղադրությունը", () -> { askLocation(); if(latest!=null){pickup.setText(String.format(java.util.Locale.US,"%.6f, %.6f",latest.getLatitude(),latest.getLongitude())); msg("GPS կոորդինատները լրացվեցին։ Գրեք նաև հասկանալի հասցեն։");}else msg("Միացրեք հեռախոսի տեղադրությունը և թույլատրեք GPS-ը։"); });
            btn("Պատվիրել տաքսի", () -> order());
            msg("Վանաձոր · Առցանց պատվեր");
        } else {
            email=field(BuildConfig.APP_ROLE.equals("dispatcher") ? "Դիսպետչերի էլ․ փոստ" : "Վարորդի էլ․ փոստ"); password=field("Գաղտնաբառ");
            btn("Մուտք գործել", () -> login());
            btn("Թարմացնել պատվերները", () -> rides());
            if(BuildConfig.APP_ROLE.equals("driver")) btn("Ուղարկել իմ GPS տեղադրությունը", () -> sendLocation());
            msg(BuildConfig.APP_ROLE.equals("dispatcher") ? "Մուտքը հասանելի է միայն հաստատված դիսպետչերի հաշվով։" : "Մուտքը հասանելի է միայն դիսպետչերի կողմից ստեղծված վարորդի հաշվով։");
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
        if(text.trim().startsWith("[")){JSONObject x=new JSONObject();x.put("data",new JSONArray(text));return x;}
        if(text.trim().startsWith("{"))return new JSONObject(text);
        if(text.trim().isEmpty())return new JSONObject();
        JSONObject scalar=new JSONObject(); scalar.put("value",new JSONTokener(text).nextValue()); return scalar;
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
            String requiredRole=BuildConfig.APP_ROLE.equals("dispatcher") ? "dispatcher" : "driver";
            if(a==null||a.length()==0||!(a.getJSONObject(0).optString("role").equals(requiredRole) || (BuildConfig.APP_ROLE.equals("dispatcher") && a.getJSONObject(0).optString("role").equals("admin")))||!a.getJSONObject(0).optBoolean("active"))throw new Exception(BuildConfig.APP_ROLE.equals("dispatcher") ? "Այս հաշիվը հաստատված դիսպետչեր չէ։" : "Այս հաշիվը հաստատված վարորդ չէ։");
            msg("Մուտքը հաջողվեց։");rides();
        }catch(Exception e){token="";msg("Մուտքը չհաջողվեց. "+e.getMessage());}
    }
    void rides(){
        if(token.isEmpty()){msg("Նախ մուտք գործեք։");return;}
        try{
            String rideQuery="/rest/v1/rides?select=id,pickup_address,destination_address,status,assigned_driver,created_at&status=in.(new,assigned,accepted,approaching,with_passenger)&order=created_at.desc";
            JSONObject r=call(rideQuery,"GET",null,true);
            JSONArray a=r.optJSONArray("data");
            if(rideList!=null) root.removeView(rideList);
            LinearLayout list=new LinearLayout(this); list.setOrientation(1); rideList=list;
            TextView title=new TextView(this); title.setText((BuildConfig.APP_ROLE.equals("dispatcher") ? "Դիսպետչերական պատվերներ՝ " : "Պատվերներ՝ ")+(a==null?0:a.length())); title.setTextSize(18); list.addView(title);
            if(a!=null) for(int i=0;i<a.length();i++){
                JSONObject x=a.getJSONObject(i); String id=x.optString("id"); String st=x.optString("status");
                TextView details=new TextView(this); details.setText("\n"+x.optString("pickup_address")+" → "+x.optString("destination_address")+"\nԿարգավիճակ՝ "+st); details.setTextSize(16); details.setPadding(8,12,8,4); list.addView(details);
                if(BuildConfig.APP_ROLE.equals("dispatcher")) {
                    TextView meta=new TextView(this); meta.setText("Պատվերի ID՝ "+id+"\nՎարորդի ID՝ "+(x.isNull("assigned_driver") ? "Չի նշանակվել" : x.optString("assigned_driver"))+"\nՍտեղծվել է՝ "+x.optString("created_at")); meta.setTextSize(13); meta.setPadding(8,0,8,12); list.addView(meta);
                } else if(st.equals("new")){
                    Button take=new Button(this); take.setText("Ընդունել ազատ պատվերը"); list.addView(take);
                    take.setOnClickListener(v -> claimRide(id,take));
                } else if(st.equals("assigned") && x.optString("assigned_driver").equals(userId)){
                    Button take=new Button(this); take.setText("Հաստատել ինձ նշանակված պատվերը"); list.addView(take);
                    take.setOnClickListener(v -> acceptAssignedRide(id,take));
                } else if(st.equals("accepted") && x.optString("assigned_driver").equals(userId)){
                    rideStatusButton(list,id,"Մոտենում եմ ուղևորին","approaching");
                } else if(st.equals("approaching") && x.optString("assigned_driver").equals(userId)){
                    rideStatusButton(list,id,"Ուղևորը նստել է","with_passenger");
                } else if(st.equals("with_passenger") && x.optString("assigned_driver").equals(userId)){
                    rideStatusButton(list,id,"Ավարտել ուղևորությունը","completed");
                }
            }
            root.addView(list);
            msg("Պատվերների ցանկը թարմացվեց։");
        }catch(Exception e){msg("Պատվերները բեռնել չհաջողվեց։");}
    }
    void rideStatusButton(LinearLayout list,String rideId,String label,String nextStatus){
        Button b=new Button(this); b.setText(label); list.addView(b);
        b.setOnClickListener(v -> {
            try{
                JSONObject args=new JSONObject(); args.put("p_ride_id",rideId); args.put("p_status",nextStatus);
                JSONObject result=call("/rest/v1/rpc/update_vantaxi_ride_status","POST",args,true);
                if(result.optBoolean("value",false)){
                    b.setEnabled(false); b.setText("Կարգավիճակը թարմացվեց");
                    msg("Պատվերի կարգավիճակը թարմացվեց։ Սեղմեք «Թարմացնել պատվերները»։");
                } else msg("Կարգավիճակը չփոխվեց։ Թարմացրեք ցանկը։");
            }catch(Exception e){msg("Կարգավիճակը չփոխվեց։ Ստուգեք կապը կամ դիմեք դիսպետչերին։");}
        });
    }
    void acceptAssignedRide(String rideId, Button b){
        try{
            JSONObject args=new JSONObject(); args.put("p_ride_id",rideId); args.put("p_status","accepted");
            call("/rest/v1/rpc/update_vantaxi_ride_status","POST",args,true);
            b.setEnabled(false); b.setText("Ընդունված է"); msg("Նշանակված պատվերը հաստատվեց։ Թարմացրեք ցանկը։");
        }catch(Exception e){msg("Պատվերը չհաստատվեց։ Ստուգեք կապը կամ դիմեք դիսպետչերին։");}
    }
    void claimRide(String rideId, Button b){
        try{
            JSONObject args=new JSONObject(); args.put("p_ride_id",rideId);
            call("/rest/v1/rpc/claim_vantaxi_ride","POST",args,true);
            b.setEnabled(false); b.setText("Ընդունված է"); msg("Պատվերն ընդունվեց։ Թարմացրեք ցանկը։");
        }catch(Exception e){msg("Պատվերը չընդունվեց։ Հնարավոր է՝ այն արդեն վերցրել է այլ վարորդ։");}
    }
    void sendLocation(){
        try{
            askLocation();
            if(latest==null){msg("GPS տեղադրությունը դեռ հասանելի չէ։ Միացրեք GPS-ը և փորձեք կրկին։");return;}
            JSONObject b=new JSONObject(); b.put("p_latitude",latest.getLatitude()); b.put("p_longitude",latest.getLongitude());
            JSONObject result=call("/rest/v1/rpc/update_vantaxi_driver_location","POST",b,true);
            if(!result.optBoolean("value",false)) throw new Exception("Տեղադրությունը չի պահպանվել");
            msg("GPS տեղադրությունն ուղարկվեց։");
        }catch(Exception e){msg("GPS-ը չուղարկվեց։ Ստուգեք կապը և թույլտվությունը։");}
    }
}
