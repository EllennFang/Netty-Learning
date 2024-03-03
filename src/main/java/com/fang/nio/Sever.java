package com.fang.nio;

import com.fang.test.TestByteBufferExam;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

@Slf4j
public class Sever {

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

                        // 将客户端的 channel 和 selector 建立关系（注册）, 并且将一个 buffer 作为附件关联到该 SelectionKey 上
                        ByteBuffer buffer = ByteBuffer.allocate(16);
                        SelectionKey scKey = sc.register(selector, 0, buffer);
                        scKey.interestOps(SelectionKey.OP_READ);
                        log.debug("{}", sc);
                        log.debug("scKey：{}", scKey);
                    } else if (key.isReadable()) { // read 类型事件
                        try {
                            SocketChannel channel = (SocketChannel) key.channel(); // 获取触发事件的 channel
                            // 获取该 selectionKey 的附件
                            ByteBuffer buffer = (ByteBuffer) key.attachment();
                            int read = channel.read(buffer); // 如果客户端正常断开，read 方法的返回值为 -1
                            if (read == -1) {
                                key.cancel();
                            } else {
                                // 读取内容
                                TestByteBufferExam.split(buffer);
                                if (buffer.position() == buffer.limit()) {
                                    // buffer 无法存储一条完整的消息
                                    ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                                    // 切换读模式
                                    buffer.flip();
                                    newBuffer.put(buffer);
                                    // 将新的 buffer 替换原来的附件
                                    key.attach(newBuffer);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            key.cancel(); // 因为客户端断开了,因此需要将 key 取消（从 selector 的 keys 集合中真正删除 key）
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
