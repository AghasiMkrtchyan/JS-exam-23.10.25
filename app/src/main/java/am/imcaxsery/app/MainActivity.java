package am.imcaxsery.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 321;
    private static final int IMPORT_REQUEST = 322;
    private WebView web;
    private byte[] exportBytes;
    private String exportMime = "application/json";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11, 20, 32));
        getWindow().setNavigationBarColor(Color.rgb(11, 20, 32));
        getWindow().getDecorView().setSystemUiVisibility(0);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(11, 20, 32));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        web.addJavascriptInterface(new AppBridge(), "AndroidApp");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !"file".equals(request.getUrl().getScheme());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setMessage(message)
                    .setPositiveButton("Այո", (dialog, which) -> result.confirm())
                    .setNegativeButton("Ոչ", (dialog, which) -> result.cancel())
                    .setOnCancelListener(dialog -> result.cancel()).show();
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    public class AppBridge {
        @JavascriptInterface public void save(String filename, String mime, String content) {
            runOnUiThread(() -> {
                exportBytes = content.getBytes(StandardCharsets.UTF_8);
                exportMime = mime.equals("text/csv") ? "text/csv" : "application/json";
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(exportMime);
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                startActivityForResult(intent, EXPORT_REQUEST);
            });
        }
        @JavascriptInterface public void openBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, IMPORT_REQUEST);
            });
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (request == EXPORT_REQUEST && exportBytes != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                    if (out == null) throw new IllegalStateException("Unable to open document");
                    out.write(exportBytes);
                }
                Toast.makeText(this, "Ֆայլը պահպանվեց", Toast.LENGTH_SHORT).show();
            } else if (request == IMPORT_REQUEST) {
                byte[] content;
                try (InputStream in = getContentResolver().openInputStream(uri);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    if (in == null) throw new IllegalStateException("Unable to read document");
                    byte[] block = new byte[4096];
                    int length;
                    while ((length = in.read(block)) != -1) {
                        if (buffer.size() + length > 4_000_000) throw new IllegalStateException("Backup is too large");
                        buffer.write(block, 0, length);
                    }
                    content = buffer.toByteArray();
                }
                String json = new String(content, StandardCharsets.UTF_8);
                web.evaluateJavascript("window.importBackupText(" + JSONObject.quote(json) + ")", null);
            }
        } catch (Exception error) {
            Toast.makeText(this, "Ֆայլի գործողությունը չհաջողվեց", Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("window.goBackInApp ? window.goBackInApp() : false", result -> {
            if (!"true".equals(result)) super.onBackPressed();
        });
    }
    @Override protected void onDestroy() {
        if (web != null) { web.removeJavascriptInterface("AndroidApp"); web.destroy(); }
        super.onDestroy();
    }
}
