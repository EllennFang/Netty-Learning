package com.fang.server.handler;

import com.fang.message.GroupCreateRequestMessage;
import com.fang.message.GroupCreateResponseMessage;
import com.fang.server.session.Group;
import com.fang.server.session.GroupSession;
import com.fang.server.session.GroupSessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.Set;

@ChannelHandler.Sharable
public class GroupCreateRequestMessageHandler extends SimpleChannelInboundHandler<GroupCreateRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupCreateRequestMessage msg) throws Exception {
        String groupName = msg.getGroupName();
        Set<String> members = msg.getMembers();
        // 群管理器
        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        Group group = groupSession.createGroup(groupName, members);
        // groupSession.createGroup() 底层用了hashmap.putIfAbsent(k, v)
        // 如果所指定的 key 已经在 HashMap 中存在，返回和这个 key 值对应的 value, 如果所指定的 key 不在 HashMap 中存在，则返回 null。
        if (group == null) {
            // 发送成功消息
            ctx.writeAndFlush(new GroupCreateResponseMessage(true, "[" + groupName + "] 创建成功"));
            // 向成员发送拉群消息
            List<Channel> channels = groupSession.getMembersChannel(groupName);
            for (Channel channel : channels) {
                channel.writeAndFlush(new GroupCreateResponseMessage(true, "你已被拉入" + "[" + groupName + "] 群聊"));
            }
        } else {
            ctx.writeAndFlush(new GroupCreateResponseMessage(false, "[" + groupName + "] 已经存在"));
        }
    }
}
