package com.zjlp.face.monitor.listener;

import com.zjlp.face.monitor.entity.dto.ShellChannelContainer;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * @author zhangbo 2017年09月13日
 * @reviewer
 */
public class ChannelListener implements HttpSessionListener {
    @Override
    public void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String id = se.getSession().getId();
        if (StringUtils.isNotBlank(id))
            ShellChannelContainer.closeAndRemoveChannel(id);
    }
}
