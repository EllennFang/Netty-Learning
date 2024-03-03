package com.fang.nio;

import com.fang.test.TestByteBufferExam;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.fang.util.ByteBufferUtil.debugAll;

@Slf4j
public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        // 创建 boss 的 Selector，管理多个 Channel
        Selector boss = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open(); // 创建服务 channel
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(8080));
        // 建立 Selector 和 Channel 的关系（注册）
        // SelectionKey 就是将来事件发生后，通过它可以知道事件和哪个channel的事件
        SelectionKey ssckey = ssc.register(boss, 0, null);
        // ssckey 只关注 accept 事件
        ssckey.interestOps(SelectionKey.OP_ACCEPT);

        // 1、创建固定数量的 worker 并初始化
        Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }
        AtomicInteger index = new AtomicInteger();
        while (true) {
            boss.select();
            Iterator<SelectionKey> iter = boss.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    log.debug("connected...{}", sc.getLocalAddress());
                    // 2、关联到 worker 的 selector 中
                    log.debug("before register...{}", sc.getLocalAddress());
                    // round robin 轮询
                    workers[index.getAndIncrement() % workers.length].register(sc); // boss　调用初始化　selector，启动 worker-i
                    log.debug("after register...{}", sc.getLocalAddress());
                }
            }
        }
    }

    static class Worker implements Runnable{
        private Thread thread;
        private Selector selector;
        private String name;
        private volatile boolean start = false; // 表示还未初始化
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>(); // 用队列来进行线程之间的对象传递
        public Worker(String name) {
            this.name = name;
        }

        // 初始化线程和 selector，并且添加 {sc 关联（注册） selector}事件
        public void register(SocketChannel sc) throws IOException {
            // 如果当前 Worker 还未初始化
            if (!start) {
                thread = new Thread(this, name);
                selector = Selector.open();
                thread.start();
                start = true;
            }
            queue.add(() -> {
                try {
                    sc.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(16));
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            });
            selector.wakeup(); // 唤醒 select() 方法
        }

        @Override
        public void run() {
            while (true) {
                try {
                    selector.select(); // 阻塞，需要 wakeup() 方法唤醒
                    // 看是否有新的 sc 需要注册
                    Runnable task = queue.poll();
                    if (task != null) {
                        task.run(); // 执行了 sc.register(selector, SelectionKey.OP_READ, null);
                    }

                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    if (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        /*if (key.isReadable()) {
                            ByteBuffer buffer = (ByteBuffer) key.attachment();
                            SocketChannel sc = (SocketChannel) key.channel();
                            log.debug("read...{}", sc.getRemoteAddress());
                            sc.read(buffer);
                            buffer.flip();
                            debugAll(buffer);
                        }*/
                        try {
                            SocketChannel sc = (SocketChannel) key.channel(); // 获取触发事件的 channel
                            // 获取该 selectionKey 的附件
                            ByteBuffer buffer = (ByteBuffer) key.attachment();
                            log.debug("read...{}", sc.getRemoteAddress());
                            int read = sc.read(buffer); // 如果客户端正常断开，read 方法的返回值为 -1
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
