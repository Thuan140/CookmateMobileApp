package com.example.cookmate;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Minimal Volley multipart/form-data request.
 * (Adapted for typical usage: getParams() -> form fields, getByteData() -> files)
 */
public abstract class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final String boundary = "apiclient-" + System.currentTimeMillis();
    private static final String LINE_FEED = "\r\n";

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
    }

    // Inheritors should provide byte parts with name -> DataPart
    protected abstract Map<String, DataPart> getByteData();

    // Default uses Request#getParams for textual fields
    @Override
    public byte[] getBody() throws AuthFailureError {
        Map<String, String> params = getParams();
        Map<String, DataPart> data = getByteData();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            if (params != null && params.size() > 0) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("--").append(boundary).append(LINE_FEED);
                    sb.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"").append(LINE_FEED).append(LINE_FEED);
                    sb.append(entry.getValue()).append(LINE_FEED);
                    baos.write(sb.toString().getBytes("UTF-8"));
                }
            }

            if (data != null && data.size() > 0) {
                for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                    DataPart dp = entry.getValue();
                    StringBuilder sb = new StringBuilder();
                    sb.append("--").append(boundary).append(LINE_FEED);
                    sb.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"; filename=\"")
                            .append(dp.getFileName()).append("\"").append(LINE_FEED);
                    sb.append("Content-Type: ").append(dp.getType()).append(LINE_FEED).append(LINE_FEED);
                    baos.write(sb.toString().getBytes("UTF-8"));
                    baos.write(dp.getContent());
                    baos.write(LINE_FEED.getBytes("UTF-8"));
                }
            }

            String end = "--" + boundary + "--" + LINE_FEED;
            baos.write(end.getBytes("UTF-8"));
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(com.android.volley.VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    public static class DataPart {
        private final String fileName;
        private final byte[] content;
        private final String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }

        public String getFileName() { return fileName; }
        public byte[] getContent() { return content; }
        public String getType() { return type; }
    }
}
