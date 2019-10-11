/*
 * Copyright (c) 2019 NetFoundry, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.netfoundry.ziti.sample.http;

import io.netfoundry.ziti.Ziti;
import io.netfoundry.ziti.net.internal.ZitiSSLSocket;

import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class HttpSample {

    public static void main(String[] args) {

        try {
            Ziti.init(args[0], "".toCharArray(), true);

            URL url = new URL(args[1]);

            runRawSSL(url);

            runHttp(url);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void runRawSSL(URL url) throws Exception {
        String host = url.getHost();
        int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();

        Socket transport = new Socket(host, port);

        SSLSocket ssl = new ZitiSSLSocket(transport, host, port);
        String req = String.format("GET %s HTTP/1.1\nAccept: */*\nAccept-Encoding: gzip, deflate\nConnection: close\nHost: %s\nUser-Agent: HTTPie/1.0.2\n\n",
                url.getPath(), host);

        ssl.getOutputStream().write(req.getBytes());
        ssl.getOutputStream().flush();

        int rc = 0;
        byte[] resp = new byte[1024];
        ByteArrayOutputStream r = new ByteArrayOutputStream();
        do {
            rc = ssl.getInputStream().read(resp, 0, resp.length);
            if (rc >= 0)
                r.write(resp, 0, rc);
        } while (rc >= 0);

        try {
            System.out.println(new String(r.toByteArray()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    static void runHttp(URL url) {

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "text/plain");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("User-Agent", "curl");
            conn.setDoInput(true);
            int rc = conn.getResponseCode();
            byte[] buf = new byte[1024];
            ByteArrayOutputStream resp = new ByteArrayOutputStream();

            if (rc != 200) {
                int len = conn.getErrorStream().read(buf);
                System.err.println(String.format("%d %s\n%s", rc, conn.getResponseMessage(), new String(buf, 0, len)));
            } else {
                do {
                    int len = conn.getInputStream().read(buf);
                    if (len < 0) break;
                    resp.write(buf, 0, len);
                } while (true);

                System.out.println(new String(resp.toByteArray()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("\n=============== Done! ==================\n");
    }
}
