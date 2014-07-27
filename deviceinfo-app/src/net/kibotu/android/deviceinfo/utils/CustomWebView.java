package net.kibotu.android.deviceinfo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.http.SslError;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import net.kibotu.android.deviceinfo.Device;
import net.kibotu.android.deviceinfo.DisplayHelper;
import net.kibotu.android.deviceinfo.R;
import net.kibotu.android.error.tracking.Logger;

/**
 * Custom WebView
 * <pre class="prettyprint">
 * <h3>How to:</h3>
 * 1. <b>new CustomWebView(activity)</b>
 * 2. <b>showWebView("http://www.wooga.com/", x, y, width, height)</b>
 * or <b>showWebViewFullScreen(url)</b>
 * or as standalone Dialog <b>CustomWebView.showWebViewInDialog(activity, url, x, y, width, height)</b>
 * <i>optional</i> 3. update url if required <b>updateUrl(url)</b>
 * 4 and dispose <b>removeWebView()</b>
 * </pre>
 *
 * @credits <a href="https://github.com/go3k/CCXWebview/blob/master/CCXWebview/CCXWebview/proj.android/src/org/go3k/ccxwebview/CCXWebview.java">CCXWebview</a>
 * @user <i>customized by</i> <a href="mailto:jan.rabe@wooga.net">Jan Rabe</a></p>
 */
@SuppressLint("NewApi")
public class CustomWebView {

    private static final String LOGGING_TAG = CustomWebView.class.getSimpleName();
    public volatile static AlertDialog dialog;

    /**
     * Context.
     */
    private final Activity context;

    /**
     * Holds the ViewGroup.
     */
    private RelativeLayout customWebViewContainer;

    /**
     * Actually holds the webview.
     */
    private RelativeLayout webViewContainer;

    /**
     * Current Webview.
     */
    public static volatile WebView webView;

    /**
     * Top shadow.
     */
    private ImageView topShadow;

    /**
     * Bottom Shadow.
     */
    private ImageView bottomShadow;

    /**
     * Progress dialog waiting
     */
    private static ProgressDialog prDialog = null;

    /**
     * Domain which the webview may loadOnCreate only from.
     */
    private static String restrictedUrl;

    /**
     * Constructs new CustomWebView.
     *
     * @param activity - Context. Must not be null.
     */
    public CustomWebView(final Activity activity) {
        if (activity == null)
            throw new IllegalArgumentException("'activity' must not be null");
        this.context = activity;
        init();
    }

