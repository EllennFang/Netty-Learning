package com.fang.server.handler;

import com.fang.server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class QuitHandler extends ChannelInboundHandlerAdapter {

    /**
     * 当连接断开时触发 inactive 事件
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 会话管理器消除断开连接的channel绑定
        SessionFactory.getSession().unbind(ctx.channel());
        log.debug("{} 断开连接", ctx.channel());
    }

    /**
     * 当出现异常时触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SessionFactory.getSession().unbind(ctx.channel());
        log.debug("{} 异常断开 异常：{}", ctx.channel(), cause.getMessage());
    }
}
