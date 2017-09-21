package com.zjlp.face.monitor.ws;

/**
 * @author zhangbo 2017年09月13日
 * @reviewer
 */

import com.zjlp.face.monitor.entity.dto.ShellChannelContainer;
import com.zjlp.face.monitor.listener.GetHttpSessionConfigurator;
import com.zjlp.face.monitor.service.LinuxStateForShellService;
import com.zjlp.face.monitor.utils.ThreadUtils;
import com.zjlp.face.sysmanager.model.ServerData;
import com.zjlp.face.sysmanager.model.ServiceData;
import com.zjlp.face.sysmanager.service.ServerService;
import com.zjlp.face.sysmanager.service.ServiceService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.SpringConfigurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@Component
@ServerEndpoint(value = "/log/{id}", configurator = GetHttpSessionConfigurator.class)
public class LogWebSocketHandle {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogWebSocketHandle.class);
    private InputStream inputStream;

    @Autowired
    private LinuxStateForShellService linuxStateForShellService;


    @Autowired
    private ServerService serverService;

    @Autowired
    private ServiceService serviceService;

    /**
     * 新的WebSocket请求开启
     */
    @OnOpen
    public void onOpen(final Session session, EndpointConfig config,@PathParam("id")String id) {
        try {
            String serverId = null;
            String serviceId = null;
            if(StringUtils.isNotBlank(id)){
                serverId = id.split("_")[1];
                serviceId = id.split("_")[0];
            }
            ServerData serverData = serverService.getServerInfoById(Integer.valueOf(serverId));
            ServiceData serviceData = serviceService.getServiceInfoById(Integer.valueOf(serviceId));
            //HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            String sessionid = session.getId();
            final InputStream inputStream = linuxStateForShellService.runDistanceShell(serviceData.getServiceName(), serverData.getOsUsername(), serverData.getOsPassword(), serverData.getServerAddress(), Integer.valueOf(serverData.getSshdPort()), sessionid,serverData.getPath());
            this.inputStream = inputStream;
            // 一定要启动新的线程，防止InputStream阻塞处理WebSocket的线程
            ThreadUtils.buriedDataPool.execute(new Runnable() {
                @Override
                public void run() {
                    sendUserLocal(inputStream, session);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendUserLocal(InputStream inputStream, Session session) {
        try {
            // 判断是否为终端信息。如果是终端信息则查询数据库获取detail
            String line;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
                while ((line = reader.readLine()) != null) {
                    // 将实时日志通过WebSocket发送给客户端，给每一行添加一个HTML换行
                    session.getBasicRemote().sendText(line + "<br>");
                }
            } catch (IOException e) {
                LOGGER.warn("客户端被强制关闭");
            }

        } catch (Exception e) {
            LOGGER.warn("客户端被强制关闭");
        }
    }

    /**
     * WebSocket请求关闭
     */
    @OnClose
    public void onClose(Session session) {
        try {
            if (this.inputStream != null) {
                this.inputStream.close();
            }
            if (StringUtils.isNotBlank(session.getId()))
                ShellChannelContainer.closeAndRemoveChannel(session.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @OnError
    public void onError(Throwable thr) {
        LOGGER.warn("客户端被强制关闭");
    }
}
