# nginx部署配置

> 目前阿里云有三个集群和数个common-proxy，而平台对应的现网common-proxy只能配置一个，每次上线游戏都需要重新修改，所以现在需要在所有现网集群的上层搭建一个nginx反向代理平台的请求

### 规则

##### 目前common-proxy支持两种方式转发集群内的http请求

+ 根据regionId转发

  /**httpService**/{msgId.servername:port}/{appId}/{regionId}/{channelId}/{proxyIndex}

+ 根据regionIndex转发

  /**httpServiceForRegionIndex**/{msgId.servername:port}/{appId}/{regionIndex}/{channelId}/{proxyIndex}

{msgId.servername:port}：游戏业务的消息名.服务名:端口号

{appId}：游戏id

{regionId}：区服id

{regionIndex}：区服索引

{channelId}：渠道id，一般没有要求填  **" .* "**，意思任何字符任意数量

##### common-proxy转发GRPC请求

/jsonMsgCheckSign/{appId}/{regionId}/{channelId}/{proxyIndex}

{appId}：游戏id

{regionId}：区服id

{channelId}：渠道id，一般没有要求填  **" .* "**，意思任何字符任意数量



{proxyIndex}：common-proxy的唯一ID，可以不填

### nginx.config配置

```
        server {
            listen       80;
            server_name  47.96.184.248;

            location ~ /httpService/delivery.pay-service:31509/1000004/wuhui-test/.* {
                proxy_pass  http://47.98.144.46:33306;
            }
            location ~ /httpServiceForRegionIndex/notify.activity-notify:31006/1000004/3/.* {
                proxy_pass  http://47.98.144.46:33306;
            }
            location ~ /jsonMsgCheckSign/1000004/wuhui-test/.* {
                proxy_pass  http://47.98.144.46:33306;
            }
        }
```

**listen**：nginx监听端口

**server_name**：nginx的ip或者域名

**location ~ /httpService/delivery.pay-service:31509/1000004/wuhui-test/.***

+ location ：需要转发的路径
+ ~ ：正则匹配

**proxy_pass** ：需要代理的common-proxy地址

### nginx部署

#### configMap.yaml

```
apiVersion: v1
kind: ConfigMap
metadata:
    name: web-nginx-config
data:
  nginx.conf: |
    user  nginx;
    worker_processes  1;

    error_log  /var/log/nginx/error.log warn;
    pid        /var/run/nginx.pid;


    events {
        worker_connections  1024;
    }


    http {
        include       /etc/nginx/mime.types;
        default_type  application/octet-stream;

        log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                          '$status $body_bytes_sent "$http_referer" '
                          '"$http_user_agent" "$http_x_forwarded_for"';

        access_log  /var/log/nginx/access.log  main;

        sendfile        on;
        #tcp_nopush     on;

        keepalive_timeout  65;

        #gzip  on;

        include /etc/nginx/conf.d/*.conf;
    }

```

#### deployment.yaml

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-nginx
spec:
  replicas: 1
  selector:
    matchLabels:
      app: web-nginx
  template:
    metadata:
      labels:
        app: web-nginx
    spec:
      containers:
        - name: web-nginx
          image: nginx:1.7.9
          imagePullPolicy: IfNotPresent
          ports:
          - containerPort: 80
          - containerPort: 443
          volumeMounts:
          - name: web-nginx-config
            mountPath: /etc/nginx/nginx.conf
            subPath: nginx.conf
      volumes:
        - name: web-nginx-config
          configMap:
            name: web-nginx-config
            items:
            - key: nginx.conf
              path: nginx.conf
```

