package com.fang.server.handler;

import com.fang.message.GroupChatRequestMessage;
import com.fang.message.GroupChatResponseMessage;
import com.fang.server.session.GroupSession;
import com.fang.server.session.GroupSessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;

@ChannelHandler.Sharable
public class GroupChatRequestMessageHandler extends SimpleChannelInboundHandler<GroupChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupChatRequestMessage msg) throws Exception {
        String groupName = msg.getGroupName();
        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        // 如果群聊不存在
        if (!groupSession.groupExist(groupName)) {
            ctx.writeAndFlush(new GroupChatResponseMessage(false, "发送失败"));
        } else {
            // 群聊存在，发送消息
            List<Channel> channels = groupSession.getMembersChannel(groupName);
            for (Channel channel : channels) {
                channel.writeAndFlush(new GroupChatResponseMessage(msg.getFrom(), msg.getContent()));
            }
        }
    }
}
