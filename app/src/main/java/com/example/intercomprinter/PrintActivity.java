package com.example.intercomprinter;

import com.honeywell.mobility.print.LinePrinter;
import com.honeywell.mobility.print.LinePrinterException;
import com.honeywell.mobility.print.PrintProgressEvent;
import com.honeywell.mobility.print.PrintProgressListener;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.*;


/**
 * This sample demonstrates printing on Android using the Intermec LinePrinter
 * class. You may enter or scan an Intermec mobile printer's MAC address and
 * click the Print button to print. The MAC Address text should have the format
 * of "nn:nn:nn:nn:nn:nn" or "nnnnnnnnnnnn" where each n is a hex digit.
 * <p>
 * You may also capture a signature to print by clicking the Sign button. It
 * will display another screen for you to sign and save the signature. After
 * you save the signature, you will see a preview of the signature graphic
 * next to the Sign button.
 * <p>
 * The printing progress will be displayed in the Progress and Status text box.
 */
public class PrintActivity extends Activity {
    private Button buttonPrint;
    private EditText editPrinterID;
    private EditText editMacAddr;
    private EditText editUserText;
    private String base64SignaturePng = null;

    public static final int CAPTURE_SIGNATURE_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_print);
        editPrinterID = (EditText) findViewById(R.id.editPrinterID);
        editPrinterID.setText("PR2");
        editMacAddr = (EditText) findViewById(R.id.editMacAddr);
        editMacAddr.setText("00:06:66:08:A7:5D");
        editUserText = (EditText) findViewById(R.id.editUserText);
        buttonPrint = (Button) findViewById(R.id.buttonPrint);
        buttonPrint.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {


                new AsyncTaskRunner().execute();
            }
        });
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {


        @Override
        protected String doInBackground(String... params) {
            LinePrinter lp = null;
            try {

                String sResult = null;
                String sDocNumber = "1234567890";


                String sPrinterURI = "bt://" + "00:06:66:08:A7:5D";
                String sUserText = "PB51";

                LinePrinter.ExtraSettings exSettings = new LinePrinter.ExtraSettings();

                exSettings.setContext(PrintActivity.this);


                InputStream stream = getAssets().open("printer_profiles.JSON");
                File profiles = new File(getExternalFilesDir(null), "printer_profiles.JSON");

                lp = new LinePrinter(
                        "file:///android_asset/printer_profiles.JSON",
                        sUserText,
                        sPrinterURI,
                        exSettings);
                Log.d("MHMT", "Hi");

                // Registers to listen for the print progress events.
                lp.addPrintProgressListener(new PrintProgressListener() {
                    public void receivedStatus(PrintProgressEvent aEvent) {
                        // Publishes updates on the UI thread.

                    }
                });

                //A retry sequence in case the bluetooth socket is temporarily not ready
                int numtries = 0;
                int maxretry = 2;
                while (numtries < maxretry) {
                    try {
                        lp.connect();  // Connects to the printer
                        break;
                    } catch (LinePrinterException ex) {
                        numtries++;
                        Thread.sleep(1000);
                    }
                }
                if (numtries == maxretry) lp.connect();//Final retry

                // Prints the Honeywell logo graphic on the receipt if the graphic
                // file exists.
                File graphicFile = new File(getExternalFilesDir(null), "honeywell_logo.bmp");
                if (graphicFile.exists()) {
                    lp.writeGraphic(graphicFile.getAbsolutePath(),
                            LinePrinter.GraphicRotationDegrees.DEGREE_0,
                            72,  // Offset in printhead dots from the left of the page
                            200, // Desired graphic width on paper in printhead dots
                            40); // Desired graphic height on paper in printhead dots
                }
                lp.newLine(1);

                // Set font style to Bold + Double Wide + Double High.
                lp.setBold(true);
                lp.setDoubleWide(true);
                lp.setDoubleHigh(true);
                lp.write("SALES ORDER");
                lp.setDoubleWide(false);
                lp.setDoubleHigh(false);
                lp.newLine(2);

                // The following text shall be printed in Bold font style.
                lp.write("CUSTOMER: Casual Step");
                lp.setBold(false);  // Returns to normal font.
                lp.newLine(2);

                // Set font style to Compressed + Double High.
                lp.setDoubleHigh(true);
                lp.setCompress(true);
                lp.write("DOCUMENT#: " + sDocNumber);
                lp.setCompress(false);
                lp.setDoubleHigh(false);
                lp.newLine(2);

                // The following text shall be printed in Normal font style.
                lp.write(" PRD. DESCRIPT.   PRC.  QTY.    NET.");
                lp.newLine(2);

                lp.write(" 1501 Timer-Md1  13.15     1   13.15");
                lp.newLine(1);
                lp.write(" 1502 Timer-Md2  13.15     3   39.45");
                lp.newLine(1);
                lp.write(" 1503 Timer-Md3  13.15     2   26.30");
                lp.newLine(1);
                lp.write(" 1504 Timer-Md4  13.15     4   52.60");
                lp.newLine(1);
                lp.write(" 1505 Timer-Md5  13.15     5   65.75");
                lp.newLine(1);
                lp.write("                        ----  ------");
                lp.newLine(1);
                lp.write("              SUBTOTAL    15  197.25");
                lp.newLine(2);

                lp.write("          5% State Tax          9.86");
                lp.newLine(2);

                lp.write("                              ------");
                lp.newLine(1);
                lp.write("           BALANCE DUE        207.11");
                lp.newLine(1);
                lp.newLine(1);

                lp.write(" PAYMENT TYPE: CASH");
                lp.newLine(2);

                lp.setDoubleHigh(true);
                lp.write("       SIGNATURE / STORE STAMP");
                lp.setDoubleHigh(false);
                lp.newLine(2);

                // Prints the captured signature if it exists.
                if (base64SignaturePng != null) {
                    android.util.Log.d("LinePrinterSample", "Base64 graphic:" + base64SignaturePng);
                    lp.writeGraphicBase64(base64SignaturePng,
                            LinePrinter.GraphicRotationDegrees.DEGREE_0,
                            72,   // Offset in printhead dots from the left of the page
                            220,  // Desired graphic width on paper in printhead dots
                            100); // Desired graphic height on paper in printhead dots
                }
                lp.newLine(1);

                lp.setBold(true);
                if (sUserText.length() > 0) {
                    // Print the text entered by user in the Optional Text field.
                    lp.write(sUserText);
                    lp.newLine(2);
                }


                lp.write("          ORIGINAL");
                lp.setBold(false);
                lp.newLine(2);

                // Print a Code 39 barcode containing the document number.
                lp.writeBarcode(LinePrinter.BarcodeSymbologies.SYMBOLOGY_CODE39,
                        sDocNumber,   // Document# to encode in barcode
                        90,           // Desired height of the barcode in printhead dots
                        40);          // Offset in printhead dots from the left of the page

                lp.newLine(4);

                sResult = "Number of bytes sent to printer: " + lp.getBytesWritten();

                return sResult;
            } catch (Exception ex) {
                Log.e("Mhmt", ex.getMessage());
            } finally {
                if (lp != null) {
                    try {
                        lp.disconnect();
                        lp.close();
                    } catch (Exception ex) {
                        Log.e("Mhmt", ex.getMessage());
                    }
                }
            }
            return "";
        }


        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation


        }


        @Override
        protected void onPreExecute() {

        }


        @Override
        protected void onProgressUpdate(String... text) {


        }
    }
}