    /**
     * Adds the webViewContainer to the current view.
     */
    private void init() {
        context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        customWebViewContainer = new RelativeLayout(context);
        context.addContentView(customWebViewContainer, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public String getPleaseWaitLoadingText() {
        final String ko = "잠시만 기다려주세요. 로딩중입니다...";
        final String en = "Please Wait, Loading...";
        return java.util.Locale.getDefault().getLanguage().equalsIgnoreCase("ko") ? ko : en;
    }

    /**
     * Shows a webview at position and with qualified dimension and loads the url.
     *
     * @param url    - Url to be loaded.
     * @param x      - X-coordinate.
     * @param y      - Y-coordinate.
     * @param width  - Width of the webview.
     * @param height - Height of the webview.
     */
    public void showWebView(final String url, final int x, final int y, final int width, final int height) {

        Logger.d("[" + url + "|x=" + x + "|y=" + y + "|w=" + width + "|h=" + height + "]");

        context.runOnUiThread(new Runnable() {

            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                prDialog = ProgressDialog.show(context, null, getPleaseWaitLoadingText());
                webViewContainer = new RelativeLayout(context);
                RelativeLayout.LayoutParams viewGroupParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                viewGroupParams.leftMargin = x;
                viewGroupParams.topMargin = y;
                viewGroupParams.width = width;
                viewGroupParams.height = height;
                webViewContainer.setLayoutParams(viewGroupParams);

                topShadow = new ImageView(context);
                Bitmap topShadowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.topcut);
                topShadow.setImageBitmap(topShadowBitmap);
                topShadow.setAdjustViewBounds(false);
                topShadow.setScaleType(ImageView.ScaleType.CENTER_CROP);
                RelativeLayout.LayoutParams topShadowParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                topShadowParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                topShadow.setLayoutParams(topShadowParams);

                bottomShadow = new ImageView(context);
                Bitmap bottomShadowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bottomcut);
                bottomShadow.setImageBitmap(bottomShadowBitmap);
                bottomShadow.setAdjustViewBounds(false);
                bottomShadow.setScaleType(ImageView.ScaleType.CENTER_CROP);
                RelativeLayout.LayoutParams bottomShadowParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                bottomShadowParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                bottomShadow.setLayoutParams(bottomShadowParams);

                webView = new WebView(context);
                RelativeLayout.LayoutParams webViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                webView.setLayoutParams(webViewParams);
                webView.loadUrl(url);
                setWebViewSettings(context, webView);

                if (android.os.Build.VERSION.SDK_INT >= 16) ;
//		        	customWebViewContainer.setBackground(new ColorDrawable(Color.TRANSPARENT));
                else
                    customWebViewContainer.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                        return restrictedUrl != null && url.contains(restrictedUrl);
                    }

                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Logger.e(LOGGING_TAG, description + " " + failingUrl + " " + errorCode);
                        if (prDialog != null) {
                            prDialog.dismiss();
//	            			prDialog = null;
                        }
//		            	Logger.toast(context,"[description= " + description + "|failingUrl=" + failingUrl + "|errorCode=" + errorCode + "]");
                        super.onReceivedError(view, errorCode, description, failingUrl);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        try {
                            customWebViewContainer.addView(webViewContainer);
                            webViewContainer.addView(webView);
                            webViewContainer.addView(topShadow);
                            topShadow.bringToFront();
                            webViewContainer.addView(bottomShadow);
                            bottomShadow.bringToFront();
                            customWebViewContainer.invalidate();
                        } catch (final Exception e) {
                            Logger.e(LOGGING_TAG, "" + e.getMessage());
                            removeWebView();
                        } finally {
                            if (prDialog != null) {
                                prDialog.dismiss();
//		            			prDialog = null;
                            }
                        }
                        super.onPageFinished(view, url);
                    }
                });
            }
        });
    }

    public static WebView createWebView(Context context) {
        final WebView view = new WebView(context) {

            @Override
            public void loadUrl(final String url) {
                Logger.v("loading url " + url);
                super.loadUrl(url);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(DisplayHelper.absScreenHeight / 2, View.MeasureSpec.AT_MOST);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        RelativeLayout.LayoutParams webViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        view.setLayoutParams(webViewParams);
        setWebViewSettings((Activity) context, view);
        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                if (restrictedUrl != null && url.contains(restrictedUrl))
                    return true;
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Logger.e(LOGGING_TAG, description + " " + failingUrl + " " + errorCode);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
//                view.setMinimumHeight(150);
                super.onPageFinished(view, url);
            }
        });
        return view;
    }

    /**
     * Shows a terms with webview at position and with qualified dimension and loads a file.
     *
     * @param url    - Url to be loaded.
     * @param x      - X-coordinate.
     * @param y      - Y-coordinate.
     * @param width  - Width of the webview.
     * @param height - Height of the webview.
     */
    public void showTermsWebView(final String url, final int x, final int y, final int width, final int height) {

        Logger.d(LOGGING_TAG, "[" + url + "|x=" + x + "|y=" + y + "|w=" + width + "|h=" + height + "]");

        context.runOnUiThread(new Runnable() {

            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                prDialog = ProgressDialog.show(context, null, getPleaseWaitLoadingText());
                webViewContainer = new RelativeLayout(context);
                RelativeLayout.LayoutParams viewGroupParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                viewGroupParams.leftMargin = x;
                viewGroupParams.topMargin = y;
                viewGroupParams.width = width;
                viewGroupParams.height = height;
                webViewContainer.setLayoutParams(viewGroupParams);

                topShadow = new ImageView(context);
                Bitmap topShadowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.topcut);
                topShadow.setImageBitmap(topShadowBitmap);
                topShadow.setAdjustViewBounds(false);
                topShadow.setScaleType(ImageView.ScaleType.CENTER_CROP);
                RelativeLayout.LayoutParams topShadowParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                topShadowParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                topShadow.setLayoutParams(topShadowParams);
                topShadow.setVisibility(View.INVISIBLE);

                bottomShadow = new ImageView(context);
                Bitmap bottomShadowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bottomcut);
                bottomShadow.setImageBitmap(bottomShadowBitmap);
                bottomShadow.setAdjustViewBounds(false);
                bottomShadow.setScaleType(ImageView.ScaleType.CENTER_CROP);
                RelativeLayout.LayoutParams bottomShadowParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                bottomShadowParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                bottomShadow.setLayoutParams(bottomShadowParams);
                bottomShadow.setVisibility(View.GONE);

                webView = new WebView(context);
                RelativeLayout.LayoutParams webViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                webView.setLayoutParams(webViewParams);
                webView.loadUrl(url);
                webView.getSettings().setSupportZoom(false);
                webView.setBackgroundColor(0x00000000);
                setWebViewSettings(context, webView);

                if (android.os.Build.VERSION.SDK_INT >= 16) ;
//		        	customWebViewContainer.setBackground(new ColorDrawable(Color.TRANSPARENT));
                else
                    customWebViewContainer.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                        return restrictedUrl != null && url.contains(restrictedUrl);
                    }

                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Logger.e(LOGGING_TAG, description + " " + failingUrl + " " + errorCode);
                        if (prDialog != null) {
                            prDialog.dismiss();
                            prDialog = null;
                        }
