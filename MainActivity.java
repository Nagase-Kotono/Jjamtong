package com.example.checkappversion;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private WebView mWebView;
    private final String updateUrl = "https://huggingface.co/Nagase-Kotono/Nagase_Mana/tree/main";

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 26) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                // 알 수 없는 앱 설치를 허용하도록 사용자에게 요청합니다.
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                startActivity(settingsIntent);
            }
        }

        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new MyWebViewClient());
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); // 자바스크립트가 자동으로 창을 열 수 있도록 설정
        webSettings.setDomStorageEnabled(true); // DOM 스토리지 활성화

        new VersionCheck(this, mWebView, updateUrl, BuildConfig.VERSION_NAME).execute();

        mWebView.setDownloadListener(new DownloadListener() { // 다운로드 리스너 설정
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url)); // 다운로드 요청 생성
                    request.setMimeType(mimeType); // MIME 타입 설정
                    request.addRequestHeader("User-Agent", userAgent); // User-Agent 헤더 추가
                    request.setDescription("Downloading file"); // 다운로드 설명 설정

                    String fileName = ""; // 파일 이름 초기화
                    String regex = "filename=\"(.+?)\""; // 파일 이름을 추출하기 위한 정규 표현식
                    Pattern pattern = Pattern.compile(regex); // 패턴 컴파일
                    Matcher matcher = pattern.matcher(contentDisposition); // 매처 생성
                    if (matcher.find()) { // 매칭된 경우
                        fileName = matcher.group(1); // 파일 이름 추출
                        fileName = URLDecoder.decode(fileName, "UTF-8"); // URL 디코딩

                        // 중복 파일 확장자 제거
                        int lastIndex = fileName.lastIndexOf(".");
                        if (lastIndex != -1 && fileName.substring(lastIndex).equals(".apk.apk")) {
                            fileName = fileName.substring(0, lastIndex) + ".apk";
                        }
                    }

                    Log.d("DownloadManager", "Original file name: " + contentDisposition); // 원본 파일 이름 로깅
                    Log.d("DownloadManager", "Processed file name: " + fileName); // 처리된 파일 이름 로깅

                    request.setTitle(fileName); // 다운로드 제목 설정
                    request.allowScanningByMediaScanner(); // 미디어 스캐너에 의한 스캔 허용
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // 다운로드 완료 시 알림 표시
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName); // 다운로드 위치 설정

                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE); //다운로드 매니저 가져오기
                    dm.enqueue(request); // 다운로드 요청 추가
                    Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show(); // 다운로드 시작 토스트 메시지 표시
                } catch (Exception e) { // 예외 처리
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) { // 외부 저장소 쓰기 권한이 없는 경우
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) { // 권한 요청 설명이 필요한 경우
                            Toast.makeText(getBaseContext(), "첨부파일 다운로드를 위해\n동의가 필요합니다.", Toast.LENGTH_LONG).show(); // 권한 요청 토스트 메시지 표시
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    110); // 권한 요청
                        } else { // 권한 요청 설명이 필요하지 않은 경우
                            Toast.makeText(getBaseContext(), "첨부파일 다운로드를 위해\n동의가 필요합니다.", Toast.LENGTH_LONG).show(); // 권한 요청 토스트 메시지 표시
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    110); // 권한 요청
                        }
                    }
                }
            }
        });

        mWebView.loadUrl("https://example.com");

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                // 다운로드 매니저 가져오기
                DownloadManager dm = (DownloadManager) ctxt.getSystemService(Context.DOWNLOAD_SERVICE);

                // 설치 화면 인텐트
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor c = dm.query(query);
                if (c.moveToFirst()) {
                    int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(statusIndex)) {
                        int uriIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        if (uriIndex != -1) {
                            String uriString = c.getString(uriIndex);
                            // 파일 객체 생성
                            File file = new File(Uri.parse(uriString).getPath());
                            // FileProvider를 사용하여 content:// URI 생성
                            Uri fileUri = FileProvider.getUriForFile(ctxt, ctxt.getApplicationContext().getPackageName() + ".provider", file);

                            // 설치 인텐트 시작
                            Intent install = new Intent(Intent.ACTION_VIEW);
                            install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            // content:// URI를 사용하여 인텐트 설정
                            install.setDataAndType(fileUri, "application/vnd.android.package-archive");
                            // URI에 대한 임시 액세스 권한 부여
                            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            // Android 8.0 (API 레벨 26) 이상에서는 앱이 APK 설치를 시작하기 전에 사용자에게 알려야 합니다.
                            if (Build.VERSION.SDK_INT >= 26) {
                                if (!ctxt.getPackageManager().canRequestPackageInstalls()) {
                                    // 알 수 없는 앱 설치를 허용하도록 사용자에게 요청합니다.
                                    Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctxt.getPackageName()));
                                    ctxt.startActivity(settingsIntent);
                                } else {
                                    ctxt.startActivity(install);
                                }
                            } else {
                                ctxt.startActivity(install);
                            }
                        } else {
                            // DownloadManager.COLUMN_LOCAL_URI 열이 존재하지 않는 경우를 처리합니다.
                            // Toast 메시지를 사용하여 사용자에게 알립니다.
                            Toast.makeText(ctxt, "Download failed or is not complete yet.", Toast.LENGTH_LONG).show();
                        }

                    }
                }
            }
        };

        // BroadcastReceiver를 등록합니다.
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
