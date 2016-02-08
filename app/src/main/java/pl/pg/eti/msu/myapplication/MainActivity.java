package pl.pg.eti.msu.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.*;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GeoProject";
    private static final String NAMESPACE = "http://geoproject_webservice/";
    // punkty in - punkty out
    // private static final String METHOD_NAME_PX = "getImageFragmentPixel";
    // private static final String SOAP_ACTION_PX = NAMESPACE + "/getImageFragmentPixel";
    //minimapka
    private static final String METHOD_NAME_MINIMAP = "getMiniMap";
    private static final String SOAP_ACTION_MINIMAP = NAMESPACE + "getMiniMap";
    // fragment mapy
    private static final String METHOD_NAME_MAPFRAGMENT_PIXEL = "getMapFragmentPixel";
    private static final String SOAP_ACTION_MAPFRAGMENT_PIXEL = NAMESPACE + "getMapFragmentPixel";

    private static final String METHOD_NAME_MAPFRAGMENT_GPS = "getMapFragmentGeo";
    private static final String SOAP_ACTION_MAPFRAGMENT_GPS = NAMESPACE + "getMapFragmentGeo";

    private static final String METHOD_NAME_MAPCOORDS = "getOryginalneKoordy";
    private static final String SOAP_ACTION_MAPCOORDS = NAMESPACE + "getOryginalneKoordy";

    private static String IP = //"192.168.42.18";
            //"192.168.1.210";
            "192.168.0.12";
    private static String PORT = "8080";
    private static String PATH = "/GeoProjectWS/GeoProjectWS?WSDL";
    private static String URL = "http://" + IP + ":" + PORT + PATH;

    private Context context;
    private TextView lblResult; //wynik zapytań
    private ProgressBar progressBar;    // spinner
    private CustomDrawableView mapa;    // handler do przechowywania mapy
    private Paint paint;
    private ShapeDrawable mDrawable; //do rysowania prostokątów
    private EditText tbXfrom, tbXto, tbYfrom, tbYto;

    private float downx = 0, downy = 0, upx = 0, upy = 0; // zaznaczony obszar
    private float odownx = 0, odowny = 0, oupx = 0, oupy = 0;
            // zaznaczony obszar przeskalowany do bitmapy na serwerze

    //wielkość mapy na serwerze
    private int serverMapXsize = 1000;
    private int serverMapYsize = 637;

    private int screenHeight; // screen sizeH
    private int screenWidth;  // screen sizeW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        context = getApplicationContext();

        lblResult = (TextView) findViewById(R.id.result);
        lblResult.setText("Miejsce na rezultat zapytania z WebService");
        mapa = (CustomDrawableView) findViewById(R.id.customowamapa);
        FloatingActionButton rightButton = (FloatingActionButton) findViewById(R.id.fab);

        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.GONE);

        tbXfrom = (EditText) findViewById(R.id.tbXfrom);
        tbXto = (EditText) findViewById(R.id.tbXto);
        tbYfrom = (EditText) findViewById(R.id.tbYfrom);
        tbYto = (EditText) findViewById(R.id.tbYto);

        uzupelnijKoordynatyGlownejMapy(); // wpisuje koordynaty do EditTextow

        paint = new Paint();
        paint.setColor(Color.GREEN);

        //http://n3wt0n.com/blog/fit-width-of-background-image-but-keep-aspect-ratio-in-android/
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth =
                728; //(int) (metrics.widthPixels - this.getResources().getDimension(R.dimen
                // .fab2_margin));
        screenHeight =
                463;//(int) (metrics.heightPixels - this.getResources().getDimension(R.dimen
                // .fab2_margin)- this.getResources().getDimension(R.dimen.big_margin));
        Log.e(TAG, String.format("screen: %d == %d", screenWidth, screenHeight));   // 800 x 1024

        Bitmap bmap =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.mapka300x191i);
        try {
            zaktualizujMapkeZeStringa(mapa, bmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        pobierzMiniMape();

        mapa.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (v.getId() == R.id.customowamapa) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downx = event.getX();
                            downy = event.getY();
                            odownx = event.getX() * serverMapXsize / mapa.getLayoutParams().width;
                            odowny = event.getY() * serverMapYsize / mapa.getLayoutParams().height;
                            Log.e(TAG,
                                    String.format("Params: %d x %d", mapa.getLayoutParams().width,
                                            mapa.getLayoutParams().height));
                            break;
                        case MotionEvent.ACTION_MOVE:
                            upx = Math.min(v.getWidth(), event.getX());
                            upy = Math.min(v.getHeight(), event.getY());
                            oupx = upx * serverMapXsize / mapa.getLayoutParams().width;
                            oupy = upy * serverMapYsize / mapa.getLayoutParams().height;
                            mDrawable = new ShapeDrawable(new RectShape());
                            mDrawable.getPaint().setColor(Color.argb(100, 255, 0, 0));
                            mDrawable.setBounds(Math.round(downx), Math.round(downy),
                                    Math.round(upx), Math.round(upy));
                            mapa.pushDrawable(mDrawable);
                            mapa.invalidate();
                            lblResult.setText(
                                    String.format("FROM: [%f x %f] - TO: [%f x %f]", downx, downy,
                                            upx, upy));
                            break;
                        case MotionEvent.ACTION_UP:
                            upx = Math.min(v.getWidth(), event.getX());
                            upy = Math.min(v.getHeight(), event.getY());
                            oupx = upx * serverMapXsize / mapa.getLayoutParams().width;
                            oupy = upy * serverMapYsize / mapa.getLayoutParams().height;
                            /*tToast(String.format("FROM:\t%f : %f \nTO:\t\t %f : %f\n" +
                                            "FROM:\t%f : %f \n TO:\t\t %f : %f",
                                    downx, upx, downy, upy,
                                    odownx, oupx, odowny, oupy));*/
                            Log.e(TAG + "_size", String.format("FROM:\t%f : %f \nTO:\t\t %f : %f\n"
                                            + "FROM:\t%f : %f \n TO:\t\t %f : %f", downx, upx,
                                    downy, upy, odownx, oupx, odowny, oupy));
                            mDrawable = new ShapeDrawable(new RectShape());
                            mDrawable.getPaint().setColor(Color.argb(150, 0, 255, 0));
                            mDrawable.setBounds(Math.round(downx), Math.round(downy),
                                    Math.round(upx), Math.round(upy));
                            mapa.pushDrawable(mDrawable);
                            mapa.invalidate();

                            tbXfrom.setText("" + odownx);
                            tbYfrom.setText("" + odowny);
                            tbXto.setText("" + oupx);
                            tbYto.setText("" + oupy);

                            pobierzMapkePX();


                            break;
                        case MotionEvent.ACTION_CANCEL:
                            break;
                        default:
                            break;
                    }
                }
                return true;
            }
        });
    }

    private void pobierzMapkePX() {

        asyncGetMapFragmentPixels asyncGetMapFragmentPixels =
                new asyncGetMapFragmentPixels(new AsyncResponse() {
                    @Override
                    public void processFinish(String output) {

                        if (output != null) {
                            try {
                                zaktualizujMapkeZeStringa(mapa,
                                        decodeImagefromBase64toBitmap(output));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        try {
            asyncGetMapFragmentPixels.execute(Double.valueOf(tbXfrom.getText().toString()),
                    Double.valueOf(tbYfrom.getText().toString()),
                    Double.valueOf(tbXto.getText().toString()),
                    Double.valueOf(tbYto.getText().toString()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void pobierzMapkeGPS() {

        asyncGetMapFragmentGPS asyncGetMapFragmentGPS =
                new asyncGetMapFragmentGPS(new AsyncResponse() {
                    @Override
                    public void processFinish(String output) {

                        if (output != null) {
                            try {
                                zaktualizujMapkeZeStringa(mapa,
                                        decodeImagefromBase64toBitmap(output));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        try {
            asyncGetMapFragmentGPS.execute(Double.valueOf(tbXfrom.getText().toString()),
                    Double.valueOf(tbYfrom.getText().toString()),
                    Double.valueOf(tbXto.getText().toString()),
                    Double.valueOf(tbYto.getText().toString()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void uzupelnijKoordynatyGlownejMapy() {
        asyncGetMapParameters asyncGetMapParameter = new asyncGetMapParameters(new AsyncResponse() {
            @Override
            public void processFinish(String output) {
                try {
                    // pobrany text z formatu 123,333333;222,3333;3333,333;4444,22222 na 11.2222
                    // dla kazdego pola
                    Log.i(TAG, "Koordynaty: " + output);
                    if (output.contains(";")) {
                        tbXfrom.setText(output.split(";")[0].replace(",", "."));
                        tbYfrom.setText(output.split(";")[1].replace(",", "."));
                        tbXto.setText(output.split(";")[2].replace(",", "."));
                        tbYto.setText(output.split(";")[3].replace(",", "."));
                    } else {
                        Log.i(TAG, "Błąd wyjścia (zamiast 4 koordynatow mamy to: " + output);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        asyncGetMapParameter.execute("");
    }

    // pobiera testowo część Rect(1.00, 1.0, 1000.0, 637.0) i zapisuje do handlera mapy
    public void leftButtonClicked(View view) {
        pobierzMiniMape();
    }

    private void pobierzMiniMape() {
        asyncGetMiniMap miniMap = new asyncGetMiniMap(new AsyncResponse() {

            @Override
            public void processFinish(String output) {
                StringBuilder stringzBase64 = new StringBuilder();
                stringzBase64.append(output);
                try {
                    zaktualizujMapkeZeStringa(mapa,
                            decodeImagefromBase64toBitmap(stringzBase64.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        miniMap.execute(1.0);
    }

    public void rightButtonClicked(View view) {
        uzupelnijKoordynatyGlownejMapy();
    }

    //Toast helper
    public void tToast(Object s) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, String.valueOf(s), duration);
        toast.show();
    }

    private void zaktualizujMapkeZeStringa(CustomDrawableView mapa, Bitmap bmp) {
        //final Bitmap bmp = decodeImagefromBase64toBitmap(sb.toString());  // bitmapa <- output
        // <- String

        mapa.pushDrawable(null); // ściągnij prostokąt

        // zaktualizuj rozmiary
        float bmapWidth = bmp.getWidth();
        float bmapHeight = bmp.getHeight();

        Log.e(TAG + "zaktualizuj",
                String.format("Wielkość bitmapy: (%f x %f)", bmapWidth, bmapHeight));
        Log.e(TAG + "zaktualizuj",
                String.format("Wielkość ekranu:  (%d x %d)", screenWidth, screenHeight));

        // stwórz 2 ratio pion i poziom
        float wRatio = screenWidth / bmapWidth;
        float hRatio = screenHeight / bmapHeight;

        Log.e(TAG + "zaktualizuj",
                String.format("Ratios:\nwRatio) %f\nhRatio) %f", wRatio, hRatio));

        float ratioMultiplier = wRatio;
        // Untested conditional though I expect this might work for landscape mode
        if (hRatio < wRatio) {
            //if (bmapHeight > bmapWidth) {
            ratioMultiplier = hRatio;
        }
        Log.e(TAG + "zaktualizuj", String.format("ratioMultiplier: %f", ratioMultiplier));
        // przemnóż stare rozmiary przez ratio > nowe rozmiary
        int newBmapWidth = (int) (bmapWidth * ratioMultiplier);
        int newBmapHeight = (int) (bmapHeight * ratioMultiplier);

        Log.e(TAG + "zaktualizuj",
                String.format("Nowe parametry bitmapy: %d x %d", newBmapWidth, newBmapHeight));
        mapa.setLayoutParams(new LinearLayout.LayoutParams(newBmapWidth, newBmapHeight));
        //mapa.setLayoutParams(new LinearLayout.LayoutParams(newBmapWidth, newBmapHeight));
        Bitmap copy = Bitmap.createScaledBitmap(bmp, newBmapWidth, newBmapHeight, false);
        mapa.setBackground(new BitmapDrawable(copy));
        mapa.setMaxHeight(newBmapHeight);
        mapa.setMaxWidth(newBmapWidth);
    }

    //Bitmap decoder
    private Bitmap decodeImagefromBase64toBitmap(String base64Image) {
        Bitmap bitmap = null;
        try {
            byte[] image = Base64.decode(base64Image, Base64.DEFAULT);

            bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            Log.d(TAG,
                    "Rozmiar obrazka z decodeImagefrombase64: [b]" + String.valueOf(image.length));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /***
     * Pobiera mape na podstawie koordynatów GPS wpisanych w pola numeryczne
     *
     * @param view
     */
    public void btGPSclick(View view) {
        pobierzMapkeGPS();
        //TODO: z java EE do przerobienia na androida GPS
        /*if((toX-fromX)<=0 || (toY-fromY)>=0 || fromX < aKoordyBazowe[0] || toX>aKoordyBazowe[2]
         || fromY > aKoordyBazowe[1] || toY < aKoordyBazowe[3]){
            return getErrorImageBase64();
        }

        //skonwertuj koordy geo na piksela dla oryginalnej mapy - dopasowujac proporcje - dziala
        dla roznych rozmiarow map
        double fromXwzgledne = (fromX-aKoordyBazowe[0]) / (aKoordyBazowe[2]-aKoordyBazowe[0]);
        double fromYwzgledne = (fromY-aKoordyBazowe[1]) / (aKoordyBazowe[3]-aKoordyBazowe[1]);
        //podzielenie dwoch ujemnych - daje dodatnia
        double toXwzgledne = (toX-aKoordyBazowe[0]) / (aKoordyBazowe[2]-aKoordyBazowe[0]);
        double toYwzgledne = (toY-aKoordyBazowe[1]) / (aKoordyBazowe[3]-aKoordyBazowe[1]);
        //podzielenie dwoch ujemnych - daje dodatnia*/

    }

    /***
     * Pobiera mape na podstawie numerów pixeli wpisanych w pola numeryczne
     *
     * @param view
     */
    public void btPXclick(View view) {
        pobierzMapkePX();
    }

    enum CoordsType {
        PIXELS_COORDS,
        GPS_COORDS
    }

    // http://stackoverflow.com/questions/12575068/how-to-get-the-result-of-onpostexecute-to-main
    // -activity-because-asynctask-is-a
    public interface AsyncResponse {
        void processFinish(String output);
    }

    // funkcja pobiera mapkę w wersji pomniejszonej
    public class asyncGetMiniMap extends AsyncTask<Double, String, String> {
        public AsyncResponse asyncResponseDelegate = null;

        public asyncGetMiniMap(AsyncResponse asyncResponseDelegate) {
            this.asyncResponseDelegate =
                    asyncResponseDelegate;//Assigning call back interface through constructor
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            lblResult.setText("Przygotowywanie zapytania...");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            lblResult.setText("Gotowe");
            asyncResponseDelegate.processFinish(result);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            lblResult.setText(values[0]);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Double... params) {
            WSDL WSDL = new WSDL(METHOD_NAME_MINIMAP, SOAP_ACTION_MINIMAP, params).invoke();

            publishProgress("Oczekiwanie na odbiór zapytania przez serwer...");
            WSDL.call();
            publishProgress("Trwa odbiór odpowiedzi z serwera...");
            String response = null;
            try {
                response = WSDL.getResponse();
            } catch (IOException e) {
                e.printStackTrace();
                return "Błąd połączenia z serwerem. Sprawdź URL: " + URL;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    public class asyncGetMapParameters extends AsyncTask<String, String, String> {

        public AsyncResponse asyncResponseDelegate = null;

        public asyncGetMapParameters(AsyncResponse asyncResponseDelegate) {
            this.asyncResponseDelegate =
                    asyncResponseDelegate;//Assigning call back interface through constructor
        }

        @Override
        protected void onPostExecute(String result) {
            lblResult.setText("Gotowe");
            asyncResponseDelegate.processFinish(result);
        }

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected String doInBackground(String... params) {
            WSDL WSDL = new WSDL(METHOD_NAME_MAPCOORDS, SOAP_ACTION_MAPCOORDS).invoke();
            publishProgress("Oczekiwanie na odbiór zapytania przez serwer...");
            WSDL.call();
            publishProgress("Trwa odbiór odpowiedzi z serwera...");
            String response = null;
            try {
                response = WSDL.getResponse();
            } catch (IOException e) {
                e.printStackTrace();
                return "Błąd połączenia z serwerem. Sprawdź URL: " + URL;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    // pobiera fragment mapy RectStart[x,y],RectEnd[x,y]
    public class asyncGetMapFragmentPixels extends AsyncTask<Double, String, String> {
        public AsyncResponse asyncResponseDelegate = null;

        public asyncGetMapFragmentPixels(AsyncResponse asyncResponseDelegate) {
            this.asyncResponseDelegate =
                    asyncResponseDelegate;//Assigning call back interface through constructor
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            lblResult.setText("Przygotowywanie zapytania...");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            lblResult.setText("Gotowe");
            asyncResponseDelegate.processFinish(result);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            lblResult.setText(values[0]);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Double... params) {
            WSDL WSDL =
                    new WSDL(METHOD_NAME_MAPFRAGMENT_PIXEL, SOAP_ACTION_MAPFRAGMENT_PIXEL, params)
                            .invoke();

            publishProgress("Oczekiwanie na odbiór zapytania przez serwer...");
            WSDL.call();
            publishProgress("Trwa odbiór odpowiedzi z serwera...");
            String response = "";
            try {
                response = WSDL.getResponse();
            } catch (IOException e) {
                e.printStackTrace();
                return "Błąd połączenia z serwerem. Sprawdź URL: " + URL;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    // pobiera fragment mapy po GPS RectStart[x,y],RectEnd[x,y]
    public class asyncGetMapFragmentGPS extends AsyncTask<Double, String, String> {
        public AsyncResponse asyncResponseDelegate = null;

        public asyncGetMapFragmentGPS(AsyncResponse asyncResponseDelegate) {
            this.asyncResponseDelegate =
                    asyncResponseDelegate;//Assigning call back interface through constructor
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            lblResult.setText("Przygotowywanie zapytania...");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            lblResult.setText("Gotowe");
            asyncResponseDelegate.processFinish(result);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            lblResult.setText(values[0]);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Double... params) {
            WSDL WSDL = new WSDL(METHOD_NAME_MAPFRAGMENT_GPS, SOAP_ACTION_MAPFRAGMENT_GPS, params)
                    .invoke();

            publishProgress("Oczekiwanie na odbiór zapytania przez serwer...");
            WSDL.call();
            publishProgress("Trwa odbiór odpowiedzi z serwera...");
            String response = "";
            try {
                response = WSDL.getResponse();
            } catch (IOException e) {
                e.printStackTrace();
                return "Błąd połączenia z serwerem. Sprawdź URL: " + URL;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    ///////////////////////////////////////////  HELPERS
    // ////////////////////////////////////////////////////
    protected class WSDL {
        private String methodName;
        private Double[] params;
        private SoapSerializationEnvelope envelope;
        private HttpTransportSE androidHttpTransport;
        private String soapAction;

        public WSDL(String methodName, String soapAction, Double... params) {
            this.methodName = methodName;
            this.params = params;
            this.soapAction = soapAction;
        }

        public SoapSerializationEnvelope getEnvelope() {
            return envelope;
        }

        public HttpTransportSE getAndroidHttpTransport() {
            return androidHttpTransport;
        }

        public WSDL invoke() {
            Pos boxS = new Pos();
            Pos boxE = new Pos();
            SoapObject request = new SoapObject(NAMESPACE, methodName);
            Log.e(TAG, String.format("Ilość doubli: %d", params.length));
            if (params.length == 4) {
                boxS.x = params[0];
                boxS.y = params[1];
                boxE.x = params[2];
                boxE.y = params[3];

                PropertyInfo propInfoa = new PropertyInfo();
                PropertyInfo propInfob = new PropertyInfo();
                PropertyInfo propInfoc = new PropertyInfo();
                PropertyInfo propInfod = new PropertyInfo();
                propInfoa.name = ("fromPosX");
                propInfoa.setValue(boxS.x);
                propInfoa.setType(Double.class);
                request.addProperty(propInfoa);

                propInfob.name = ("fromPosY");
                propInfob.setValue(boxS.y);
                propInfob.setType(Double.class);
                request.addProperty(propInfob);

                propInfoc.name = ("toPosX");
                propInfoc.setValue(boxE.x);
                propInfoc.setType(Double.class);
                request.addProperty(propInfoc);

                propInfod.name = ("toPosY");
                propInfod.setValue(boxE.y);
                propInfod.setType(Double.class);
                request.addProperty(propInfod);
            }
            envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.implicitTypes = true;
            envelope.encodingStyle = SoapSerializationEnvelope.XSD;
            envelope.setOutputSoapObject(request);
            androidHttpTransport = new HttpTransportSE(URL);
            androidHttpTransport.debug = true;
            MarshalDouble md = new MarshalDouble();

            new MarshalBase64().register(envelope);
            md.register(envelope);
            return this;
        }

        public String getResponse() throws SoapFault {
            SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
            Log.i("GeoProject", "androidHttpTransport \t--->\t requested: \t"
                    + androidHttpTransport.requestDump);
            Log.i("GeoProject", "androidHttpTransport \t<---\t returned: \t"
                    + androidHttpTransport.responseDump);
            return (resultsRequestSOAP.toString());
        }

        public String call() {
            Log.i("GeoProject", "androidHttpTransport 	--->	 Set request.");
            try {
                Log.i("GeoProject", "androidHttpTransport 	--->	 Set request.");
                androidHttpTransport.call(soapAction, envelope);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                return "Błąd odpowiedzi z serwera. Czy uruchomiono Deploy?";
            } catch (IOException e) {
                e.printStackTrace();
                return "Błąd połączenia z serwerem. Sprawdź URL: " + URL;
            }
            return "Nawiązano połączenie z serwerem";
        }
    }
}



