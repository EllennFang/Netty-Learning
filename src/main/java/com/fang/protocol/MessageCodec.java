package com.fang.protocol;

import com.fang.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * 自定义 Netty 协议，编解码器
 * 必须和 LengthFieldBasedFrameDecoder 一起使用，确保接到的 ByteBuf 消息是完整的
 *
 */
@Slf4j
public class MessageCodec extends ByteToMessageCodec<Message> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message msg, ByteBuf out) throws Exception {
        // 1. 4 字节的魔数
        out.writeBytes(new byte[]{1, 2, 3, 4});
        // 2. 1 字节的版本号
        out.writeByte(1);
        // 3. 1 字节的序列化方式(算法)（如 jdk 0，json 1）
        out.writeByte(0);
        // 4. 1 自己二的指令类型
        out.writeByte(msg.getMessageType());
        // 5. 4 字节的请求序列号
        out.writeInt(msg.getSequenceId());
        // 无意义，为了对齐填充（2^n固定字节长度）
        out.writeByte(0xff);
        // 6. 获取内容（消息）的字节数组（序列化消息体）
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        byte[] bytes = bos.toByteArray();
        // 7. 4 字节的长度（正文长度）
        out.writeInt(bytes.length);
        // 8. 写入内容
        out.writeBytes(bytes);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        int magicNum = in.readInt(); // 魔数
        byte version = in.readByte(); // 版本号
        byte serializerType = in.readByte(); // 序列化方式（算法）
        byte messageType = in.readByte(); // 指令类型
        int sequenceId = in.readInt(); // 请求序列号
        in.readByte(); // 填充字节
        int length = in.readInt(); // 消息长度
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length); // 获取消息字节数组
        // 反序列化字节数组
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Message message = (Message) ois.readObject();
        log.debug("{}, {}, {}, {}, {}, {}", magicNum, version, serializerType, messageType, sequenceId, length);
        log.debug("{}", message);
        out.add(message); // 传给下一个 handler
    }
}
