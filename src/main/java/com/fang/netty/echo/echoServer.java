package com.fang.netty.echo;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

@Slf4j
public class echoServer {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                // log.debug(buf.toString(Charset.defaultCharset()));
                                System.out.println(buf.toString(Charset.defaultCharset()));

                                // 建议使用 ctx.alloc() 来创建 ByteBuf
                                ByteBuf responseBuf = ctx.alloc().buffer();
                                responseBuf.writeBytes(buf);
                                ctx.writeAndFlush(responseBuf);
                                // 思考：需要释放 buf 吗？
                                // 思考：需要释放 responsebuf 吗？
                            }
                        });
                    }
                }).bind(8081);
    }
}
