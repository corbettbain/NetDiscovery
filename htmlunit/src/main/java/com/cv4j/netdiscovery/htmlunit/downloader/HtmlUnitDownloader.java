package com.cv4j.netdiscovery.htmlunit.downloader;

import com.cv4j.netdiscovery.core.config.Constant;
import com.cv4j.netdiscovery.core.domain.Request;
import com.cv4j.netdiscovery.core.domain.Response;
import com.cv4j.netdiscovery.core.downloader.Downloader;
import com.cv4j.proxy.domain.Proxy;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.safframework.tony.common.utils.Preconditions;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.functions.Function;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by tony on 2018/6/3.
 */
public class HtmlUnitDownloader implements Downloader {

    private WebClient webClient;

    public HtmlUnitDownloader() {

        this.webClient = new WebClient(BrowserVersion.CHROME);
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        this.webClient.getOptions().setThrowExceptionOnScriptError(false);
        this.webClient.getOptions().setRedirectEnabled(false);
        this.webClient.getOptions().setCssEnabled(false);
        this.webClient.setJavaScriptTimeout(1000);
        this.webClient.getOptions().setJavaScriptEnabled(true);
        this.webClient.setAjaxController(new NicelyResynchronizingAjaxController());//设置支持AJAX
    }

    @Override
    public Maybe<Response> download(Request request) {

        URL url = null;
        try {
            url = new URL(request.getUrl());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        WebRequest webRequest = new WebRequest(url);
        webRequest.setHttpMethod(HttpMethod.GET);

        if (request.getHttpMethod() == io.vertx.core.http.HttpMethod.POST) {

            webRequest.setHttpMethod(HttpMethod.POST);

            if (Preconditions.isNotBlank(request.getHttpRequestBody())) {

                String body = new String(request.getHttpRequestBody().getBody());
                webRequest.setRequestBody(body);

                // 针对post请求，需要对header添加一些信息
                if (Preconditions.isNotBlank(request.getHttpRequestBody().getContentType())) {

                    webRequest.setAdditionalHeader(Constant.CONTENT_TYPE,request.getHttpRequestBody().getContentType());
                }
            }
        }

        //header
        webRequest.setAdditionalHeaders(request.getHeader());

        //proxy
        Proxy proxy = request.getProxy();
        if(proxy != null) {
            webRequest.setProxyHost(proxy.getIp());
            webRequest.setProxyPort(proxy.getPort());
        }

        return Maybe.create(new MaybeOnSubscribe<WebResponse>() {
            @Override
            public void subscribe(MaybeEmitter emitter) throws Exception {
                HtmlPage page = webClient.getPage(webRequest);

                emitter.onSuccess(page.getWebResponse());
            }
        }).map(new Function<WebResponse, Response>() {

            @Override
            public Response apply(WebResponse webResponse) throws Exception {

                Response response = new Response();
                response.setContent(webResponse.getContentAsString().getBytes());
                response.setStatusCode(webResponse.getStatusCode());
                response.setContentType(webResponse.getContentType());

                return response;
            }
        });
    }

    @Override
    public void close() throws IOException {
        webClient.close();
    }
}
