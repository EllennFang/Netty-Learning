package com.fang.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

@Slf4j
public class WriteServer {

    public static void main(String[] args) {
        try {
            // 1、创建 Selector，管理多个 Channel
            Selector selector = Selector.open();
            ServerSocketChannel ssc = ServerSocketChannel.open(); // 创建服务 channel
            ssc.configureBlocking(false);
            ssc.bind(new InetSocketAddress(8080));
            // 2、建立 Selector 和 Channel 的关系（注册）
            // SelectionKey 就是将来事件发生后，通过它可以知道事件和哪个channel的事件
            SelectionKey ssckey = ssc.register(selector, 0, null);
            // ssckey 只关注 accept 事件
            ssckey.interestOps(SelectionKey.OP_ACCEPT);
            log.debug("sscKey：{}", ssckey);

            while (true) {
                // 3、select 方法。没有事件发生时线程阻塞，有事件发生时恢复运行
                // select 在事件未处理时，它不会阻塞，事件发生后要么处理要么取消，不能置之不理
                selector.select();
                // 4、处理事件，selectionKeys 集合内部包含了所有发生的事件
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); // 获取迭代器

                // 遍历所有事件逐一处理
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    // 处理 key 时，要从 selectionKeys 集合中删除，否则下次处理就会有问题（因为它不会自己删除）
                    iter.remove();
                    log.debug("key：{}", key);
                    // 5、区分事件类型
                    if (key.isAcceptable()) { // accept 类型事件
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel(); // 获取触发事件的 channel
                        SocketChannel sc = channel.accept();// 建立链接
                        sc.configureBlocking(false);

                        // 将客户端的 channel 和 selector 建立关系（注册）
                        SelectionKey scKey = sc.register(selector, 0, null);
                        scKey.interestOps(SelectionKey.OP_READ);
                        // 向客户端发送消息
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 30000000; i++) {
                            sb.append("a");
                        }
                        ByteBuffer buffer = Charset.defaultCharset().encode(sb.toString());
                        int write = sc.write(buffer);
                        // write 表示实际写了多少字节
                        System.out.println("实际写入字节：" + write);
                        // 如果还有剩余未写字节，才需要关注写事件
                        if (buffer.hasRemaining()) {
                            // 在原有关注事件的基础上，多关注写事件
                            scKey.interestOps(scKey.interestOps() + SelectionKey.OP_WRITE);
                            // 把 buffer 作为scKey附件
                            scKey.attach(buffer);
                        }
                        log.debug("{}", sc);
                        log.debug("scKey：{}", scKey);
                    } else if (key.isWritable()) {
                        // 获取触发事件的 channel
                        SocketChannel sc = (SocketChannel) key.channel();
                        // 获取 key 的附件
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        int write = sc.write(buffer);
                        System.out.println("实际写入字节：" + write);
                        if (!buffer.hasRemaining()) {
                            // 写完了
                            key.interestOps(key.interestOps() - SelectionKey.OP_WRITE);
                            key.attach(null);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