//		            	Logger.toast(context,"[description= " + description + "|failingUrl=" + failingUrl + "|errorCode=" + errorCode + "]");
                        super.onReceivedError(view, errorCode, description, failingUrl);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        try {
                            customWebViewContainer.addView(webViewContainer);
                            webViewContainer.addView(webView);
                            webViewContainer.addView(topShadow);
                            topShadow.bringToFront();
                            webViewContainer.addView(bottomShadow);
                            bottomShadow.bringToFront();
                            customWebViewContainer.invalidate();
                        } catch (final Exception e) {
                            Logger.e(LOGGING_TAG, "" + e.getMessage());
                            removeWebView();
                        } finally {
                            if (prDialog != null) {
                                prDialog.dismiss();
                                prDialog = null;
                            }
                        }
                        super.onPageFinished(view, url);
                    }
                });
            }
        });
    }

    /**
     * Static method to create and display an url in a webview at a position with a certain dimension.
     *
     * @param activity - Context.
     * @param url      - Url.
     * @param x        - X-coordinate.
     * @param y        - Y-coordinate.
     * @param width    - Width.
     * @param height   - Height.
     */
    public static void showWebViewInDialog(final Activity activity, final String url, final int x, final int y, final int width, final int height) {
        showWebViewInDialog(activity, url, x, y, width, height, null);
    }

    public static void destroy() {
        Device.context().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(webView != null) CustomWebView.webView.destroy();
                if(dialog != null) CustomWebView.dialog.cancel();
            }
        });
    }

    public static interface UrlHandler {

        public String getUrlQualifier();

        public boolean dealWithUrl(final String url);
    }

    public static void showWebViewInDialog(final Activity activity, final String url, final int x, final int y, final int width, final int height, final UrlHandler dealer) {
        Logger.v(LOGGING_TAG, "[" + url + "|x=" + x + "|y=" + y + "|w=" + width + "|h=" + height + "]");

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                RelativeLayout viewGroup = new RelativeLayout(activity);
                RelativeLayout.LayoutParams viewGroupParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                viewGroup.setLayoutParams(viewGroupParams);

                final ImageView topShadow = new ImageView(activity);
                Bitmap topShadowBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.topcut);
                topShadow.setImageBitmap(topShadowBitmap);
                topShadow.setAdjustViewBounds(false);
                topShadow.setScaleType(ImageView.ScaleType.CENTER_CROP);
                topShadow.bringToFront();
                RelativeLayout.LayoutParams topShadowParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                topShadowParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                topShadow.setLayoutParams(topShadowParams);

                final ImageView bottomShadow = new ImageView(activity);
                Bitmap bottomShadowBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.bottomcut);
                bottomShadow.setImageBitmap(bottomShadowBitmap);
                bottomShadow.setAdjustViewBounds(false);
                bottomShadow.bringToFront();
                bottomShadow.setScaleType(ImageView.ScaleType.CENTER_CROP);
                RelativeLayout.LayoutParams bottomShadowParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                bottomShadowParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                bottomShadow.setLayoutParams(bottomShadowParams);

                webView = new LiveWebView(activity);
                RelativeLayout.LayoutParams webViewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                webView.setLayoutParams(webViewParams);
                webView.loadUrl(url);
                setWebViewSettings(activity, webView);

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {

                        if (dealer != null && url.startsWith(dealer.getUrlQualifier())) {
                            return dealer.dealWithUrl(url);
                        }

                        if (restrictedUrl != null && url.contains(restrictedUrl))
                            return true;
                        return false;
                    }

                    @Override
                    public void onPageFinished(final WebView view, final String url) {
                        view.setMinimumHeight(height);
                        if (dealer != null && url.startsWith(dealer.getUrlQualifier())) {
                            view.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onReceivedError(final WebView view, final int errorCode, final String description, final String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        Logger.v("onReceivedError " + errorCode + " " + description);
                    }

                    @Override
                    public void onReceivedSslError(final WebView view, final SslErrorHandler handler, final SslError error) {
                        super.onReceivedSslError(view, handler, error);
                        Logger.v("onReceivedSslError " + error);
                    }
                });

                //AlertDialog dialog = new AlertDialog.Builder(mActivity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).create();
                //AlertDialog dialog = AlertDialog.Builder(new ContextThemeWrapper(mActivity,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)).create();

                Builder builder = new Builder(new ContextThemeWrapper(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen));
                dialog = builder.create();

                WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setCanceledOnTouchOutside(true);
                //params.gravity = Gravity.CENTER;
                //params.x = x;
                //params.y = y;
                dialog.getWindow().setAttributes(params);

                viewGroup.addView(webView);
                viewGroup.addView(topShadow);
                viewGroup.addView(bottomShadow);

                dialog.setView(viewGroup, 0, 0, 0, 0);
                dialog.show();

                // important! call after dialog.show()
                dialog.getWindow().setLayout(width, height);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                webView.requestFocus(View.FOCUS_DOWN);
                webView.requestFocusFromTouch();
                webView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP:
                                if (!v.hasFocus()) {
                                    v.requestFocus();
                                }
                                break;
                        }
                        return false;
                    }
                });
            }
        });
    }

    public static class LiveWebView extends WebView {

        public LiveWebView(Context context) {
            super(context);
        }

        public LiveWebView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public LiveWebView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return true;
        }
    }

    /**
     * Sets default settings for a WebView.
     *
     * @param activity - Current context.
     * @param webView  - Webview to set up.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void setWebViewSettings(final Activity activity, final WebView webView) {
        if (activity == null) return;
        if (webView == null) return;
        final String internalFilePath = activity.getFilesDir().getPath();
        // http://stackoverflow.com/questions/10097233/optimal-webview-settings-for-html5-support
        final WebSettings s = webView.getSettings();
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        s.setJavaScriptEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setPluginState(PluginState.ON_DEMAND);
        s.setRenderPriority(RenderPriority.HIGH);
        s.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // WebSettings.LOAD_DEFAULT
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setDatabasePath(internalFilePath + "databases/");
        s.setAppCacheEnabled(true);
        s.setAppCachePath(internalFilePath + "cache/");
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        // Configure the webview https://code.google.com/p/html5webview/source/browse/trunk/HTML5WebView/src/org/itri/html5webview/HTML5WebView.java
        s.setBuiltInZoomControls(true);
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSavePassword(false);
        s.setSaveFormData(true);
//        String userAgent = "Mozilla/5.0 (Linux; U; Android 4.2.2; en-gb; GT-I9100 Build/JDQ39E; CyanogenMod-10.1) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30";
//        s.setUserAgentString(userAgent); // custom android user agent
//        webView.requestFocus(View.FOCUS_DOWN);
    }

    /**
     * Shows an url in a webview in fullscreen.
     *
     * @param url - Url to be shown.
     */
    public void showWebViewFullScreen(final String url) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        showWebView(url, 0, 0, width, height);
    }

    /**
     * Updates the webview with a new url.
     *
     * @param url - New url to be shown.
     */
    public void updateUrl(final String url) {

        if (webView == null)
            throw new IllegalArgumentException("'webView' must not be null");

        Logger.v(LOGGING_TAG, "updateURL" + url);

        context.runOnUiThread(new Runnable() {
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    /**
     * Disposes the webview.
     */
    public void removeWebView() {

        if (webView == null || customWebViewContainer == null)
            return;

        Logger.v(LOGGING_TAG, "removeWebView");

        context.runOnUiThread(new Runnable() {
            public void run() {
                if (prDialog != null) {
                    prDialog.dismiss();
                    prDialog = null;
                }
                if (webViewContainer != null) {
                    webViewContainer.removeView(webView);
                    webViewContainer.removeView(topShadow);
                    webViewContainer.removeView(bottomShadow);
                    customWebViewContainer.removeView(webViewContainer);
                    webView.destroy();
                    webView = null;
                    webViewContainer = null;
                }
            }
        });
    }

    public void setRestrictedUrl(final String url) {
        restrictedUrl = url;
    }
}
