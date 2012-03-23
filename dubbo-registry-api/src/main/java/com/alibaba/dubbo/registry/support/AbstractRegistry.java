/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

/**
 * 嵌入式注册中心实现，不开端口，只是map进行存储查询.不需要显示声明
 * 
 * @author chao.liuc
 * @author william.liangf
 */
public abstract class AbstractRegistry implements Registry {

    // 日志输出
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private URL registryUrl;

    private final Set<String> registered = new ConcurrentHashSet<String>();

    private final ConcurrentMap<String, Set<NotifyListener>> subscribed = new ConcurrentHashMap<String, Set<NotifyListener>>();

    public AbstractRegistry(URL url) {
        setUrl(url);
    }
    
    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public Set<String> getRegistered() {
        return registered;
    }

    public Map<String, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }

    public URL getUrl() {
        return registryUrl;
    }

    public List<URL> lookup(URL url) {
        List<URL> urls= new ArrayList<URL>();
        for (String r: getRegistered()) {
            URL u = URL.valueOf(r);
            if (UrlUtils.isMatch(url, u)) {
                urls.add(u);
            }
        }
        return urls;
    }

    public void register(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Register: " + url);
        }
        registered.add(url.toFullString());
    }
    
    public void unregister(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Unregister: " + url);
        }
        registered.remove(url.toFullString());
    }
    
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Subscribe: " + url);
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        String key = url.toFullString();
        Set<NotifyListener> listeners = subscribed.get(key);
        if (listeners == null) {
            subscribed.putIfAbsent(key, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(key);
        }
        listeners.add(listener);
    }
    
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Unsubscribe: " + url);
        }
        String key = url.toFullString();
        Set<NotifyListener> listeners = subscribed.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void recover() throws Exception {
        // register
        Set<String> recoverRegistered = new HashSet<String>(getRegistered());
        if (! recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register services " + recoverRegistered);
            }
            for (String url : recoverRegistered) {
                register(URL.valueOf(url), null);
            }
        }
        // subscribe
        Map<String, Set<NotifyListener>> recoverSubscribed = new HashMap<String, Set<NotifyListener>>(getSubscribed());
        if (recoverSubscribed.size() > 0) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe services " + recoverSubscribed);
            }
            for (Map.Entry<String, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                String url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(URL.valueOf(url), listener);
                }
            }
        }
    }
    
    public void destroy() {
        if (logger.isInfoEnabled()){
            logger.info("Destroy registry: " + getUrl());
        }
        for (String url : new HashSet<String>(registered)) {
            try {
                unregister(URL.valueOf(url), null);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }
    
    public String toString() {
        return getUrl().toString();
    }

}